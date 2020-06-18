package controllers

import java.io.File
import java.nio.file.Files

import dao.dutyDao
import javax.inject.Inject
import org.apache.commons.io.FileUtils
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents, Headers}
import utils.{CompressUtil, ExecCommand, MyStringTool, Utils}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.collection.JavaConverters._

import utils.Implicits._

class RService @Inject()(cc: ControllerComponents,dutydao:dutyDao,dutyController: DutyController)(implicit exec: ExecutionContext) extends AbstractController(cc) with MyStringTool{


  //PCA
  case class PCA1Data(taskname:String,txdata1:String,scale:String,showname:String,drawcircle:String)

  val PCA1Form=Form(
    mapping (
      "taskname"->text,
      "txdata1"->text,
      "scale"->text,
      "showname"->text,
      "drawcircle"->text
    )(PCA1Data.apply)(PCA1Data.unapply)
  )

  def doPCA(isgroup:Boolean)=Action(parse.multipartFormData){implicit request=>
    val data=PCA1Form.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val file1=request.body.file("table1").get
    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")
    val input=
    if(isgroup&&(!request.body.file("table2").isEmpty)) file1.filename+"/"+ request.body.file("table2").get.filename
    else file1.filename
    val param=
      if(isgroup) "归一化："+ data.scale + "/是否显示样本名：" + data.showname + "/是否绘制分组圈：" +data.drawcircle
      else "归一化："+data.scale

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"PCA","主成分分析（PCA）",input,param)
    //矩阵文件读取写入任务文件下table.txt
    file1.ref.moveTo(tableFile)

    Future{
      val command1 = "Rscript "+Utils.path+"R/pca/pca_data.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out" + " -sca " + data.scale
      val execCommand1 = new ExecCommand
      //exec需要指定结果输出路径的时候，不指定默认本地任务路径
      execCommand1.exect(command1,dutyDir+"/temp")
      //是否有group文件
      val group=
        if(isgroup){
          if(!request.body.file("table2").isEmpty){
            val file = request.body.file("table2").get
            val groupdatas = FileUtils.readFileToString(file.ref.file)
            FileUtils.writeStringToFile(groupFile, "#SampleID\tGroup\n"+groupdatas)
            //        request.body.file("table2").get.ref.moveTo(groupFile)
          }else{
            FileUtils.writeStringToFile(groupFile, "#SampleID\tGroup\n"+data.txdata1)
          }
          " -g "+groupFile.getAbsolutePath
        }else ""

      val name=if(data.showname.equals("TRUE")){
        val f=FileUtils.readLines(groupFile).asScala
        val n=f.map{_.split('\t').last}.distinct.drop(1).mkString(",")
        " -b " + n
      }else ""

      val command2 = "Rscript "+Utils.path+"R/pca/pca_plot.R -i "+ dutyDir+"/out/pca.x.xls" +
        " -si " + dutyDir+"/out/pca.sdev.xls" + group + " -o " +dutyDir+"/out" + name + " -c " + data.drawcircle
      val execCommand2 = new ExecCommand
      execCommand2.exect(command2,dutyDir+"/temp")

      if (execCommand1.isSuccess && execCommand2.isSuccess) {
        val finish=dutyController.updateFini(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),group+name+" -c "+data.drawcircle)
        creatZip(dutyDir)
        Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/temp/pca.png")
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand1.getErrStr+"\n\n"+execCommand2.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readPCAData(taskname:String)=Action{implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val head=FileUtils.readFileToString(new File(path+"/table.txt")).trim.split("\n")
    val gnum=head(0).trim.split("\t").drop(1).length
    //获取分组
    val group=
      if(new File(path+"/group.txt").exists()) {
        val f=FileUtils.readLines(new File(path+"/group.txt")).asScala
        val g=f.map{_.split('\t').last}.distinct.drop(1).mkString(" , ")
        //检查group的数量与矩阵head是否一样，小于则+nogroup，相等则不变
        if(f.map{_.split('\t').head}.drop(1).length<gnum) g+" , nogroup"
        else g
      }else "无分组"
    //获取x,y轴数据
    val data=FileUtils.readLines(new File(path+"/out/pca.x.xls"))
    val col=data.get(0).split("\"").filter(_.trim!="").map(_.trim)
    //获取图片
    val pics=getReDrawPics(path)
    val downpics=path+"/out/pca.pdf"
    Ok(Json.obj("group"->group,"cols"->col,"pics"->pics,"downpics"->downpics))
  }

  case class RePCAData(xdata:String,ydata:String,width:String,
                       length:String,color:String,resolution:String,xts:String,yts:String,
                       xls:String,yls:String,lts:String)

  val RePCAForm=Form(
    mapping (
      "xdata"->text,
      "ydata"->text,
      "width"->text,
      "length"->text,
      "color"->text,
      "resolution"->text,
      "xts"->text,
      "yts"->text,
      "xls"->text,
      "yls"->text,
      "lts"->text,
    )(RePCAData.apply)(RePCAData.unapply)
  )

  def redrawPCA(taskname:String)=Action(parse.multipartFormData) { implicit request =>
    val data=RePCAForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname
    val groupFile=new File(dutyDir,"group.txt")

    //读取log.txt 包括-g -b -c
    val log=FileUtils.readFileToString(new File(dutyDir+"/log.txt"))
    val color=
      if(data.color!="") {
        if(!groupFile.exists()) " -oc "+data.color.split(",").mkString(":")
        else " -cs "+data.color.split(",").mkString(":")
      }else ""

    val command = "Rscript "+Utils.path+"R/pca/pca_plot.R -i "+ dutyDir+"/out/pca.x.xls" +
      " -si " + dutyDir+"/out/pca.sdev.xls" + " -o " +dutyDir+"/out" + " -pxy "+ data.xdata+":"+ data.ydata +log +
    " -is "+ data.width + ":" + data.length + color + " -dpi " + data.resolution + " -xts " + "sans:plain:"+data.xts +
    " -yts " + "sans:plain:"+data.yts + " -xls " + "sans:plain:"+data.xls + " -yls " + "sans:plain:"+data.yls +
      " -lts " + "sans:plain:"+data.lts

    val execCommand = new ExecCommand
    execCommand.exect(command,dutyDir+"/temp")

    if (execCommand.isSuccess) {
      creatZip(dutyDir) //替换压缩文件包
      Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/temp/pca.png") //替换图片
      val pics=dutyDir+"/temp/pca.png"
      Ok(Json.obj("valid"->"true","pics"->pics))
    } else {
      Ok(Json.obj("valid"->"false"))
    }

  }





  //Boxplot
  case class BoxplotData(taskname:String,spot:String,ymin:String,ymax:String)

  val BoxplotForm = Form(
    mapping (
      "taskname"->text,
      "spot"->text,
      "ymin"->text,
      "ymax"->text
    )(BoxplotData.apply)(BoxplotData.unapply)
  )

  def doBoxplot=Action(parse.multipartFormData){ implicit request=>
    val data=BoxplotForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val file1=request.body.file("table1").get
    val tableFile=new File(dutyDir,"table.txt")
    val input= file1.filename
    val param= "是否绘制盒子内部点："+data.spot+"/Y轴范围："+data.ymin+"-"+data.ymax
    val ylim=
      if(data.ymin!=""&&data.ymax!="") data.ymin+":"+data.ymax
      else ""

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"Boxplot","Boxplot 盒型图",input,param)
    //矩阵文件读取写入任务文件下table.txt
    file1.ref.moveTo(tableFile)

    Future{
      val command = "Rscript "+Utils.path+"R/box/boxplot.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out" + " -sp " + data.spot + ylim + " -ls 12:10"
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (execCommand.isSuccess) {
        val finish=dutyController.updateFini(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt")," -sp "+data.spot)
        creatZip(dutyDir)
        Utils.pdf2Png(dutyDir+"/out/box.pdf",dutyDir+"/temp/box.png")
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readBoxData(taskname:String)=Action{implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val head=FileUtils.readFileToString(new File(path+"/table.txt")).trim.split("\n")
    val group=head(0).trim.split("\t").mkString(" , ")

    //获取图片
    val pics=getReDrawPics(path)
    val downpics=path+"/out/box.pdf"
    Ok(Json.obj("group"->group,"pics"->pics,"downpics"->downpics))
  }

  case class ReBoxData(spot:String,ymin:String,ymax:String,color:String,width:String,length:String,
                       dpi:String,boxwidth:String,alp:String,add:String,xts:String,yts:String,lts:String)

  val ReBoxForm=Form(
    mapping (
      "spot"->text,
      "ymin"->text,
      "ymax"->text,
      "color"->text,
      "width"->text,
      "length"->text,
      "dpi"->text,
      "boxwidth"->text,
      "alp"->text,
      "add"->text,
      "xts"->text,
      "yts"->text,
      "lts"->text
    )(ReBoxData.apply)(ReBoxData.unapply)
  )

  def redrawBox(taskname:String)=Action(parse.multipartFormData) { implicit request =>
    val data=ReBoxForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname

    val color=
      if(data.color!="") {
        " -cs "+data.color.split(",").mkString(":")
      }else ""

    val command = "Rscript "+Utils.path+"R/box/boxplot.R -i "+ dutyDir+"/table.txt" +
      " -o " +dutyDir+"/out" + " -sp "+ data.spot + " -ymm " + data.ymin + ":" + data.ymax +
      color + " -ls " + data.width + ":" + data.length + " -dpi " + data.dpi + " -bw " + data.boxwidth +
      " -alp " + data.alp + " -add " + data.add + " -xts " + "sans:bold.italic:"+ data.xts +
      " -yts " + "sans:bold.italic:" + data.yts + " -lts " + "sans:bold.italic:"+data.lts

    val execCommand = new ExecCommand
    execCommand.exect(command,dutyDir+"/temp")

    if (execCommand.isSuccess) {
      creatZip(dutyDir) //替换压缩文件包
      Utils.pdf2Png(dutyDir+"/out/box.pdf",dutyDir+"/temp/box.png") //替换图片
      val pics=dutyDir+"/temp/box.png"
      Ok(Json.obj("valid"->"true","pics"->pics))
    } else {
      Ok(Json.obj("valid"->"false"))
    }

  }





  //Heatmap
  case class HeatmapData(taskname:String,col:String,scale:String,lg2:String,cluster_rows:String, cluster_cols:String,
                         color:String,color1:String,color2:String,color3:String,xfs:String,yfs:String,
                         hasnum:String,hasborder:String,hasrname:String,hascname:String)

  val HeatmapForm=Form(
    mapping (
      "taskname"->text,
      "col"->text,
      "scale"->text,
      "lg2"->text,
      "cluster_rows"->text,
      "cluster_cols"->text,
      "color"->text,
      "color1"->text,
      "color2"->text,
      "color3"->text,
      "xfs"->text,
      "yfs"->text,
      "hasnum"->text,
      "hasborder"->text,
      "hasrname"->text,
      "hascname"->text,
    )(HeatmapData.apply)(HeatmapData.unapply)
  )

  def doHeatmap=Action(parse.multipartFormData) { implicit request =>
    val data = HeatmapForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val file1=request.body.file("table1").get
    val tableFile=new File(dutyDir,"table.txt")
    val input= file1.filename
    val c=if(data.col=="") "" else "作图的列："+data.col
    val param= c+"/归一化："+data.scale+"/是否取lg2："+data.lg2+"/是否对行聚类："+
      data.cluster_rows+ "/是否对列聚类："+data.cluster_cols+"/颜色："+data.color1+":"+data.color2+":"+
      data.color3+ "/xy字体大小："+data.xfs+":"+data.yfs+ "/在格子上显示数字："+data.hasnum+"/是否显示行名："+
      data.hasrname+"/是否显示列名："+data.hascname

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"Heatmap","Heatmap 热图",input,param)
    //矩阵文件读取写入任务文件下table.txt
    file1.ref.moveTo(tableFile)

    Future{
      val color=
        if(data.color=="0") ""
        else " -c "+data.color1+":"+data.color2+":"+data.color3

      val border=
        if(data.hasborder=="TRUE") " -cbc #000000 "
        else ""

      val col=
        if(data.col=="") ""
        else " -ics "+data.col

      val command = "Rscript "+Utils.path+"R/heatmap/heatMap_plot.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out" + col + " -s " + data.scale + " -lg2 " + data.lg2 + " -cls " +
        data.cluster_rows+":"+ data.cluster_cols + color + " -fs " + data.xfs + ":" + data.yfs + border +
        " -sn " + data.hasrname + ":" + data.hascname + ":" + data.hasnum

      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (execCommand.isSuccess) {
        val finish=dutyController.updateFini(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),col+" -s " + data.scale + " -lg2 " + data.lg2)
        creatZip(dutyDir)
        Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/temp/heatmap.png")
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readHeatData(taskname:String)=Action{implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname
    val head=FileUtils.readFileToString(new File(path+"/table.txt")).trim.split("\n")
    val rnum=head.length-1
    val cnum=head(1).trim.split("\t").length-1
    //获取图片
    val pics=getReDrawPics(path)
    val downpics=path+"/out/heatmap.pdf"
    Ok(Json.obj("pics"->pics,"downpics"->downpics,"rnum"->rnum,"cnum"->cnum))
  }

  case class ReHeatData(cluster_rows:String, cluster_cols:String,rtree:String,ctree:String,rp:String,cp:String,
                         color:String,color1:String,color2:String,color3:String,cc:String,xfs:String,yfs:String,
                        hasborder:String,color4:String,hasnum:String,hasrname:String,hascname:String)

  val ReHeatForm=Form(
    mapping (
      "cluster_rows"->text,
      "cluster_cols"->text,
      "rtree"->text,
      "ctree"->text,
      "rp"->text,
      "cp"->text,
      "color"->text,
      "color1"->text,
      "color2"->text,
      "color3"->text,
      "cc"->text,
      "xfs"->text,
      "yfs"->text,
      "hasborder"->text,
      "color4"->text,
      "hasnum"->text,
      "hasrname"->text,
      "hascname"->text
    )(ReHeatData.apply)(ReHeatData.unapply)
  )

  def redrawHeatmap(taskname:String)=Action(parse.multipartFormData) { implicit request =>
    val data=ReHeatForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname
    val tableFile=new File(dutyDir,"table.txt")
    val logFile=new File(dutyDir,"log.txt")
    val log=FileUtils.readFileToString(logFile)

    val color=
      if(data.color=="0") ""
      else " -c "+data.color1+":"+data.color2+":"+data.color3

    val border=
      if(data.hasborder=="0") ""
      else " -cbc " + data.color4

    val command = "Rscript "+Utils.path+"R/heatmap/heatMap_plot.R -i "+ tableFile.getAbsolutePath +
      " -o " +dutyDir+"/out" + log + " -cls " +data.cluster_rows+":"+ data.cluster_cols + " -th " +
      data.rtree + ":" + data.ctree + " -rp " + data.rp + " -cp " + data.cp + color + " -cc " + data.cc +
      " -fs " + data.xfs + ":" + data.yfs + border + " -sn " + data.hasrname + ":" + data.hascname + ":" +
      data.hasnum

    val execCommand = new ExecCommand
    execCommand.exect(command,dutyDir+"/temp")

    if (execCommand.isSuccess) {
      creatZip(dutyDir) //替换压缩文件包
      Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/temp/heatmap.png") //替换图片
      val pics=dutyDir+"/temp/heatmap.png"
      Ok(Json.obj("valid"->"true","pics"->pics))
    } else {
      Ok(Json.obj("valid"->"false"))
    }
  }





  //CCA
  case class CCAData(taskname:String, anatype:String)

  val CCAForm=Form(
    mapping (
      "taskname"->text,
      "anatype"->text
    )(CCAData.apply)(CCAData.unapply)
  )

  def doCCA(isgroup:Boolean)=Action(parse.multipartFormData){implicit request=>
    val data=CCAForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)
    val otuFile=new File(dutyDir,"otu.txt")
    val enviFile=new File(dutyDir,"envi.txt")
    val groupFile=new File(dutyDir,"group.txt")

    val file1=request.body.file("table1").get
    val file2=request.body.file("table2").get
    val input=
      if(isgroup)
        file1.filename+"/"+file2.filename+"/"+request.body.file("table3").get.filename
      else file1.filename+"/"+file2.filename
    val param= "分析类型：" + data.anatype

    val start=dutyController.insertDuty(data.taskname,id,"CCA","CCA/RDA",input,param)
    file1.ref.moveTo(otuFile)
    file2.ref.moveTo(enviFile)

    Future{
      val command1 = "Rscript "+Utils.path+"R/cca/rda_cca_data.R -pi "+ otuFile.getAbsolutePath +
      " -ei " + enviFile.getAbsoluteFile + " -o " + dutyDir + "/out" + " -m " + data.anatype
      val execCommand1 = new ExecCommand
      execCommand1.exect(command1,dutyDir+"/temp")

      val group=
        if(isgroup){
          val file3 = request.body.file("table3").get
          file3.ref.moveTo(groupFile)
          " -g "+groupFile.getAbsolutePath
        }else ""

      val command2 = "Rscript "+Utils.path+"R/cca/rda_cca_plot.R -sai "+ dutyDir + "/out/samples.xls" +
        " -spi " + dutyDir + "/out/species.xls" + " -ei " + dutyDir + "/out/envi.xls" + " -pci " +
        dutyDir + "/out/percent.xls" + " -o " + dutyDir + "/out" + " -is 15:15" + group

      val execCommand2 = new ExecCommand
      execCommand2.exect(command2,dutyDir+"/temp")

      if (execCommand1.isSuccess&&execCommand2.isSuccess) {
        val finish=dutyController.updateFini(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"")
        creatZip(dutyDir)
        Utils.pdf2Png(dutyDir+"/out/rdacca.pdf",dutyDir+"/temp/rdacca.png")
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand1.getErrStr+"\n\n"+execCommand2.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readCCAData(taskname:String)=Action{implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val head=FileUtils.readFileToString(new File(path+"/otu.txt")).trim.split("\n")
    val gnum=head(0).trim.split("\t").drop(1).length
    val group=
      if(new File(path+"/group.txt").exists()) {
        val f=FileUtils.readLines(new File(path+"/group.txt")).asScala
        val g=f.map{_.split('\t').last}.distinct.drop(1).mkString(" , ")
        //检查group的数量与矩阵head是否一样，小于则+nogroup，相等则不变
        if(f.map{_.split('\t').head}.drop(1).length<gnum) g+" , nogroup"
        else g
      }else "无分组"
    //获取x,y轴数据
    val data=FileUtils.readLines(new File(path+"/out/samples.xls"))
    val col=data.get(0).split("\"").filter(_.trim!="").map(_.trim)
    //获取图片
    val pics=getReDrawPics(path)
    val downpics=path+"/out/rdacca.pdf"
    Ok(Json.obj("pics"->pics,"downpics"->downpics,"group"->group,"cols"->col))
  }

  case class ReCCAData(xdata:String, ydata:String,xaxis:String,yaxis:String,samfont:String,
                        samsize:String,color:String,evifont:String,color3:String,
                        color4:String,specifont:String,specisize:String,color5:String, width:String,
                       height:String,dpi:String,xts:String,yts:String,xls:String,yls:String)

  val ReCCAForm=Form(
    mapping (
      "xdata"->text,
      "ydata"->text,
      "xaxis"->text,
      "yaxis"->text,
      "samfont"->text,
      "samsize"->text,
      "color"->text,
      "evifont"->text,
      "color3"->text,
      "color4"->text,
      "specifont"->text,
      "specisize"->text,
      "color5"->text,
      "width"->text,
      "height"->text,
      "dpi"->text,
      "xts"->text,
      "yts"->text,
      "xls"->text,
      "yls"->text
    )(ReCCAData.apply)(ReCCAData.unapply)
  )

  def redrawCCA(taskname:String,showsname:Boolean,showevi:Boolean,showspeci:Boolean)=Action(parse.multipartFormData) { implicit request =>
    val data=ReCCAForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname
    val groupFile=new File(dutyDir,"group.txt")

    val xyr=data.xdata.substring(3) + ":" + data.ydata.substring(3)
    val sname=
      if(showsname) " -sspt TRUE:TRUE" else " -sspt TRUE:FALSE"
    val sat=
      if(!groupFile.exists&&data.color!="") " -sat " + data.color + ":" + data.samfont else " -sat #1E90FF:" + data.samfont
    val gc=if(data.color!="") " -gc " + data.color.split(",").mkString(":") else ""
    val group=
      if(groupFile.exists) " -g " + groupFile.getAbsolutePath else ""
    val evi=
      if(showevi) " -sepl TRUE:TRUE" else " -sepl FALSE:FALSE"
    val speci=
      if(showspeci) " -sppt TRUE:TRUE" else " -sppt FALSE:FALSE"

    val command =
      "Rscript "+Utils.path+"R/cca/rda_cca_plot.R -sai "+ dutyDir + "/out/samples.xls -spi " + dutyDir +
        "/out/species.xls -ei " + dutyDir + "/out/envi.xls -pci " + dutyDir + "/out/percent.xls -o " +
        dutyDir + "/out" + group + gc + " -is " + data.width + ":" + data.height + " -xyr " + xyr + " -op " +
        data.xaxis + ":" + data.yaxis + sname + " -sap #000000:" + data.samsize + sat +
        group + evi + " -ett " + data.color3 + ":" + data.evifont + " -elc " + data.color4 +
        speci + " -spp " + data.color5 + ":" + data.specisize + " -spt " + data.color5 + ":" + data.specifont +
        " -dpi " + data.dpi + " -xts  sans:bold.italic:" + data.xts + " -yts  sans:bold.italic:" + data.yts +
        " -xls  sans:bold.italic:" + data.xls + " -yls  sans:bold.italic:" + data.yls

    println(command)

    val execCommand = new ExecCommand
    execCommand.exect(command,dutyDir+"/temp")

    if (execCommand.isSuccess) {
      creatZip(dutyDir) //替换压缩文件包
      Utils.pdf2Png(dutyDir+"/out/rdacca.pdf",dutyDir+"/temp/rdacca.png") //替换图片
      val pics=dutyDir+"/temp/rdacca.png"
      Ok(Json.obj("valid"->"true","pics"->pics))
    } else {
      Ok(Json.obj("valid"->"false"))
    }
  }

  //Net
  case class NetData(taskname:String,anatype:String)

  val NetForm=Form(
    mapping (
      "taskname"->text,
      "anatype"->text
    )(NetData.apply)(NetData.unapply)
  )

  def doNet =Action(parse.multipartFormData){implicit  request=>
    val data=NetForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val file1=request.body.file("table1").get
    val file2=request.body.file("table2").get
    val tableFile=new File(dutyDir,"table.txt")
    val eviFile=new File(dutyDir,"evi.txt")
    val input= file1.filename+"/"+file2.filename
    val param= "相关分析方法:"+data.anatype

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"Net","网络图",input,param)
    //矩阵文件读取写入任务文件下table.txt
    file1.ref.moveTo(tableFile)
    file2.ref.moveTo(eviFile)

    Future{
      val command = "Rscript "+Utils.path+"R/net/network_data.R -i2 "+ tableFile.getAbsolutePath + " -i1 " +
        eviFile.getAbsolutePath + " -o " +dutyDir+"/out/result.xls" + " -m1 " + data.anatype + " -m2 " + data.anatype

      println(command)

      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (execCommand.isSuccess) {
        val finish=dutyController.updateFini(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt")," -m1 " + data.anatype + " -m2 " + data.anatype)
        creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readNetData(taskname:String)=Action{implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname
    val cat=(Json.obj("name"->"GeneId"),Json.obj("name"->"Environment"))

    val genus=FileUtils.readLines(new File("F:\\CloudPlatform\\R\\net\\data\\genus.txt")).asScala
    val g=genus.map{line=>
      line.trim.split("\t").head
    }

    val evi=FileUtils.readLines(new File("F:\\CloudPlatform\\R\\net\\data\\env.txt")).asScala
    val e=evi.map{line=>
      line.trim.split("\t").head
    }

    val list=e.drop(1)++g.drop(1)
    var count=0;
    val nodes=list.map{x=>
      count=count+1
      val evi= if(count<=e.drop(1).length) 1 else 0
      Json.obj("name"->x,"value"->1,"category"->evi)
    }

    val data=FileUtils.readLines(new File("F:\\CloudPlatform\\R\\net\\test\\result.xls")).asScala
    val links=data.map{x=>
      val e = x.split("\"").filter(_.trim!="")
      val source=list.indexOf(e(1))
      val target=list.indexOf(e(2))
      Json.obj("source"->source,"target"->target)
    }.drop(1)

    val rows=Json.obj("type"->"force","categories"->cat,"nodes"->nodes,"links"->links)
    Ok(Json.obj("rows"->rows))
  }


  case class GoData(taskname:String,species:String,human:String)

  val GoForm=Form(
    mapping (
      "taskname"->text,
      "species"->text,
      "human"->text
    )(GoData.apply)(GoData.unapply)
  )

  def doGo=Action(parse.multipartFormData){implicit request=>
    val data=GoForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val file1=request.body.file("table1").get

    val tableFile=new File(dutyDir,"table.txt")
    val input= file1.filename
    val param= "选择物种:"+data.species+"/是否为人类："+data.human

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"GO","GO富集分析",input,param)
    //矩阵文件读取写入任务文件下table.txt
    file1.ref.moveTo(tableFile)

    Future{
      val command1 = "java -jar" + (Utils.path + "R/gokegg/data/gokoData-1.0-SNAPSHOT.jar").unixPath +" -m "+
        data.species + " -i " + tableFile.getAbsolutePath.unixPath + " -o " + (dutyDir + "/out").unixPath +
        " -human " + data.human + " -n gokegg"
      val command2 =
        "Rscript " + Utils.path + "R/gokegg/plot/goko_stack_plot.R -i " + dutyDir + "/out/gokegg.Ko.bar.dat" +
          " -o " + dutyDir + "/out" + " -in ko_stack -sm 10 -n 15 -xts sans:bold.italic:18 -yts sans:bold.italic:18"
      val command3 =
        "Rscript " + Utils.path + "R/gokegg/plot/goko_stack_plot.R -i " + dutyDir + "/out/gokegg.Go.bar.dat" +
          " -o " + dutyDir + "/out" + " -in go_stack -sm 10 -n 15 -xts sans:bold.italic:18 -yts sans:bold.italic:18"
      val command4=
        "Rscript " + Utils.path + "R/gokedd/plot/goko_dodge_plot.R -i " + dutyDir + "/out/gokegg.Ko.bar.dat" +
          " -o " + dutyDir + "/out" + " -in ko_dodge -sm 10 -n 15 -xts sans:bold.italic:18 -yts sans:bold.italic:18"
      val command5=
        "Rscript " + Utils.path+"R/gokegg/plot/goko_dodge_plot.R -i " + dutyDir + "/out/gokegg.Go.bar.dat" +
          " -o " + dutyDir + "/out" + " -in go_dodge -sm 10 -n 15 -xts sans:bold.italic:18 -yts sans:bold.italic:18"

      println(command1)
      val command=Array(command1,command2,command3,command4,command5)

      val execCommand = new ExecCommand
      execCommand.exec(command)

      if (execCommand.isSuccess) {
        val finish=dutyController.updateFini(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt")," -m " + data.species + " -human " + data.human)
        creatZip(dutyDir)
        Utils.pdf2Png(dutyDir+"/out/gokegg.Go.enrich.pdf",dutyDir+"/temp/gokegg.Go.enrich.png")
        Utils.pdf2Png(dutyDir+"/out/gokegg.Ko.enrich.pdf",dutyDir+"/temp/gokegg.Ko.enrich.png")
        Utils.pdf2Png(dutyDir+"/out/ko_stack.pdf",dutyDir+"/temp/ko_stack.png")
        Utils.pdf2Png(dutyDir+"/out/ko_dodge.pdf",dutyDir+"/temp/ko_dodge.png")
        Utils.pdf2Png(dutyDir+"/out/go_stack.pdf",dutyDir+"/temp/go_stack.png")
        Utils.pdf2Png(dutyDir+"/out/go_dodge.pdf",dutyDir+"/temp/go_dodge.png")
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readGoData(taskname:String)=Action{implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname
    //获取图片
    val pics=(path+"/temp/go_stack.png",path+"/temp/go_dodge.png",path+"/temp/ko_stack.png",path+"/temp/ko_dodge.png",path+"/temp/gokegg.Go.enrich.png",path+"/temp/gokegg.Ko.enrich.png")
//      getReDrawPics(path)
    val downpics=(path+"/out/go_stack.pdf",path+"/out/go_dodge.pdf",path+"/out/ko_stack.pdf",path+"/out/ko_dodge.pdf",path+"/out/gokegg.Go.enrich.pdf",path+"/out/gokegg.Ko.enrich.pdf")
    Ok(Json.obj("pics"->pics,"downpics"->downpics))
  }

  case class ReGoData(g:String,n:String,sm:String,br:String,cs:String,width:String,height:String,dpi:String,xts:String,yts:String,lts:String)

  val ReGoForm=Form(
    mapping (
      "g"->text,
      "n"->text,
      "sm"->text,
      "br"->text,
      "cs"->text,
      "width"->text,
      "height"->text,
      "dpi"->text,
      "xts"->text,
      "yts"->text,
      "lts"->text
    )(ReGoData.apply)(ReGoData.unapply)
  )

  def redrawGO(taskname:String,types:String,kogo:String)=Action(parse.multipartFormData) { implicit request =>
    val data=ReGoForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname
    val r=
      if(types=="stack") "R/gokegg/plot/goko_stack_plot.R" else "R/gokegg/plot/goko_dodge_plot.R"

    val g=
      if(types=="stack") "" else " -g " + data.g

    val i=
      if(kogo=="ko") dutyDir + "/out/gokegg.Ko.bar.dat" else dutyDir + "/out/gokegg.Go.bar.dat"

    val in=
      if(types=="stack") {
        if(kogo=="ko") "ko_stack" else "go_stack"
      } else {
        if(kogo=="ko") "ko_dodge" else "go_dodge"
      }

    val cs=
      if(data.cs=="") "" else " -cs " + data.cs.split(",").mkString(":") + ":#E41A1C:#FFC0CB:#1E90FF:#00BFFF:#FF8C00:#FFDEAD:#4DAF4A:#90EE90:#9692C3:#CDB4FF:#40E0D0:#00FFFF"

    val command =
      "Rscript " + Utils.path + r + " -i "+ i + " -o " + dutyDir + "/out -n " + data.n + " -sm " + data.sm + " -br " + data.br +
        cs + " -is " + data.width + ":" + data.height + " -dpi " + data.dpi + " -xts sans:bold.italic:" + data.xts +
        " -yts sans:bold.italic:" + data.yts + " -lts sans:bold.italic:" + data.lts + " -in " + in + g + " -if pdf"

    println(command)

    val execCommand = new ExecCommand
    execCommand.exect(command,dutyDir+"/temp")

    if (execCommand.isSuccess) {
      creatZip(dutyDir) //替换压缩文件包
      Utils.pdf2Png(dutyDir+"/out/"+in+".pdf",dutyDir+"/temp/"+in+".png") //替换图片
      val pics=dutyDir+"/temp/"+in+".png"
      Ok(Json.obj("valid"->"true","pics"->pics))
    } else {
      Ok(Json.obj("valid"->"false"))
    }
  }


  case class KeggData(taskname:String,text1:String,txdata:String,para22:String,para33:String,para44:String,text2:String,parameter4:String,parameter3:String,text3:String)

  val KeggForm=Form(
    mapping (
      "taskname"->text,
      "text1"->text,
      "txdata"->text,
      "para22"->text,
      "para33"->text,
      "para44"->text,
      "text2"->text,
      "parameter4"->text,
      "parameter3"->text,
      "text3"->text
    )(KeggData.apply)(KeggData.unapply)
  )

  def doKegg(id:String,userefer:Boolean,desc:Boolean)=Action(parse.multipartFormData){implicit request=>
    val data=KeggForm.bindFromRequest.get
    val checkTaskname= Await.result(dutydao.checkTaskName(id,data.taskname),Duration.Inf)
    if(data.text1.isEmpty && data.txdata.isEmpty){
      Ok(Json.obj("valid" -> "false", "message" -> "请上传或手动输入基因列表文件！"))
    }
    else if(!userefer && data.text2.isEmpty){
      Ok(Json.obj("valid" -> "false", "message" -> "请上传背景基因表！"))
    }
    else if(desc && data.text3.isEmpty){
      Ok(Json.obj("valid" -> "false", "message" -> "请上传基因差异倍数表！"))
    }
    else if(checkTaskname.length==1) { //2.check taskname重复
      Ok(Json.obj("valid" -> "false", "message" -> "任务编号已存在，请换一个编号！"))
    }
    else {
      //数据库加入duty
//      dutyController.insertDuty(data.taskname,id,"2","KEGG富集分析")
      //在用户下创建任务文件夹和结果文件夹
      val dutyDir=creatUserDir(id,data.taskname)
      val geneFile=new File(dutyDir,"gene.txt")
      val keggFile=new File(dutyDir,"kegg.txt")
      val diffFile=new File(dutyDir,"diff.txt")

      if(!data.text1.isEmpty){
        val file = request.body.file("table1").get
        val genedatas = FileUtils.readFileToString(file.ref.file)
        FileUtils.writeStringToFile(geneFile, genedatas)
      } else{
        FileUtils.writeStringToFile(geneFile, data.txdata)
      }

      if(userefer){
        //读取原有库放入kegg.txt
        data.para22+data.para33+data.para44
      }else{
        val file = request.body.file("table2").get
        val keggdatas = FileUtils.readFileToString(file.ref.file)
        FileUtils.writeStringToFile(keggFile, keggdatas)
        keggFile.getAbsolutePath+data.parameter4+data.parameter3
      }

      if(desc){
        val file = request.body.file("table3").get
        val diffdatas = FileUtils.readFileToString(file.ref.file)
        FileUtils.writeStringToFile(diffFile, diffdatas)
        diffFile.getAbsolutePath
      }


      val command =""

      val execCommand = new ExecCommand
      //exec需要指定结果输出路径的时候，不指定默认本地任务路径
      execCommand.exect(command,dutyDir+"/out")
      if (execCommand.isSuccess) {
        val excel = execCommand.getOutStr
//        dutydao.updateFini(id,data.taskname)
        Ok(Json.obj("excel" -> excel,"taskname"->data.taskname,"userId"->id,"path"->dutyDir))
      } else {
        dutydao.updateFailed(id,data.taskname)
        Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
        //运行失败，修改状态，删除建立的文件夹
      }
    }

  }

  val userDutyDir=Utils.path+"users/"

  //创建用户任务文件夹和结果文件夹
  def creatUserDir(uid:String,taskname:String)={
    new File(userDutyDir+uid+"/"+taskname).mkdir()
    new File(userDutyDir+uid+"/"+taskname+"/out").mkdir()
    new File(userDutyDir+uid+"/"+taskname+"/temp").mkdir()

    userDutyDir+uid+"/"+taskname
  }


  def creatZip(path:String) ={
    val target=path+"/outcome.zip"
    new File(target).createNewFile()
    CompressUtil.zip(path+"/out",target)
  }




  def getPics(id:String,taskname:String)= {
    val files = new File(Utils.path+"/users/"+id+"/"+taskname+"/out").listFiles().filter(_.getName.contains("png")).map(_.getAbsolutePath)
    files
  }

  def getFiles(id:String,taskname:String)= {
    val files = new File(Utils.path+"/users/"+id+"/"+taskname+"/out").listFiles().map(_.getAbsolutePath)
    files
  }

  def getDownloadFiles(taskname:String,soft:String)=Action {implicit request=>
    val id=request.session.get("userId").get
    val files = new File(Utils.path+"/users/"+id+"/"+taskname+"/out").listFiles().map(_.getAbsolutePath)
    Ok(Json.obj("files"->files))
  }

  def getReDrawPics(path:String)={
    val pics = new File(path+"/temp").listFiles().filter(_.getName.contains("png")).map(_.getAbsolutePath)
    pics
  }

  val pcaIns=("pca分析结果图","rotation","sdev","pca分析结果")
  val boxIns=("boxplot盒型图","sd")

  def getInstruction(soft:String) ={
    val ins=soft match {
      case "PCA" => pcaIns
      case "Boxplot" => boxIns
    }
    ins
  }


  def download(path:String) = Action{implicit request=>
    val file=new File(path)
    Ok.sendFile(file).withHeaders(
      //缓存
      CACHE_CONTROL -> "max-age=3600",
      CONTENT_DISPOSITION -> ("attachment; filename=" + file.getName()),
      CONTENT_TYPE -> "application/x-download"
    )

  }

  def downloadZip(taskname:String)=Action{implicit request=>
    val path=new File(userDutyDir+request.session.get("userId").get+"/"+taskname+"/outcome.zip")
    Ok.sendFile(path).withHeaders(
      //缓存
      CACHE_CONTROL -> "max-age=3600",
      CONTENT_DISPOSITION -> ("attachment; filename=" + path.getName()),
      CONTENT_TYPE -> "application/x-download"
    )
  }

  def downloadExamples(name:String)=Action{implicit request=>
    val file=new File(Utils.path+"files/examples/"+name)
    Ok.sendFile(file).withHeaders(
      CACHE_CONTROL->"max-age=3600",
      CONTENT_DISPOSITION -> ("attachment; filename=" + file.getName()),
      CONTENT_TYPE -> "application/x-download"
    )
  }

  def getPic(path:String,num:Double) = Action{implicit request=>
    val file = new File(path)
    SendImg(file,request.headers)
  }

  def SendImg(file: File,headers:Headers) = {
    val lastModifiedStr = file.lastModified().toString
    val MimeType = "image/jpg"
    val byteArray = Files.readAllBytes(file.toPath)
    val ifModifiedSinceStr = headers.get(IF_MODIFIED_SINCE)
    if (ifModifiedSinceStr.isDefined && ifModifiedSinceStr.get == lastModifiedStr) {
      NotModified
    } else {
      Ok(byteArray).as(MimeType).withHeaders(LAST_MODIFIED -> file.lastModified().toString)
    }
  }

}
