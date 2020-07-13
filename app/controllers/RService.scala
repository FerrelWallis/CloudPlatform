package controllers

import java.io.File
import java.nio.file.Files

import dao.dutyDao
import javax.inject.Inject
import org.apache.commons.io.FileUtils
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Headers, Result}
import utils.{CompressUtil, ExecCommand, MyStringTool, Utils}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.collection.JavaConverters._
import utils.Implicits._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class RService @Inject()(cc: ControllerComponents,dutydao:dutyDao,dutyController: DutyController)(implicit exec: ExecutionContext) extends AbstractController(cc) with MyStringTool{


  //PCA
  case class PCA1Data(taskname:String,showname:String,showerro:String,txdata1:String,txdata2:String,drawcircle:String)

  val PCA1Form: Form[PCA1Data] =Form(
    mapping (
      "taskname"->text,
      "showname"->text,
      "showerro"->text,
      "txdata1"->text,
      "txdata2"->text,
      "drawcircle"->text
    )(PCA1Data.apply)(PCA1Data.unapply)
  )

  def doPCA(isgroup:Boolean,table:String,group:String)=Action(parse.multipartFormData){implicit request=>
    val data=PCA1Form.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹

    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")
    val input=
    if(table=="2") {
      val file1=request.body.file("table1").get
      //矩阵文件读取写入任务文件下table.txt
      file1.ref.moveTo(tableFile)
      if(isgroup) file1.filename+"/"+ request.body.file("table2").get.filename
      else file1.filename
    } else{
      FileUtils.writeStringToFile(tableFile, data.txdata1)
      if(isgroup && group=="2") request.body.file("table2").get.filename
      else "无"
    }

    val (param,co)=
      if(isgroup) ("是否显示样本名：" + data.showname + "/是否分组绘图：" +
        isgroup + "/是否绘制分组圈：" + data.drawcircle,"#CD0000:#3A89CC:#769C30:#D99536:#7B0078:#BFBC3B:#E2609F:#00688B:#C10077:#CAAA76" +
        ":#EEEE00:#458B00:#8B4513:#008B8B:#6E8B3D:#8B7D6B:#7FFF00:#CDBA96:#ADFF2F")
      else ("是否显示样本名：" + data.showname + "/是否分组绘图：" + isgroup,"#48FF75")

    val elements= Json.obj("xdata"->"PC1","ydata"->"PC2","width"->"15","length"->"12","showname"->data.showname,"showerro"->data.showerro,"color"->co,"resolution"->"300","xts"->"15","yts"->"15","xls"->"17","yls"->"17","lts"->"14").toString()

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"PCA","主成分分析（PCA）",input,param,elements)


    Future{
      val command1 = "Rscript "+Utils.path+"R/pca/pca_data.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out" + " -sca TRUE"

      println(command1)

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
            FileUtils.writeStringToFile(groupFile, "#SampleID\tGroup\n"+data.txdata2)
          }
          " -g "+groupFile.getAbsolutePath
        }else ""

      val name=if(data.showname.equals("TRUE") && groupFile.exists()){
        val f=FileUtils.readLines(groupFile).asScala
        val n=f.map{_.split('\t').last}.distinct.drop(1).mkString(",")
        " -b " + n
      }else if(data.showname.equals("TRUE") && !groupFile.exists()){
        " -sl TRUE"
      }
      else ""

      val command2 = "Rscript "+Utils.path+"R/pca/pca_plot.R -i "+ dutyDir+"/out/pca.x.xls" +
        " -si " + dutyDir+"/out/pca.sdev.xls" + group + " -o " +dutyDir+"/out" + name + " -c " +
        data.drawcircle + " -if pdf -ss " + data.showerro
      println(command2)

      val execCommand2 = new ExecCommand
      execCommand2.exect(command2,dutyDir+"/temp")

      if (execCommand1.isSuccess && execCommand2.isSuccess) {
        val finish=dutyController.updateFini(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),group+name+" -c "+data.drawcircle)
        creatZip(dutyDir)
        Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.png")
        Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.tiff")
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand1.getErrStr+"\n\n"+execCommand2.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readPCAData(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val elements=jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).elements)
    val color=elements("color").split(":")

    val head=FileUtils.readFileToString(new File(path+"/table.txt")).trim.split("\n")
    val gnum=head(0).trim.split("\t").drop(1).length
    //获取分组
    val group=
      if(new File(path+"/group.txt").exists()) {
        val f=FileUtils.readLines(new File(path+"/group.txt")).asScala
        val g=f.map{_.split('\t').last}.distinct.drop(1)
        //检查group的数量与矩阵head是否一样，小于则+nogroup，相等则不变
        if(f.map{_.split('\t').head}.drop(1).length<gnum) g.append("nogroup")
        g.toArray
      }else Array("nogroup")

    //获取x,y轴数据
    val data=FileUtils.readLines(new File(path+"/out/pca.x.xls"))
    val col=data.get(0).split("\"").filter(_.trim!="").map(_.trim)
    //获取图片
    val pics=getReDrawPics(path)
    val (downpics,downpng,downtiffs)=(path+"/out/pca.pdf",path+"/out/pca.png",path+"/out/pca.tiff")
    Ok(Json.obj("group"->group,"cols"->col,"pics"->pics,"downpng"->downpng,"downpics"->downpics,"downtiffs"->downtiffs,"elements"->elements,"color"->color))
  }

  case class RePCAData(xdata:String,ydata:String,showname:String,showerro:String,color:String,width:String,
                       length:String,resolution:String,xts:String,yts:String,
                       xls:String,yls:String,lts:String)

  val RePCAForm: Form[RePCAData] =Form(
    mapping (
      "xdata"->text,
      "ydata"->text,
      "showname"->text,
      "showerro"->text,
      "color"->text,
      "width"->text,
      "length"->text,
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

    val elements= Json.obj("xdata"->data.xdata,"ydata"->data.ydata,"width"->"15","length"->"12","showname"->data.showname,"showerro"->data.showerro,"color"->data.color,"resolution"->data.resolution,"xts"->data.xts,"yts"->data.yts,"xls"->data.xls,"yls"->data.yls,"lts"->data.lts).toString()

    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    //读取log.txt 包括-g -b -c
    val log=FileUtils.readFileToString(new File(dutyDir+"/log.txt"))
    val c=
      if(!groupFile.exists()) " -oc "+data.color
      else " -cs "+ data.color

    val name=if(data.showname.equals("TRUE") && groupFile.exists()){
      val f=FileUtils.readLines(groupFile).asScala
      val n=f.map{_.split('\t').last}.distinct.drop(1).mkString(",")
      " -b " + n
    }else if(data.showname.equals("TRUE") && !groupFile.exists()){
      " -sl TRUE"
    }
    else ""

    val command = "Rscript "+Utils.path+"R/pca/pca_plot.R -i "+ dutyDir+"/out/pca.x.xls" +
      " -si " + dutyDir+"/out/pca.sdev.xls" + " -o " +dutyDir+"/out" + " -pxy "+ data.xdata+":"+ data.ydata +log +
    " -is "+ data.width + ":" + data.length + c + " -dpi " + data.resolution + " -xts " + "sans:plain:"+data.xts +
    " -yts " + "sans:plain:"+data.yts + " -xls " + "sans:plain:"+data.xls + " -yls " + "sans:plain:"+data.yls +
      " -lts " + "sans:plain:" + data.lts + name + " -if pdf -ss " + data.showerro

    println(command)

    val execCommand = new ExecCommand
    execCommand.exect(command,dutyDir+"/temp")

    if (execCommand.isSuccess) {
      creatZip(dutyDir) //替换压缩文件包
      Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.tiff") //替换图片
      val pics=dutyDir+"/out/pca.png"
      Ok(Json.obj("valid"->"true","pics"->pics))
    } else {
      Ok(Json.obj("valid"->"false"))
    }

  }



  //Boxplot
  case class BoxplotData(taskname:String,spot:String,ymin:String,ymax:String)

  val BoxplotForm: Form[BoxplotData] = Form(
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

    val elements= Json.obj("spot"->data.spot,"ymin"->data.ymin,"ymax"->data.ymax,"boxwidth"->"0.7","alp"->"0.8","add"->"1","color"->"#E41A1C:#1E90FF:#FF8C00:#4DAF4A:#984EA3:#40E0D0:#F4AAC4:#DC414B:#957624:#43B43C","width"->"12","length"->"10","dpi"->"300","xts"->"15","yts"->"15","lts"->"14").toString()

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"Boxplot","Boxplot 盒型图",input,param,elements)
    //矩阵文件读取写入任务文件下table.txt
    file1.ref.moveTo(tableFile)

    Future{
      val command = "Rscript "+Utils.path+"R/box/boxplot.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out" + " -sp " + data.spot + ylim + " -ls 12:10"
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (execCommand.isSuccess) {
        val finish=dutyController.updateFini(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"")
        creatZip(dutyDir)
        Utils.pdf2Png(dutyDir+"/out/box.pdf",dutyDir+"/out/box.png")
        Utils.pdf2Png(dutyDir+"/out/box.pdf",dutyDir+"/out/box.tiff")
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readBoxData(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val elements=jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).elements)
    val color=elements("color").split(":")

    val head=FileUtils.readFileToString(new File(path+"/table.txt")).trim.split("\n")
    val group=head(0).trim.split("\t")

    //获取图片
    val pics=getReDrawPics(path)
    val (downpics,downpng,downtiffs)=(path+"/out/box.pdf",path+"/out/box.png",path+"/out/box.tiff")
    Ok(Json.obj("group"->group,"pics"->pics,"downpng"->downpng,"downpics"->downpics,"downtiffs"->downtiffs,"elements"->elements,"color"->color))
  }

  case class ReBoxData(spot:String,ymin:String,ymax:String,color:String,width:String,length:String,
                       dpi:String,boxwidth:String,alp:String,add:String,xts:String,yts:String,lts:String)

  val ReBoxForm: Form[ReBoxData] =Form(
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

    val elements= Json.obj("spot"->data.spot,"ymin"->data.ymin,"ymax"->data.ymax,"boxwidth"->data.boxwidth,"alp"->data.alp,"add"->data.add,"color"->data.color,"width"->data.width,"length"->data.length,"dpi"->data.dpi,"xts"->data.xts,"yts"->data.yts,"lts"->data.lts).toString()
    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    val ymm=if(data.ymin==""||data.ymax=="") "" else " -ymm " + data.ymin + ":" + data.ymax

    val command = "Rscript "+Utils.path+"R/box/boxplot.R -i "+ dutyDir+"/table.txt" + " -o " +dutyDir+"/out" +
      " -sp "+ data.spot + ymm + " -ls " + data.width + ":" + data.length + " -dpi " + data.dpi + " -bw " +
      data.boxwidth + " -alp " + data.alp + " -add " + data.add + " -xts " + "sans:bold.italic:"+ data.xts +
      " -yts " + "sans:bold.italic:" + data.yts + " -lts " + "sans:bold.italic:" + data.lts + " -cs " + data.color

    val execCommand = new ExecCommand
    execCommand.exect(command,dutyDir+"/temp")

    if (execCommand.isSuccess) {
      creatZip(dutyDir) //替换压缩文件包
      Utils.pdf2Png(dutyDir+"/out/box.pdf",dutyDir+"/out/box.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/box.pdf",dutyDir+"/out/box.tiff") //替换图片
      val pics=dutyDir+"/out/box.png"
      Ok(Json.obj("valid"->"true","pics"->pics))
    } else {
      Ok(Json.obj("valid"->"false"))
    }

  }



  //Heatmap
  case class HeatmapData(taskname:String,col:String,scale:String,lg:String,cluster_rows:String,cluster_cols:String,
                         color:String,xfs:String,yfs:String,hasnum:String,hasborder:String,hasrname:String,hascname:String)

  val HeatmapForm: Form[HeatmapData] =Form(
    mapping (
      "taskname"->text,
      "col"->text,
      "scale"->text,
      "lg"->text,
      "cluster_rows"->text,
      "cluster_cols"->text,
      "color"->text,
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
    val param= c+"/归一化："+data.scale+"/是否取lg："+data.lg+"/是否对行聚类："+
      data.cluster_rows+ "/是否对列聚类："+data.cluster_cols+"/颜色："+data.color + "/xy字体大小："+data.xfs+":"+data.yfs+ "/在格子上显示数字："+data.hasnum+"/是否显示行名："+
      data.hasrname+"/是否显示列名："+data.hascname

    val elements=Json.obj("col"->data.col,"scale"->data.scale,"lg"->data.lg,"cluster_rows"->data.cluster_rows,"cluster_cols"->data.cluster_cols,"rtree"->"50","ctree"->"50","rp"->"1","cp"->"1","color"->data.color,"cc"->"30","xfs"->data.xfs,"yfs"->data.yfs,"hasborder"->data.hasborder,"colorborder"->"#000000","hasnum"->data.hasnum,"hasrname"->data.hasrname,"hascname"->data.hascname).toString()

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"Heatmap","Heatmap 热图",input,param,elements)
    //矩阵文件读取写入任务文件下table.txt
    file1.ref.moveTo(tableFile)

    Future{
      val color= " -c "+ data.color

      val border=
        if(data.hasborder=="TRUE") " -cbc #000000 "
        else ""

      val col=
        if(data.col=="") ""
        else " -ics "+data.col

      val command = "Rscript "+Utils.path+"R/heatmap/heatMap_plot.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out" + col + " -s " + data.scale + " -lg " + data.lg + " -cls " +
        data.cluster_rows+":"+ data.cluster_cols + color + " -fs " + data.xfs + ":" + data.yfs + border +
        " -sn " + data.hasrname + ":" + data.hascname + ":" + data.hasnum

      println(command)

      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (execCommand.isSuccess) {
        val finish=dutyController.updateFini(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"")
        creatZip(dutyDir)
        Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.png")
        Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.tiff")
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readHeatData(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val elements=jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).elements)

    val head=FileUtils.readFileToString(new File(path+"/table.txt")).trim.split("\n")
    val rnum=head.length-1
    val cnum=
      if(elements("col")=="") head(1).trim.split("\t").length-1
      else elements("col").split(",").length
    //获取图片
    val pics=getReDrawPics(path)
    val (downpics,downpng,downtiffs)=(path+"/out/heatmap.pdf",path+"/out/heatmap.png",path+"/out/heatmap.tiff")
    Ok(Json.obj("pics"->pics,"downpng"->downpng,"downpics"->downpics,"downtiffs"->downtiffs,"rnum"->rnum,"cnum"->cnum,"elements"->elements,"allcol"->(head(1).trim.split("\t").length-1)))
  }

  case class ReHeatData(col:String, scale:String, lg:String, cluster_rows:String, cluster_cols:String, rtree:String, ctree:String,
                        rp:String, cp:String, color:String, designcolor:String, cc:String, xfs:String, yfs:String,
                        hasborder:String, colorborder:String, hasnum:String, hasrname:String, hascname:String)

  val ReHeatForm: Form[ReHeatData] =Form(
    mapping (
      "col"->text,
      "scale"->text,
      "lg"->text,
      "cluster_rows"->text,
      "cluster_cols"->text,
      "rtree"->text,
      "ctree"->text,
      "rp"->text,
      "cp"->text,
      "color"->text,
      "designcolor"->text,
      "cc"->text,
      "xfs"->text,
      "yfs"->text,
      "hasborder"->text,
      "colorborder"->text,
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

    val color= if(data.color=="0") data.designcolor else data.color
    val border= if(data.hasborder=="FALSE") "" else " -cbc " + data.colorborder
    val ics= if(data.col=="") "" else " -ics " + data.col

    val elements=Json.obj("col"->data.col,"scale"->data.scale,"lg"->data.lg,"cluster_rows"->data.cluster_rows,"cluster_cols"->data.cluster_cols,"rtree"->data.rtree,"ctree"->data.ctree,"rp"->data.rp,"cp"->data.cp,"color"->color,"cc"->data.cc,"xfs"->data.xfs,"yfs"->data.yfs,"hasborder"->data.hasborder,"colorborder"->data.colorborder,"hasnum"->data.hasnum,"hasrname"->data.hasrname,"hascname"->data.hascname).toString()
    println(elements)
    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    val head=FileUtils.readFileToString(tableFile).trim.split("\n")
    val cnum= if(data.col=="") (head(1).trim.split("\t").length-1).toString else data.col.split(",").length.toString

    val command = "Rscript "+Utils.path+"R/heatmap/heatMap_plot.R -i "+ tableFile.getAbsolutePath +
      " -o " +dutyDir+"/out -s " + data.scale + " -lg " + data.lg + ics + " -cls " +data.cluster_rows+":"+
      data.cluster_cols + " -th " + data.rtree + ":" + data.ctree + " -rp " + data.rp + " -cp " + data.cp +
      " -c " + color + " -cc " + data.cc + " -fs " + data.xfs + ":" + data.yfs + border + " -sn " +
      data.hascname + ":" + data.hasrname + ":" + data.hasnum

    val execCommand = new ExecCommand
    execCommand.exect(command,dutyDir+"/temp")

    if (execCommand.isSuccess) {
      creatZip(dutyDir) //替换压缩文件包
      Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.tiff") //替换图片
      val pics=dutyDir+"/out/heatmap.png"
      Ok(Json.obj("valid"->"true","pics"->pics,"cnum"->cnum))
    } else {
      Ok(Json.obj("valid"->"false"))
    }
  }



  //CCA
  case class CCAData(taskname:String, anatype:String)

  val CCAForm: Form[CCAData] =Form(
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
    val (input,co)=
      if(isgroup)
        (file1.filename+"/"+file2.filename+"/"+request.body.file("table3").get.filename,"#336666:#996633:#CCCC33:#336633:#990033:#FFCC99:#333366:#669999:#996600")
      else (file1.filename+"/"+file2.filename,"#1E90FF")
    val param= "分析类型：" + data.anatype

    val (xdata,ydata)=
      if(data.anatype=="RDA") ("RDA1","RDA2") else ("CCA1","CCA2")

    val elements=
      Json.obj("xdata"->xdata,"ydata"->ydata,"xaxis"->"0","yaxis"->"0","samsize"->"6","color"->co,"showsname"->"true","samfont"->"7","showevi"->"true","color1"->"#E41A1C","evifont"->"7","color2"->"#E41A1C","showspeci"->"true","specifont"->"7","specisize"->"6","color3"->"#FF8C00","width"->"15","height"->"15","dpi"->"300","xts"->"16","yts"->"16","xls"->"18","yls"->"18").toString()

    val start=dutyController.insertDuty(data.taskname,id,"CCA","CCA/RDA",input,param,elements)
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

      println(command1)
      println(command2)

      val execCommand2 = new ExecCommand
      execCommand2.exect(command2,dutyDir+"/temp")

      if (execCommand1.isSuccess&&execCommand2.isSuccess) {
        val finish=dutyController.updateFini(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"")
        creatZip(dutyDir)
        Utils.pdf2Png(dutyDir+"/out/rdacca.pdf",dutyDir+"/out/rdacca.png")
        Utils.pdf2Png(dutyDir+"/out/rdacca.pdf",dutyDir+"/out/rdacca.tiff")
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand1.getErrStr+"\n\n"+execCommand2.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readCCAData(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val elements=jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).elements)

    val color=elements("color").split(":")

    val head=FileUtils.readFileToString(new File(path+"/otu.txt")).trim.split("\n")
    val gnum=head(0).trim.split("\t").drop(1).length
    val group=
      if(new File(path+"/group.txt").exists()) {
        val f=FileUtils.readLines(new File(path+"/group.txt")).asScala
        val g=f.map{_.split('\t').last}.distinct.drop(1)
        //检查group的数量与矩阵head是否一样，小于则+nogroup，相等则不变
        if(f.map{_.split('\t').head}.drop(1).length<gnum) g.append("nogroup")
        g.toArray
      }else Array("无分组")

    println(group.toList)
    //获取x,y轴数据
    val data=FileUtils.readLines(new File(path+"/out/samples.xls"))
    val col=data.get(0).split("\"").filter(_.trim!="").map(_.trim)
    //获取图片
    val pics=getReDrawPics(path)
    val (downpics,downpng,downtiffs)=(path+"/out/rdacca.pdf",path+"/out/rdacca.png",path+"/out/rdacca.tiff")
    Ok(Json.obj("pics"->pics,"downpng"->downpng,"downpics"->downpics,"downtiffs"->downtiffs,"group"->group,"cols"->col,"elements"->elements,"color"->color))
  }

  case class ReCCAData(xdata:String, ydata:String,xaxis:String,yaxis:String,samfont:String,
                        samsize:String,color:String,evifont:String,color1:String,
                        color2:String,specifont:String,specisize:String,color3:String, width:String,
                       height:String,dpi:String,xts:String,yts:String,xls:String,yls:String)

  val ReCCAForm: Form[ReCCAData] =Form(
    mapping (
      "xdata"->text,
      "ydata"->text,
      "xaxis"->text,
      "yaxis"->text,
      "samfont"->text,
      "samsize"->text,
      "color"->text,
      "evifont"->text,
      "color1"->text,
      "color2"->text,
      "specifont"->text,
      "specisize"->text,
      "color3"->text,
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

    val elements=
      Json.obj("xdata"->data.xdata,"ydata"->data.ydata,"xaxis"->data.xaxis,"yaxis"->data.yaxis,"samsize"->data.samsize,"color"->data.color,"showsname"->showsname.toString,"samfont"->data.samfont,"showevi"->showevi.toString,"color1"->data.color1,"evifont"->data.evifont,"color2"->data.color2,"showspeci"->showspeci.toString,"specifont"->data.specifont,"specisize"->data.specisize,"color3"->data.color3,"width"->data.width,"height"->data.height,"dpi"->data.dpi,"xts"->data.xts,"yts"->data.yts,"xls"->data.xls,"yls"->data.yls).toString()
    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    val xyr=data.xdata.substring(3) + ":" + data.ydata.substring(3)
    val sname=
      if(showsname) " -sspt TRUE:TRUE" else " -sspt TRUE:FALSE"
    val sat=
      if(!groupFile.exists&&data.color!="") " -sat " + data.color + ":" + data.samfont else " -sat #1E90FF:" + data.samfont
    val gc=if(groupFile.exists) " -gc " + data.color else ""
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
        group + evi + " -ett " + data.color1 + ":" + data.evifont + " -elc " + data.color2 +
        speci + " -spp " + data.color3 + ":" + data.specisize + " -spt " + data.color3 + ":" + data.specifont +
        " -dpi " + data.dpi + " -xts  sans:bold.italic:" + data.xts + " -yts  sans:bold.italic:" + data.yts +
        " -xls  sans:bold.italic:" + data.xls + " -yls  sans:bold.italic:" + data.yls

    println(command)

    val execCommand = new ExecCommand
    execCommand.exect(command,dutyDir+"/temp")

    if (execCommand.isSuccess) {
      creatZip(dutyDir) //替换压缩文件包
      Utils.pdf2Png(dutyDir+"/out/rdacca.pdf",dutyDir+"/out/rdacca.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/rdacca.pdf",dutyDir+"/out/rdacca.tiff") //替换图片
      val pics=dutyDir+"/out/rdacca.png"
      Ok(Json.obj("valid"->"true","pics"->pics))
    } else {
      Ok(Json.obj("valid"->"false"))
    }
  }



  //Net
  case class NetData(taskname:String,anatype:String)

  val NetForm: Form[NetData] =Form(
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

    val elements=Json.obj("gshape"->"ellipse","color1"->"#555555","gopa"->"1","gsize"->"5","gfont"->"12","color2"->"#ffffff","eshape"->"diamond","color3"->"#5da5fb","eopa"->"1","esize"->"10","efont"->"12","color4"->"#ffffff","color5"->"#d0b7d5","opacity"->"0.4").toString()

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"NetWeight","权重网络图",input,param,elements)
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

  def readNetData(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val elements=jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).elements)

    val genus=FileUtils.readLines(new File(path+"/table.txt")).asScala
    val g=genus.map{line=>
      line.trim.split("\t").head
    }

    val evi=FileUtils.readLines(new File(path+"/evi.txt")).asScala
    val e=evi.map{line=>
      line.trim.split("\t").head
    }

    val list=e.drop(1)++g.drop(1)
    var count=0;
    val nodes=list.map{x=>
      count=count+1
      val id=list.indexOf(x).toString
      val xy=Json.obj("x"->Math.random()*500,"y"->Math.random()*500)
      val (group,score)=
        if(count<=e.drop(1).length) ("evi",elements("esize").toDouble) //环境node
        else ("gene",elements("gsize").toDouble) //基因node
      val data=Json.obj("id"->id,"name"->x,"score"->score,"group"->group)
      Json.obj("data"->data,"position"->xy,"group"->"nodes")
    }

    val result=FileUtils.readLines(new File(path+"/out/result.xls")).asScala
    var eid=0;
    val edges=result.drop(1).map{x=>
      eid=eid+1
      val id="e"+eid
      val e = x.split("\"").filter(_.trim!="")
      val source=list.indexOf(e(1))
      val target=list.indexOf(e(2))
      val weight=e(3).trim.split("\t").last.toDouble
      val lab="pvalue="+weight
      val data=Json.obj("source"->source,"target"->target,"weight"->weight,"label"->lab)
      Json.obj("data"->data,"group"->"edges","id"->id)
    }

    val rows=nodes++edges

    val node=Json.obj("selector"->"node", "style"->Json.obj("width"->"mapData(score, 0, 10, 10, 100)", "height"->"mapData(score, 0, 10, 10, 100)", "content"-> "data(name)", "font-size"-> "12px", "text-valign"-> "center", "text-halign"-> "center", "text-outline-width"-> "2px"))

    val font1=elements("gfont")+"px"
    val nodegene=Json.obj("selector"-> "node[group='gene']","style"->Json.obj( "shape"-> elements("gshape"), "background-color"-> elements("color1"), "text-outline-color"-> elements("color1"), "opacity"-> elements("gopa"), "font-size"->font1,"color"->elements("color2")))

    val font2=elements("efont")+"px"
    val nodeevi=Json.obj("selector"-> "node[group='evi']","style"->Json.obj("shape"-> elements("eshape"), "background-color"-> elements("color3"), "text-outline-color"-> elements("color3"), "opacity"-> elements("eopa"), "font-size"->font2,"color"->elements("color4")))

    val nodesele=Json.obj("selector"-> "node:selected","style"->Json.obj("border-width"-> "6px", "border-color"-> "#AAD8FF", "border-opacity"-> "0.5", "background-color"-> "#993399", "text-outline-color"-> "#993399"))

//    val edgehigh=Json.obj("selector"-> "edge.highlighted","style"->Json.obj("line-color"-> "#2a6cd6", "target-arrow-color"-> "#2a6cd6", "opacity"-> 0.7))

    val edgehigh=Json.obj("selector"-> "edge.highlighted","style"->Json.obj("line-color"-> "#2a6cd6", "target-arrow-color"-> "#2a6cd6", "opacity"-> 0.7, "label"-> "data(label)", "edge-text-rotation"-> "autorotate"))


    //    val edgesele=Json.obj("selector"-> "edge[label]:selected","style"->Json.obj("line-color"-> "#2a6cd6", "target-arrow-color"-> "#2a6cd6", "opacity"-> 0.7, "label"-> "data(label)", "edge-text-rotation"-> "autorotate"))

    val edge=Json.obj("selector"-> "edge","style"->Json.obj("curve-style"-> "haystack", "haystack-radius"-> "0.5", "opacity"-> elements("opacity"), "line-color"-> elements("color5"), "width"-> "mapData(weight, 0, 1, 1, 8)", "overlay-padding"-> "3px"))

    val selector=Array(node,nodegene,nodeevi,nodesele,edge,edgehigh)

    Ok(Json.obj("rows"->rows,"elements"->elements,"selector"->selector))
  }

  case class ReNetData(gshape:String, color1:String,gopa:String,gsize:String,gfont:String,
                       color2:String,eshape:String,color3:String,eopa:String,esize:String,
                       efont:String,color4:String,color5:String, opacity:String)

  val ReNetForm: Form[ReNetData] =Form(
    mapping (
      "gshape"->text,
      "color1"->text,
      "gopa"->text,
      "gsize"->text,
      "gfont"->text,
      "color2"->text,
      "eshape"->text,
      "color3"->text,
      "eopa"->text,
      "esize"->text,
      "efont"->text,
      "color4"->text,
      "color5"->text,
      "opacity"->text
    )(ReNetData.apply)(ReNetData.unapply)
  )

  def redrawNet(taskname:String)=Action(parse.multipartFormData) { implicit request =>
    val data=ReNetForm.bindFromRequest.get
    val id=request.session.get("userId").get

    val elements=Json.obj("gshape"->data.gshape,"color1"->data.color1,"gopa"->data.gopa,"gsize"->data.gsize,"gfont"->data.gfont,"color2"->data.color2,"eshape"->data.eshape,"color3"->data.color3,"eopa"->data.eopa,"esize"->data.esize,"efont"->data.efont,"color4"->data.color4,"color5"->data.color5,"opacity"->data.opacity).toString()
    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    Ok(Json.obj("valid"->"true"))
  }



  //GO && KEGG
  case class GoData(taskname:String,species:String,txdata1:String)

  val GoForm: Form[GoData] =Form(
    mapping (
      "taskname"->text,
      "species"->text,
      "txdata1"->text
    )(GoData.apply)(GoData.unapply)
  )

  def doGo(types:String,refer:String,table:String)=Action(parse.multipartFormData){implicit request=>
    val data=GoForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)
    val tableFile=new File(dutyDir,"table.txt")
    val koFile=new File(dutyDir,"kogo.txt")

    println(123)

    val input=
      if(table=="2"){
        val file1=request.body.file("table1").get
        file1.ref.moveTo(tableFile)
        if(refer=="TRUE"){
          file1.filename
        }else{
          val file2=request.body.file("table2").get
          file2.ref.moveTo(koFile)
          file1.filename+"/"+file2.filename
        }
      } else {
        FileUtils.writeStringToFile(tableFile, data.txdata1)
        if(refer=="TRUE"){
          "无"
        }else{
          val file3=request.body.file("table2").get
          file3.ref.moveTo(koFile)
          file3.filename
        }
      }

    val (param,model)=
      if(refer=="TRUE") {
        if(types=="ko") ("使用已有参考:"+refer+"/选择物种："+data.species,"ko_data.jar -m "+data.species)
        else ("使用已有参考:"+refer+"/选择物种："+data.species,"go_data.jar -m "+data.species)
      }
      else {
        if(types=="ko") ("使用已有参考:"+refer,"ko_data_file.jar -pathway "+koFile.getAbsolutePath)
        else ("使用已有参考:"+refer,"Go_data_file.jar -go "+koFile.getAbsolutePath)
      }

    val elements=
      Json.obj("n"->"15","br"->"0.9","g"->"FALSE","sm"->"50","cs"->"#E41A1C:#FFC0CB:#1E90FF:#00BFFF:#FF8C00:#FFDEAD:#4DAF4A:#90EE90:#9692C3:#CDB4FF:#40E0D0:#00FFFF","width"->"20","height"->"14","xts"->"13","yts"->"14","lts"->"15","dpi"->"300").toString()

    Future{
      val (command1,command2,start)=
        if(types=="ko")
          ("java -jar " + Utils.path + "R/gokegg/data/" + model + " -i " + tableFile.getAbsolutePath +
            " -o " + dutyDir + "/out/ko" , "Rscript " + Utils.path + "R/gokegg/plot/Ko_dodge_plot.R -i " + dutyDir +
            "/out/ko.Ko.bar.dat" + " -o " + dutyDir + "/out" + " -in ko_dodge -if pdf -sm 50 -n 15",
            dutyController.insertDuty(data.taskname,id,"KEGG","KEGG富集分析",input,param,elements))
        else
          ("java -jar " + Utils.path + "R/gokegg/data/" + model + " -i " + tableFile.getAbsolutePath +
            " -o " + dutyDir + "/out/go" , "Rscript " + Utils.path + "R/gokegg/plot/Go_stack_plot.R -i " + dutyDir +
            "/out/go.Go.bar.dat" + " -o " + dutyDir + "/out" + " -in go_stack -if pdf -sm 50 -n 15",
            dutyController.insertDuty(data.taskname,id,"GO","GO富集分析",input,param,elements))


      val command3 = "dos2unix "+ tableFile.getAbsolutePath
      val command4 = "dos2unix "+koFile.getAbsolutePath
      val commandpack1=
        if(koFile.exists()) Array(command3,command4)
        else Array(command3)
      val execCommand1 = new ExecCommand
      execCommand1.exec(commandpack1)

      if(execCommand1.isSuccess){
        val commandpack2=Array(command1,command2)
        val execCommand2 = new ExecCommand
        execCommand2.exec(commandpack2)

        println(command1)
        println(command2)

        if (execCommand2.isSuccess) {
          val finish=dutyController.updateFini(id,data.taskname)
          FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"")
          creatZip(dutyDir)
          if(types=="ko"){
            Utils.pdf2Png(dutyDir+"/out/ko.Ko.enrich.pdf",dutyDir+"/out/ko.Ko.enrich.png")
            Utils.pdf2Png(dutyDir+"/out/ko.Ko.enrich.pdf",dutyDir+"/out/ko.Ko.enrich.tiff")
            Utils.pdf2Png(dutyDir+"/out/ko_dodge.pdf",dutyDir+"/out/ko_dodge.png")
            Utils.pdf2Png(dutyDir+"/out/ko_dodge.pdf",dutyDir+"/out/ko_dodge.tiff")
          }else {
            Utils.pdf2Png(dutyDir+"/out/go.Go.enrich.pdf",dutyDir+"/out/go.Go.enrich.png")
            Utils.pdf2Png(dutyDir+"/out/go.Go.enrich.pdf",dutyDir+"/out/go.Go.enrich.tiff")
            Utils.pdf2Png(dutyDir+"/out/go_stack.pdf",dutyDir+"/out/go_stack.png")
            Utils.pdf2Png(dutyDir+"/out/go_stack.pdf",dutyDir+"/out/go_stack.tiff")
          }
        } else {
          dutydao.updateFailed(id,data.taskname)
          FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand2.getErrStr+"\n\n运行失败！")
        }
      }else{
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand1.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readGoData(types:String,taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val elements=jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).elements)
    val color=elements("cs").split(":")
    //获取图片
    val (pics,downpics,downpngs,downtiffs)=
      if(types=="ko") ((path+"/out/ko_dodge.png",path+"/out/ko.Ko.enrich.png"),(path+"/out/ko_dodge.pdf",path+"/out/ko.Ko.enrich.pdf"),(path+"/out/ko_dodge.png",path+"/out/ko.Ko.enrich.png"),(path+"/out/ko_dodge.tiff",path+"/out/ko.Ko.enrich.tiff"))
      else ((path+"/out/go_stack.png",path+"/out/go.Go.enrich.png"),(path+"/out/go_stack.pdf",path+"/out/go.Go.enrich.pdf"),(path+"/out/go_stack.png",path+"/out/go.Go.enrich.png"),(path+"/out/go_stack.tiff",path+"/out/go.Go.enrich.tiff"))
    Ok(Json.obj("pics"->pics,"downpng"->downpngs,"downpics"->downpics,"downtiffs"->downtiffs,"color"->color,"elements"->elements))
  }

  case class ReGoData(g:String,n:String,sm:String,br:String,cs:String,width:String,height:String,dpi:String,xts:String,yts:String,lts:String)

  val ReGoForm: Form[ReGoData] =Form(
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

  def redrawGO(taskname:String,types:String)=Action(parse.multipartFormData) { implicit request =>
    val data=ReGoForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname
    val (r,i,g,in)=
      if(types=="ko")
        ("R/gokegg/plot/Ko_dodge_plot.R",dutyDir + "/out/ko.Ko.bar.dat"," -g " + data.g,"ko_dodge")
      else ("R/gokegg/plot/Go_stack_plot.R",dutyDir + "/out/go.Go.bar.dat","","go_stack")

    val cs=
      if(data.cs=="") "" else " -cs " + data.cs.split(",").mkString(":") + ":#E41A1C:#FFC0CB:#1E90FF:#00BFFF:#FF8C00:#FFDEAD:#4DAF4A:#90EE90:#9692C3:#CDB4FF:#40E0D0:#00FFFF"

    val elements=
      Json.obj("n"->data.n,"br"->data.br,"g"->data.g,"sm"->data.sm,"cs"->"#E41A1C:#FFC0CB:#1E90FF:#00BFFF:#FF8C00:#FFDEAD:#4DAF4A:#90EE90:#9692C3:#CDB4FF:#40E0D0:#00FFFF","width"->data.width,"height"->data.height,"xts"->data.xts,"yts"->data.yts,"lts"->data.lts,"dpi"->data.dpi).toString()
    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    val command =
      "Rscript " + Utils.path + r + " -i "+ i + " -o " + dutyDir + "/out -n " + data.n + " -sm " + data.sm + " -br " + data.br +
        cs + " -is " + data.width + ":" + data.height + " -dpi " + data.dpi + " -xts sans:bold.italic:" + data.xts +
        " -yts sans:bold.italic:" + data.yts + " -lts sans:bold.italic:" + data.lts + " -in " + in + g + " -if pdf"

    println(command)
    val execCommand = new ExecCommand
    execCommand.exect(command,dutyDir+"/temp")

    if (execCommand.isSuccess) {
      creatZip(dutyDir) //替换压缩文件包
      Utils.pdf2Png(dutyDir+"/out/"+in+".pdf",dutyDir+"/out/"+in+".png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/"+in+".pdf",dutyDir+"/out/"+in+".tiff") //替换图片
      val pics=dutyDir+"/out/"+in+".png"
      Ok(Json.obj("valid"->"true","pics"->pics))
    } else {
      Ok(Json.obj("valid"->"false"))
    }
  }


  //getVennData
  def getJvennData = Action(parse.multipartFormData) { implicit request =>
    val file = request.body.file("browse_upload").get.ref.file
    val buffer = FileUtils.readLines(file).asScala

    val checkfile = buffer.map(_.split("[\t|;|,]").length).distinct
    Ok(Json.toJson(Array(if (buffer.length == 0) {
      Json.obj("error" -> "请上传一个带有分隔符的TXT文件！")
    } /*else if (checkfile.length != 1) {
    Json.obj("error" -> "文件字段行数不一样！")
  } */ else if (checkfile.head > 6) {
      Json.obj("error" -> "文件中列数过多！")
    } else {
      val matrix = buffer.map(_.split("[\t|;|,]"))
      val head = matrix.head
      val char = Array("A", "B", "C", "D", "E", "F")
      val body = (0 to matrix.head.length).map { x =>
        matrix.map { y =>
          if (y.length > x) y(x) else ""
        }.distinct.init
      }.toBuffer

      val result = body.flatMap { x =>
        x.map { y =>
          val key = body.filter(_.contains(y)).map(z => char(body.indexOf(z))).mkString
          (key, y)
        }.distinct
      }.distinct

      val data = result.groupBy(_._1).map(x => x._1 -> x._2.map(_._2))
      val name = head.zipWithIndex.map(x => char(x._2) -> x._1).toMap
      val values = result.groupBy(_._1).map(x => x._1 -> x._2.map(_._2).length)

      Json.obj("data" -> data, "name" -> name, "values" -> values)
    })))
  }



  //innergroup correlation
  case class InnerGroupData(taskname:String,anatype:String,vector:String,color:String,hasnum:String,
                            hasborder:String, hasrname:String,hascname:String)

  val InnerGroupForm: Form[InnerGroupData] =Form(
    mapping (
      "taskname"->text,
      "anatype"->text,
      "vector"->text,
      "color"->text,
      "hasnum"->text,
      "hasborder"->text,
      "hasrname"->text,
      "hascname"->text,
    )(InnerGroupData.apply)(InnerGroupData.unapply)
  )

  def doIGC=Action(parse.multipartFormData) { implicit request =>
    val data = InnerGroupForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val file1=request.body.file("table1").get
    val tableFile=new File(dutyDir,"table.txt")
    val input= file1.filename

    val param= "分析类型：" + data.anatype + "/计算向量：" + data.vector + "/颜色：" + data.color +
      "/在格子上显示数字：" + data.hasnum + "/画出格子的边界：" + data.hasborder + "/是否显示行名：" +
      data.hasrname + "/是否显示列名：" + data.hascname

//    val elements=Json.obj("cluster_rows"->"FALSE","cluster_cols"->"FALSE","rtree"->"50","ctree"->"50","rp"->"1","cp"->"1","color"->data.color,"cc"->"30","xfs"->"10","yfs"->"10","hasborder"->data.hasborder,"colorborder"->"#000000","hasnum"->data.hasnum,"hasrname"->data.hasrname,"hascname"->data.hascname).toString()

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"IGC","组内相关性分析",input,param,"")
    //矩阵文件读取写入任务文件下table.txt
    file1.ref.moveTo(tableFile)

    Future{
      val color= " -c "+ data.color

      val border=
        if(data.hasborder=="TRUE") " -cbc #000000 "
        else ""

      val command1 = "Rscript "+Utils.path+"R/igc/cormap_data.R -i " + tableFile.getAbsolutePath +
        " -o " + dutyDir + "/out" + " -m " + data.vector + " -am " + data.anatype

      val command2 = ""
//
//      println(command)
//
//      val execCommand = new ExecCommand
//      execCommand.exect(command,dutyDir+"/temp")
//
//      if (execCommand.isSuccess) {
//        val finish=dutyController.updateFini(id,data.taskname)
//        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"")
//        creatZip(dutyDir)
//        Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.png")
//        Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.tiff")
//      } else {
//        dutydao.updateFailed(id,data.taskname)
//        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
//      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readIGC(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val elements=jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).elements)

    val head=FileUtils.readFileToString(new File(path+"/table.txt")).trim.split("\n")
    val rnum=head.length-1
    val cnum=
      if(elements("col")=="") head(1).trim.split("\t").length-1
      else elements("col").split(",").length
    //获取图片
    val pics=getReDrawPics(path)
    val (downpics,downpng,downtiffs)=(path+"/out/heatmap.pdf",path+"/out/heatmap.png",path+"/out/heatmap.tiff")
    Ok(Json.obj("pics"->pics,"downpng"->downpng,"downpics"->downpics,"downtiffs"->downtiffs,"rnum"->rnum,"cnum"->cnum,"elements"->elements))
  }

  case class ReIGCData(col:String, scale:String, lg:String, cluster_rows:String, cluster_cols:String, rtree:String, ctree:String,
                        rp:String, cp:String, color:String, designcolor:String, cc:String, xfs:String, yfs:String,
                        hasborder:String, colorborder:String, hasnum:String, hasrname:String, hascname:String)

  val ReIGCForm: Form[ReHeatData] =Form(
    mapping (
      "col"->text,
      "scale"->text,
      "lg"->text,
      "cluster_rows"->text,
      "cluster_cols"->text,
      "rtree"->text,
      "ctree"->text,
      "rp"->text,
      "cp"->text,
      "color"->text,
      "designcolor"->text,
      "cc"->text,
      "xfs"->text,
      "yfs"->text,
      "hasborder"->text,
      "colorborder"->text,
      "hasnum"->text,
      "hasrname"->text,
      "hascname"->text
    )(ReHeatData.apply)(ReHeatData.unapply)
  )

  def redrawIGC(taskname:String)=Action(parse.multipartFormData) { implicit request =>
    val data=ReHeatForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname
    val tableFile=new File(dutyDir,"table.txt")

    val color= if(data.color=="0") data.designcolor else data.color
    val border= if(data.hasborder=="FALSE") "" else " -cbc " + data.colorborder
    val ics= if(data.col=="") "" else " -ics " + data.col


    val elements=Json.obj("col"->data.col,"scale"->data.scale,"lg"->data.lg,"cluster_rows"->data.cluster_rows,"cluster_cols"->data.cluster_cols,"rtree"->data.rtree,"ctree"->data.ctree,"rp"->data.rp,"cp"->data.cp,"color"->color,"cc"->data.cc,"xfs"->data.xfs,"yfs"->data.yfs,"hasborder"->data.hasborder,"colorborder"->data.colorborder,"hasnum"->data.hasnum,"hasrname"->data.hasrname,"hascname"->data.hascname).toString()
    println(elements)
    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    val command = "Rscript "+Utils.path+"R/heatmap/heatMap_plot.R -i "+ tableFile.getAbsolutePath +
      " -o " +dutyDir+"/out -s " + data.scale + " -lg " + data.lg + ics + " -cls " +data.cluster_rows+":"+
      data.cluster_cols + " -th " + data.rtree + ":" + data.ctree + " -rp " + data.rp + " -cp " + data.cp +
      " -c " + color + " -cc " + data.cc + " -fs " + data.xfs + ":" + data.yfs + border + " -sn " +
      data.hascname + ":" + data.hasrname + ":" + data.hasnum

    val execCommand = new ExecCommand
    execCommand.exect(command,dutyDir+"/temp")

    if (execCommand.isSuccess) {
      creatZip(dutyDir) //替换压缩文件包
      Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.tiff") //替换图片
      val pics=dutyDir+"/out/heatmap.png"
      Ok(Json.obj("valid"->"true","pics"->pics))
    } else {
      Ok(Json.obj("valid"->"false"))
    }
  }





  val userDutyDir: String =Utils.path+"users/"

  //创建用户任务文件夹和结果文件夹
  def creatUserDir(uid:String,taskname:String): String ={
    new File(userDutyDir+uid+"/"+taskname).mkdir()
    new File(userDutyDir+uid+"/"+taskname+"/out").mkdir()
    new File(userDutyDir+uid+"/"+taskname+"/temp").mkdir()

    userDutyDir+uid+"/"+taskname
  }


  def creatZip(path:String): Unit ={
    val target=path+"/outcome.zip"
    new File(target).createNewFile()
    CompressUtil.zip(path+"/out",target)
  }

  def getPics(id:String,taskname:String): Array[String] = {
    val files = new File(Utils.path+"/users/"+id+"/"+taskname+"/out").listFiles().filter(_.getName.contains("png")).map(_.getAbsolutePath)
    files
  }

  def getFiles(id:String,taskname:String): Array[String] = {
    val files = new File(Utils.path+"/users/"+id+"/"+taskname+"/out").listFiles().map(_.getAbsolutePath)
    files
  }


  val CCA: Array[String] =Array("rdacca.pdf", "percent.xls", "samples.xls", "species.xls", "envi.xls")
  val CCAins: Array[String] =Array("CCA/RDA结果图", "百分比表", "样本坐标表", "物种坐标表", "环境因子坐标表")

  val Heat: Array[String] =Array("heatmap.pdf")
  val Heatins: Array[String] =Array("热图")

  val PCA: Array[String] =Array("pca.pdf", "pca.sdev.xls", "pca.rotation.xls")
  val PCAins: Array[String] =Array("PCA结果图", "PCA值表格", "特征向量矩阵表格")

  var GO: Array[String] =Array("go.Go.enrich.pdf", "go_stack.pdf", "go.Go.enrich.xls")
  var GOins: Array[String] =Array("GO富集分析结果图", "GO富集分析柱状图", "GO富集分析结果")

  var KEGG: Array[String] =Array("ko.Ko.enrich.pdf", "ko_dodge.pdf", "ko.Ko.enrich.xls")
  var KEGGins: Array[String] =Array("KEGG富集分析结果图", "KEGG富集分析柱状图", "KEGG富集分析结果")

  var Box: Array[String] =Array("box.pdf")
  var Boxins: Array[String] =Array("盒型图")

  var Net: Array[String] =Array("result.xls")
  var Netins: Array[String] =Array("相关性系数及P值分析结果")


  def getDownloadFiles(taskname:String,soft:String): Action[AnyContent] =Action { implicit request=>
    val id=request.session.get("userId").get
    val (outfiles,filesins)=
      if(soft=="PCA") (PCA,PCAins)
      else if(soft=="CCA") (CCA,CCAins)
      else if(soft=="Heatmap") (Heat,Heatins)
      else if(soft=="Boxplot") (Box,Boxins)
      else if(soft=="NetWeight") (Net,Netins)
      else if(soft=="KEGG") (KEGG,KEGGins)
      else if(soft=="GO") (GO,GOins)
      else (Array(""),Array(""))

    val files=outfiles.map{x=>
      Utils.path + "/users/" + id + "/" + taskname + "/out/" + x
    }

    Ok(Json.obj("files"->files,"name"->outfiles,"ins"->filesins))
  }

  def getReDrawPics(path:String): Array[String] ={
    val pics = new File(path+"/out").listFiles().filter(_.getName.contains("png")).map(_.getAbsolutePath)
    pics
  }

  def download(path:String): Action[AnyContent] = Action{ implicit request=>
    val file=new File(path)
    Ok.sendFile(file).withHeaders(
      //缓存
      CACHE_CONTROL -> "max-age=3600",
      CONTENT_DISPOSITION -> ("attachment; filename=" + file.getName),
      CONTENT_TYPE -> "application/x-download"
    )

  }


  def downloadZip(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val path=new File(userDutyDir+request.session.get("userId").get+"/"+taskname+"/outcome.zip")
    Ok.sendFile(path).withHeaders(
      //缓存
      CACHE_CONTROL -> "max-age=3600",
      CONTENT_DISPOSITION -> ("attachment; filename=" + path.getName),
      CONTENT_TYPE -> "application/x-download"
    )
  }

  def downloadExamples(name:String): Action[AnyContent] =Action{ implicit request=>
    val file=new File(Utils.path+"files/examples/"+name)
    Ok.sendFile(file).withHeaders(
      CACHE_CONTROL->"max-age=3600",
      CONTENT_DISPOSITION -> ("attachment; filename=" + file.getName),
      CONTENT_TYPE -> "application/x-download"
    )
  }

  def getPic(path:String,num:Double): Action[AnyContent] = Action{ implicit request=>
    val file = new File(path)
    SendImg(file,request.headers)
  }

  def SendImg(file: File,headers:Headers): Result = {
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

  def jsonToMap(json:String): Map[String, String] = {
    scala.util.parsing.json.JSON.parseFull(json).get.asInstanceOf[Map[String, String]]
  }

  def mapToJson(map:Map[String,String]): String = {
    Json.toJson(map).toString()
  }

}
