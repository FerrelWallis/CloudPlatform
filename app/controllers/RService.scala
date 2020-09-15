package controllers

import java.io.{File, FileInputStream}
import java.nio.file.{Files, Path}

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
  case class PCA1Data(taskname:String,showname:String,showerro:String,txdata1:String,txdata2:String)

  val PCA1Form: Form[PCA1Data] =Form(
    mapping (
      "taskname"->text,
      "showname"->text,
      "showerro"->text,
      "txdata1"->text,
      "txdata2"->text
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
      if(isgroup && group=="2") file1.filename+"/"+ request.body.file("table2").get.filename
      else file1.filename
    } else{
      FileUtils.writeStringToFile(tableFile, data.txdata1)
      if(isgroup && group=="2") request.body.file("table2").get.filename
      else "无"
    }

    val (param,co)=
      if(isgroup) ("是否显示样本名：" + data.showname + "/是否分组绘图：" +
        isgroup,
        "#CD0000:#3A89CC:#769C30:#D99536:#7B0078:#BFBC3B:#E2609F:#00688B:#C10077:#CAAA76" +
        ":#EEEE00:#458B00:#8B4513:#008B8B:#6E8B3D:#8B7D6B:#7FFF00:#CDBA96:#ADFF2F")
      else ("是否显示样本名：" + data.showname + "/是否分组绘图：" + isgroup,"#48FF75")

    val elements= Json.obj("xdata"->"PC1","ydata"->"PC2","width"->"15","length"->"12","showname"->data.showname,
      "showerro"->data.showerro,"color"->co,"resolution"->"300","xts"->"15","yts"->"15","xls"->"17","yls"->"17",
      "lts"->"14","lms"->"15","lmtext"->"","ms"->"17","mstext"->"","c"->"FALSE","big"->"no",
      "xdamin"->"","xdamax"->"","ydamin"->"","ydamax"->"").toString()

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
      val groupdata=
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
        " -si " + dutyDir+"/out/pca.sdev.xls" + groupdata + " -o " +dutyDir+"/out" + name +
        " -if pdf -ss " + data.showerro
      println(command2)

      val execCommand2 = new ExecCommand
      execCommand2.exect(command2,dutyDir+"/temp")

      if (execCommand1.isSuccess && execCommand2.isSuccess) {
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),groupdata+name)
        Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.png")
        val finish=dutyController.updateFini(id,data.taskname)
        Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.tiff")
        creatZip(dutyDir)
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

    val elements=jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)
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

  case class RePCAData(xdata:String,ydata:String,showname:String,showerro:String,color:String,
                       width:String, length:String,resolution:String,xts:String,yts:String,
                       xls:String,yls:String,lts:String,lms:String,lmtext:String,ms:String,
                       mstext:String,c:String)

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
      "lms"->text,
      "lmtext"->text,
      "ms"->text,
      "mstext"->text,
      "c"->text
    )(RePCAData.apply)(RePCAData.unapply)
  )

  case class RePCAData2(big:String,xdamin:String,xdamax:String,ydamin:String,ydamax:String)
  val RePCAForm2: Form[RePCAData2] =Form(
    mapping (
      "big"->text,
      "xdamin"->text,
      "xdamax"->text,
      "ydamin"->text,
      "ydamax"->text
    )(RePCAData2.apply)(RePCAData2.unapply)
  )

  def redrawPCA(taskname:String)=Action(parse.multipartFormData) { implicit request =>
    val data=RePCAForm.bindFromRequest.get
    val data2=RePCAForm2.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname
    val groupFile=new File(dutyDir,"group.txt")

    val elements= Json.obj("xdata"->data.xdata,"ydata"->data.ydata,"width"->"15","length"->"12",
      "showname"->data.showname,"showerro"->data.showerro,"color"->data.color,"resolution"->data.resolution,
      "xts"->data.xts,"yts"->data.yts,"xls"->data.xls,"yls"->data.yls,"lts"->data.lts,"lms"->data.lms,
      "lmtext"->data.lmtext,"ms"->data.ms,"mstext"->data.mstext,"c"->data.c,"big"->data2.big,
      "xdamin"->data2.xdamin,"xdamax"->data2.xdamax,"ydamin"->data2.ydamin,
      "ydamax"->data2.ydamax).toString()

    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    //读取log.txt 包括-g -b
    val log=FileUtils.readFileToString(new File(dutyDir+"/log.txt"))
    val c=
      if(!groupFile.exists()) " -oc \""+data.color+"\""
      else " -cs \""+ data.color+"\""

    val name=if(data.showname.equals("TRUE") && groupFile.exists()){
      val f=FileUtils.readLines(groupFile).asScala
      val n=f.map{_.split('\t').last}.distinct.drop(1).mkString(",")
      " -b " + n
    }else if(data.showname.equals("TRUE") && !groupFile.exists()){
      " -sl TRUE"
    } else ""
    val big=if(data2.big=="no") ""
    else if(data2.big=="x")
      " -da x:"+data2.xdamin+","+data2.xdamax
    else " -da y:"+data2.ydamin+","+data2.ydamax

    println(data.ydata)

    val lms=if(!data.lmtext.equals("")) " -lms sans:bold.italic:" + data.lms + ":\"" + data.lmtext+"\"" else ""
    val ms=if(!data.mstext.equals("")) " -ms sans:plain:" + data.ms + ":\"" + data.mstext+"\"" else ""
    val command = "Rscript "+Utils.path+"R/pca/pca_plot.R -i "+ dutyDir+"/out/pca.x.xls" + log +
      " -si " + dutyDir+"/out/pca.sdev.xls" + " -o " +dutyDir+"/out" + " -pxy "+ data.xdata+":"+ data.ydata +
      " -is "+ data.width + ":" + data.length + c + " -dpi " + data.resolution + " -xts " + "sans:plain:"+data.xts +
      " -yts " + "sans:plain:"+data.yts + " -xls " + "sans:plain:"+data.xls + " -yls " + "sans:plain:"+data.yls +
      " -lts " + "sans:plain:" + data.lts + name + " -if pdf -ss " + data.showerro + lms + ms +
      " -c " + data.c + big

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")
//    execCommand.exect(command,dutyDir+"/temp")

    println(command)
    println(execCommand.getOutStr)
    println(execCommand.getErrStr)

    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.tiff") //替换图片
      creatZip(dutyDir) //替换压缩文件包
      val pics=dutyDir+"/out/pca.png"
      val pdfs=dutyDir+"/out/pca.pdf"
      val tiffs=dutyDir+"/out/pca.tiff"
      Ok(Json.obj("valid"->"true","pics"->pics,"downpics"->pdfs,"downtiffs"->tiffs))
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
      if(data.ymin!=""&&data.ymax!="") " -ymm " + data.ymin+":"+data.ymax
      else ""

    val elements= Json.obj("spot"->data.spot,"ymin"->data.ymin,"ymax"->data.ymax,"lp"->"right:top",
      "boxwidth"->"0.7","alp"->"0.8","add"->"1","color"->"#E41A1C:#1E90FF:#FF8C00:#4DAF4A:#984EA3:#40E0D0:#F4AAC4:#DC414B:#957624:#43B43C",
      "width"->"12","length"->"10","dpi"->"300","xts"->"15","xls"->"12","xtext"->"","yts"->"15",
      "yls"->"12","ytext"->"","lts"->"14","lls"->"15","lltext"->"","ms"->"12","mstext"->"").toString()

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"Boxplot","Boxplot 盒型图",input,param,elements)
    //矩阵文件读取写入任务文件下table.txt
    file1.ref.moveTo(tableFile)

    Future{
      val command = "Rscript "+Utils.path+"R/box/boxplot.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out" + " -sp " + data.spot + ylim + " -ls 12:10"
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      println(command)

      if (execCommand.isSuccess) {
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"")
        Utils.pdf2Png(dutyDir+"/out/box.pdf",dutyDir+"/out/box.png")
        val finish=dutyController.updateFini(id,data.taskname)
        Utils.pdf2Png(dutyDir+"/out/box.pdf",dutyDir+"/out/box.tiff")
        creatZip(dutyDir)

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

    val elements=jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)
    val color=elements("color").split(":")

    val head=FileUtils.readFileToString(new File(path+"/table.txt")).trim.split("\n")
    val group=head(0).trim.split("\t")

    //获取图片
    val pics=getReDrawPics(path)
    val (downpics,downpng,downtiffs)=(path+"/out/box.pdf",path+"/out/box.png",path+"/out/box.tiff")
    Ok(Json.obj("group"->group,"pics"->pics,"downpng"->downpng,"downpics"->downpics,"downtiffs"->downtiffs,"elements"->elements,"color"->color))
  }

  case class ReBoxData(lp:String,spot:String,ymin:String,ymax:String,color:String,width:String,
                       length:String,dpi:String,boxwidth:String,alp:String,add:String,xts:String,
                       xls:String,xtext:String,yts:String,yls:String,ytext:String,lts:String,lls:String,
                       lltext:String,ms:String,mstext:String)

  val ReBoxForm: Form[ReBoxData] =Form(
    mapping (
      "lp"->text,
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
      "xls"->text,
      "xtext"->text,
      "yts"->text,
      "yls"->text,
      "ytext"->text,
      "lts"->text,
      "lls"->text,
      "lltext"->text,
  "ms"->text,
  "mstext"->text
    )(ReBoxData.apply)(ReBoxData.unapply)
  )

  def redrawBox(taskname:String)=Action(parse.multipartFormData) { implicit request =>
    val data=ReBoxForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname

    val elements= Json.obj("lp"->data.lp,"spot"->data.spot,"ymin"->data.ymin,"ymax"->data.ymax,
      "boxwidth"->data.boxwidth,"alp"->data.alp,"add"->data.add,"color"->data.color, "width"->data.width,
      "length"->data.length,"dpi"->data.dpi,"xts"->data.xts,"xls"->data.xls,"xtext"->data.xtext,
      "yts"->data.yts,"yls"->data.yls,"ytext"->data.ytext,"lts"->data.lts,"lls"->data.lls,
      "lltext"->data.lltext,"ms"->data.ms,"mstext"->data.mstext).toString()

    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    val ymm=if(data.ymin==""||data.ymax=="") "" else " -ymm " + data.ymin + ":" + data.ymax

    val yls=if(!data.ytext.equals("")) " -yls sans:bold.italic:" + data.yls + ":\"" + data.ytext + "\"" else ""
    val xls=if(!data.xtext.equals("")) " -xls sans:bold.italic:" + data.xls + ":\"" + data.xtext + "\"" else ""
    val lls=if(!data.lltext.equals("")) " -lls sans:bold.italic:" + data.lls + ":\"" + data.lltext + "\"" else ""
    val ms=if(!data.mstext.equals("")) " -ms sans:bold.italic:" + data.ms + ":\"" + data.mstext + "\"" else ""

    val command = "Rscript "+Utils.path+"R/box/boxplot.R -i "+ dutyDir+"/table.txt" + " -o " +dutyDir+"/out" +
      " -sp "+ data.spot + ymm + " -ls " + data.width + ":" + data.length + " -dpi " + data.dpi + " -bw " +
      data.boxwidth + " -alp " + data.alp + " -add " + data.add + " -xts " + "sans:bold.italic:"+ data.xts +
      " -yts " + "sans:bold.italic:" + data.yts + " -lts " + "sans:bold.italic:" + data.lts + " -cs \"" + data.color +
      "\" -lp " + data.lp + xls + yls + lls + ms

    println(command)

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

//    val execCommand = new ExecCommand
//    execCommand.exect(command,dutyDir+"/temp")
    println(execCommand.getErrStr)
    println(execCommand.getOutStr)

    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/box.pdf",dutyDir+"/out/box.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/box.pdf",dutyDir+"/out/box.tiff") //替换图片
      creatZip(dutyDir) //替换压缩文件包
      val pics=dutyDir+"/out/box.png"
      val pdfs=dutyDir+"/out/box.pdf"
      val tiffs=dutyDir+"/out/box.tiff"
      Ok(Json.obj("valid"->"true","pics"->pics,"downpics"->pdfs,"downtiffs"->tiffs))
    } else {
      Ok(Json.obj("valid"->"false"))
    }

  }

  //Heatmap
  case class HeatmapData(taskname:String,cluster_rows:String,crm:String,cluster_cols:String,
                         ccm:String,inr:String,inc:String, lg:String,sc:String,
                         color:String,hasnum:String,hasrname:String, hascname:String,hasborder:String)

  val HeatmapForm: Form[HeatmapData] =Form(
    mapping (
      "taskname"->text,
      "cluster_rows"->text,
      "crm"->text,
      "cluster_cols"->text,
      "ccm"->text,
      "inr"->text,
      "inc"->text,
      "lg"->text,
      "sc"->text,
      "color"->text,
      "hasnum"->text,
      "hasrname"->text,
      "hascname"->text,
      "hasborder"->text
    )(HeatmapData.apply)(HeatmapData.unapply)
  )

  def doHeatmap(isgroupr:Boolean,isgroupc:Boolean,istag:Boolean)=Action(parse.multipartFormData) { implicit request =>
    val data = HeatmapForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)
    val tableFile=new File(dutyDir,"table.txt")
    val treerFile=new File(dutyDir,"treer.txt")
    val treecFile=new File(dutyDir,"treec.txt")
    val grouprFile=new File(dutyDir,"groupr.txt")
    val groupcFile=new File(dutyDir,"groupc.txt")
    val tagFile=new File(dutyDir,"tag.txt")

    val filename2=
      if(data.cluster_rows=="file") {
        val file=request.body.file("table2").get
        file.ref.moveTo(treerFile)
        "/"+file.filename
      }else ""

    val filename3=
      if(data.cluster_cols=="file") {
        val file=request.body.file("table3").get
        file.ref.moveTo(treecFile)
        "/"+file.filename
      }else ""

    val filename4=
      if(isgroupr==true) {
        val file=request.body.file("table4").get
        file.ref.moveTo(grouprFile)
        "/"+file.filename
      }else ""

    val filename5=
      if(isgroupc==true) {
        val file=request.body.file("table5").get
        file.ref.moveTo(groupcFile)
        "/"+file.filename
      }else ""

    val filename6=
      if(istag==true) {
        val file=request.body.file("table6").get
        file.ref.moveTo(tagFile)
        "/"+file.filename
      }else ""

    //在用户下创建任务文件夹和结果文件夹
    val file1=request.body.file("table1").get
    //矩阵文件读取写入任务文件下table.txt
    file1.ref.moveTo(tableFile)
    val input= file1.filename+filename2+filename3+filename4+filename5+filename6
    val c=
      if(data.inr=="" && data.inc=="") ""
      else if(data.inr!="" && data.inc=="") "/作图的行："+data.inr
      else if(data.inr=="" && data.inc!="") "/作图的列："+data.inc
      else "/作图的行："+data.inr+"/作图的列："+data.inc
    val rm=if(data.cluster_rows=="TRUE") "/行聚类方法："+data.crm else ""
    val cm=if(data.cluster_cols=="TRUE") "/列聚类方法："+data.ccm else ""

    val param= "是否对行聚类：" + data.cluster_rows + rm + "/是否对列聚类：" +
      data.cluster_cols + cm + "/是否行分组：" + isgroupr + "/是否列分组：" +
      isgroupc + "/是否自定义格子标签：" + istag + c + "/是否取lg：" + data.lg +
      "/归一化：" + data.sc + "/颜色：" + data.color + "/在格子上显示数字：" +
      data.hasnum + "/是否显示行名：" + data.hasrname + "/是否显示列名：" + data.hascname +
      "/画出格子的边界：" + data.hasborder

    val elements=Json.obj("smt"->"full","cluster_rows"->data.cluster_rows,"crm"->data.crm,
      "rp"->"1","cluster_cols"->data.cluster_cols,"ccm"->data.ccm,"cp"->"1","inr"->data.inr,
      "inc"->data.inc,"sc"->data.sc,"lg"->data.lg,"color"->data.color,"cc"->"30","nc"->"#DDDDDD",
      "hasborder"->data.hasborder,"cbc"->"#ffffff","hasnum"->data.hasnum,"hasrname"->data.hasrname,
      "hascname"->data.hascname,"rtree"->"50","ctree"->"50","xfs"->"10","yfs"->"10","xfa"->"90","fn"->"8").toString()

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"Heatmap","Heatmap 热图",input,param,elements)


    Future{
      val trf=if(treerFile.exists()) " -trf "+treerFile.getAbsolutePath else ""
      val tcf=if(treecFile.exists()) " -tcf "+treecFile.getAbsolutePath else ""
      val ari=if(grouprFile.exists()) " -ari "+ grouprFile.getAbsolutePath else ""
      val aci=if(groupcFile.exists()) " -aci "+ groupcFile.getAbsolutePath else ""
      val lfi=if(tagFile.exists()) " -lfi "+ tagFile.getAbsolutePath else ""

      val rowclu=
        if(data.cluster_rows!="file") " -crw " + data.cluster_rows+" -crm " + data.crm
        else ""
      val colclu=
        if(data.cluster_cols!="file") " -ccl " + data.cluster_cols+" -ccm " + data.ccm
        else ""

      val inr=if(data.inr!="") " -inr "+data.inr else ""
      val inc=if(data.inc!="") " -inc "+data.inc else ""

      val command = "Rscript "+Utils.path+"R/heatmap/heatMap.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out" + trf + tcf + ari + aci + lfi + rowclu + colclu + inr + inc +
        " -lg " + data.lg + " -sc " + data.sc + " -sn " +  data.hasrname + ":" + data.hascname +
        ":" + data.hasnum + " -c " + data.color + " -cbc " + data.hasborder + " -if pdf -cln TRUE -fn 8"

      println(command)

      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (execCommand.isSuccess) {
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"")
        Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.png")
        val finish=dutyController.updateFini(id,data.taskname)
        Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.tiff")
        creatZip(dutyDir)
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

    //如果行列有分组文件，就不能进行用于作图的分组选择
    val groupr=new File(path,"groupr.txt").exists()
    val groupc=new File(path,"groupc.txt").exists()

    val elements=jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)

    val head=FileUtils.readFileToString(new File(path+"/table.txt")).trim.split("\n")
    val rnum=
      if(elements("inr")=="") head.length-1
      else {
        val r1=elements("inr").split(",").length-elements("inr").split(",").filter(_.contains("-")).length
        val r2=elements("inr").split(",").filter(_.contains("-")).map{x=>
          val temp=x.split("-")
          temp(1).toInt-temp(0).toInt+1
        }.sum
        if((r1+r2)>(head.length-1)) head.length-1 else r1+r2
      }
    val cnum=
      if(elements("inc")=="") head(1).trim.split("\t").length-1
      else {
        val c1=elements("inc").split(",").length-elements("inc").split(",").filter(_.contains("-")).length
        val c2=elements("inc").split(",").filter(_.contains("-")).map{x=>
          val temp=x.split("-")
          temp(1).toInt-temp(0).toInt+1
        }.sum
        if((c1+c2)>head(1).trim.split("\t").length-1) head(1).trim.split("\t").length-1 else c1+c2
      }

    //获取图片
    val pics=getReDrawPics(path)
    val (downpics,downpng,downtiffs)=(path+"/out/heatmap.pdf",path+"/out/heatmap.png",path+"/out/heatmap.tiff")
//    Ok(Json.obj("pics"->pics,"downpng"->downpng,"downpics"->downpics,"downtiffs"->downtiffs,"rnum"->rnum,"allcol"->(head(1).trim.split("\t").length-1)))
    Ok(Json.obj("pics"->pics,"downpng"->downpng,"downpics"->downpics,"downtiffs"->downtiffs,"rnum"->rnum,"cnum"->cnum,"elements"->elements,"allcol"->(head(1).trim.split("\t").length-1),"allrow"->(head.length-1),"groupr"->groupr,"groupc"->groupc))
  }

  case class ReHeatData(smt:String,cluster_rows:String,crm:String,rp:String,cluster_cols:String,ccm:String,
                        cp:String,inr:String,inc:String,designcolor:String,lg:String,color:String,cc:String,
                        nc:String,hasborder:String,cbc:String,hasrname:String, hascname:String,hasnum:String)

  val ReHeatForm: Form[ReHeatData] =Form(
    mapping (
      "smt"->text,
      "cluster_rows"->text,
      "crm"->text,
      "rp"->text,
      "cluster_cols"->text,
      "ccm"->text,
      "cp"->text,
      "inr"->text,
      "inc"->text,
      "designcolor"->text,
      "lg"->text,
      "color"->text,
      "cc"->text,
      "nc"->text,
      "hasborder"->text,
      "cbc"->text,
      "hasrname"->text,
      "hascname"->text,
      "hasnum"->text
    )(ReHeatData.apply)(ReHeatData.unapply)
  )

  def redrawHeatmap(taskname:String,rtree:String,ctree:String,xfs:String,yfs:String,sc:String,xfa:String,fn:String)=Action(parse.multipartFormData) { implicit request =>
    val data=ReHeatForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname
    val tableFile=new File(dutyDir,"table.txt")
    val treerFile=new File(dutyDir,"treer.txt")
    val treecFile=new File(dutyDir,"treec.txt")
    val grouprFile=new File(dutyDir,"groupr.txt")
    val groupcFile=new File(dutyDir,"groupc.txt")
    val tagFile=new File(dutyDir,"tag.txt")

    if(data.cluster_rows=="file" && !treerFile.exists()){
      Ok(Json.obj("valid"->"false","message"->"没有上传行聚类信息文件，无法使用！"))
    } else if(data.cluster_cols=="file" && !treecFile.exists()){
      Ok(Json.obj("valid"->"false","message"->"没有上传列聚类信息文件，无法使用！"))
    }else{
      val ari=if(grouprFile.exists()) " -ari "+ grouprFile.getAbsolutePath else ""
      val aci=if(groupcFile.exists()) " -aci "+ groupcFile.getAbsolutePath else ""
      val lfi=if(tagFile.exists()) " -lfi "+ tagFile.getAbsolutePath else ""

      val rowclu=
        if(data.cluster_rows=="TRUE") " -crw " + data.cluster_rows + " -crm " + data.crm + " -rp " + data.rp
        else if(data.cluster_rows=="FALSE") " -crw " + data.cluster_rows
        else " -trf " + treerFile.getAbsolutePath + " -rp " + data.rp
      val colclu=
        if(data.cluster_cols=="TRUE") " -ccl " + data.cluster_cols + " -ccm " + data.ccm + " -cp " + data.cp
        else if(data.cluster_cols=="FALSE") " -ccl " + data.cluster_cols
        else " -tcf " + treecFile.getAbsolutePath + " -cp " + data.cp

      val color= if(data.color=="0") data.designcolor else data.color
      val cbc= if(data.hasborder=="white") " -cbc " + data.cbc else " -cbc " + data.hasborder
      val inc= if(data.inc=="") "" else " -inc " + data.inc
      val inr= if(data.inr=="") "" else " -inr " + data.inr

      val elements=Json.obj("smt"->data.smt,"cluster_rows"->data.cluster_rows,"crm"->data.crm,
        "rp"->data.rp,"cluster_cols"->data.cluster_cols,"ccm"->data.ccm,"cp"->data.cp,"inr"->data.inr,
        "inc"->data.inc,"sc"->sc,"lg"->data.lg,"color"->color,"cc"->data.cc,"nc"->data.nc,
        "hasborder"->data.hasborder,"cbc"->data.cbc,"hasnum"->data.hasnum, "hasrname"->data.hasrname,
        "hascname"->data.hascname,"rtree"->rtree,"ctree"->ctree,"xfs"->xfs,"yfs"->yfs,"xfa"->xfa,"fn"->fn).toString()

      Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

      val head=FileUtils.readFileToString(tableFile).trim.split("\n")
      val rnum =
        if(data.inr=="") (head.length-1).toString
        else {
          val r1=data.inr.split(",").length-data.inr.split(",").filter(_.contains("-")).length
          val r2=data.inr.split(",").filter(_.contains("-")).map{x=>
            val temp=x.split("-")
            temp(1).toInt-temp(0).toInt+1
          }.sum
          if((r1+r2)>(head.length-1)) (head.length-1).toString else (r1+r2).toString
        }
      val cnum =
        if(data.inc=="") (head(1).trim.split("\t").length-1).toString
        else {
          val c1=data.inc.split(",").length-data.inc.split(",").filter(_.contains("-")).length
          val c2=data.inc.split(",").filter(_.contains("-")).map{x=>
            val temp=x.split("-")
            temp(1).toInt-temp(0).toInt+1
          }.sum
          if((c1+c2)>head(1).trim.split("\t").length-1) (head(1).trim.split("\t").length-1).toString else (c1+c2).toString
        }


      val command = "Rscript "+Utils.path+"R/heatmap/heatMap.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out"+ " -smt " + data.smt + ari + aci + lfi + rowclu + colclu + inr + inc +
        " -sc " + sc + " -lg " + data.lg + " -c " + color + " -cc " + data.cc + " -nc " + data.nc +
        cbc + " -sn " + data.hasrname + ":" + data.hascname + ":" + data.hasnum + " -th " +
        rtree + ":" + ctree + " -fs " + yfs + ":" + xfs + " -if pdf -cln TRUE -xfa " + xfa + " -fn " + fn

      println(command)

      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (execCommand.isSuccess) {
        Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.png") //替换图片
        Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.tiff") //替换图片
        creatZip(dutyDir) //替换压缩文件包
        val pics=dutyDir+"/out/heatmap.png"
        val pdfs=dutyDir+"/out/heatmap.pdf"
        val tiffs=dutyDir+"/out/heatmap.tiff"
        Ok(Json.obj("valid"->"true","pics"->pics,"downpics"->pdfs,"downtiffs"->tiffs,"cnum"->cnum,"rnum"->rnum))
      } else {
        Ok(Json.obj("valid"->"false","message"->execCommand.getErrStr))
      }
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
      Json.obj("xdata"->xdata,"ydata"->ydata,"xaxis"->"0","yaxis"->"0","samsize"->"6",
        "color"->co,"showsname"->"true","samfont"->"7","showevi"->"true","color1"->"#E41A1C",
        "evifont"->"7","color2"->"#E41A1C","showspeci"->"true","specifont"->"7","specisize"->"6",
        "color3"->"#FF8C00","width"->"15","height"->"15","dpi"->"300","xts"->"16","yts"->"16",
        "xls"->"18","yls"->"18","lts"->"15","lms"->"19","ms"->"12","mstext"->"").toString()

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
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"")
        Utils.pdf2Png(dutyDir+"/out/rdacca.pdf",dutyDir+"/out/rdacca.png")
        val finish=dutyController.updateFini(id,data.taskname)
        Utils.pdf2Png(dutyDir+"/out/rdacca.pdf",dutyDir+"/out/rdacca.tiff")
        creatZip(dutyDir)
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

    val elements=jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)

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

  case class ReCCAMoreData(lts:String, lms:String,ms:String,mstext:String)

  val ReCCAMoreForm: Form[ReCCAMoreData] =Form(
    mapping (
      "lts"->text,
      "lms"->text,
      "ms"->text,
      "mstext"->text
    )(ReCCAMoreData.apply)(ReCCAMoreData.unapply)
  )

  def redrawCCA(taskname:String,showsname:Boolean,showevi:Boolean,showspeci:Boolean)=Action(parse.multipartFormData) { implicit request =>
    val data=ReCCAForm.bindFromRequest.get
    val data2=ReCCAMoreForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname
    val groupFile=new File(dutyDir,"group.txt")

    val elements=
      Json.obj("xdata"->data.xdata,"ydata"->data.ydata,"xaxis"->data.xaxis,
        "yaxis"->data.yaxis,"samsize"->data.samsize,"color"->data.color,
        "showsname"->showsname.toString,"samfont"->data.samfont,"showevi"->showevi.toString,
        "color1"->data.color1,"evifont"->data.evifont,"color2"->data.color2,
        "showspeci"->showspeci.toString,"specifont"->data.specifont,"specisize"->data.specisize,
        "color3"->data.color3,"width"->data.width,"height"->data.height,"dpi"->data.dpi,
        "xts"->data.xts,"yts"->data.yts,"xls"->data.xls,"yls"->data.yls,"lts"->data2.lts,
        "lms"->data2.lms,"ms"->data2.ms,"mstext"->data2.mstext).toString()

    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    val xyr=data.xdata.substring(3) + ":" + data.ydata.substring(3)
    val sname=
      if(showsname) " -sspt \"TRUE:TRUE\"" else " -sspt \"TRUE:FALSE\""
    val sat=
      if(!groupFile.exists&&data.color!="")
        " -sat \"" + data.color + ":" + data.samfont + "\""
      else " -sat \"#1E90FF:" + data.samfont + "\""
    val gc=if(groupFile.exists) " -gc \"" + data.color + "\"" else ""
    val group=
      if(groupFile.exists) " -g " + groupFile.getAbsolutePath else ""
    val evi=
      if(showevi) " -sepl \"TRUE:TRUE\"" else " -sepl \"FALSE:FALSE\""
    val speci=
      if(showspeci) " -sppt \"TRUE:TRUE\"" else " -sppt \"FALSE:FALSE\""

    val ms=if(!data2.mstext.equals("")) " -ms \"sans:bold.italic:" + data2.ms + ":" + data2.mstext + "\"" else ""

    val command =
      "Rscript "+Utils.path+"R/cca/rda_cca_plot.R -sai "+ dutyDir + "/out/samples.xls -spi " + dutyDir +
        "/out/species.xls -ei " + dutyDir + "/out/envi.xls -pci " + dutyDir + "/out/percent.xls -o " +
        dutyDir + "/out" + group + gc + " -is \"" + data.width + ":" + data.height + "\" -xyr \"" + xyr + "\" -op \"" +
        data.xaxis + ":" + data.yaxis + "\"" + sname + " -sap \"#000000:" + data.samsize + "\"" + sat +
        evi + " -ett \"" + data.color1 + ":" + data.evifont + "\" -elc \"" + data.color2 + "\"" +
        speci + " -spp \"" + data.color3 + ":" + data.specisize + "\" -spt \"" + data.color3 + ":" +
        data.specifont + "\" -dpi \"" + data.dpi + "\" -xts  \"sans:bold.italic:" + data.xts +
        "\" -yts  \"sans:bold.italic:" + data.yts + "\" -xls  \"sans:bold.italic:" + data.xls +
        "\" -yls  \"sans:bold.italic:" + data.yls + "\" -lts \"sans:bold.italic:" + data2.lts +
        "\" -lms \"sans:bold.italic:" + data2.lms + "\"" + ms

    println(command)

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

//    val execCommand = new ExecCommand
//    execCommand.exect(command,dutyDir+"/temp")

    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/rdacca.pdf",dutyDir+"/out/rdacca.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/rdacca.pdf",dutyDir+"/out/rdacca.tiff") //替换图片
      creatZip(dutyDir) //替换压缩文件包
      val pics=dutyDir+"/out/rdacca.png"
      val pdfs=dutyDir+"/out/rdacca.pdf"
      val tiffs=dutyDir+"/out/rdacca.tiff"
      Ok(Json.obj("valid"->"true","pics"->pics,"downpics"->pdfs,"downtiffs"->tiffs))
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

    val elements=Json.obj("gshape"->"ellipse","color1"->"#555555","gopa"->"1","gsize"->"5",
      "gfont"->"20","color2"->"#ffffff","eshape"->"diamond","color3"->"#5da5fb","eopa"->"1","esize"->"10",
      "efont"->"20","color4"->"#ffffff","color5"->"#737373","opacity"->"0.6","dot"->"3","pthres"->"0.1",
      "cthres"->"1").toString()

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

    val elements=jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)

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

    var resultFilter = Array("")
    result.drop(1).foreach{x=>
      val ei = x.split("\"").filter(_.trim!="")
      val w=ei(3).trim.split("\t").last.toDouble
      val c=ei(3).trim.split("\t").head.toDouble
      if(w<elements("pthres").toDouble && Math.abs(c)<elements("cthres").toDouble) {
        resultFilter=resultFilter:+x
      }
    }

    val edges=resultFilter.drop(1).map{x=>
      eid=eid+1
      val id="e"+eid
      val e = x.split("\"").filter(_.trim!="")
      val source=list.indexOf(e(1))
      val target=list.indexOf(e(2))
      val weight=e(3).trim.split("\t").last.toDouble
      val cc=e(3).trim.split("\t").head.toDouble
      val lab="c="+cc.formatted("%."+elements("dot")+"f")+"；p="+weight.formatted("%."+elements("dot")+"f")
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

  case class ReNetData(dot:String,pthres:String,cthres:String,gshape:String, color1:String,
                       gopa:String,gsize:String,gfont:String,color2:String,eshape:String,
                       color3:String,eopa:String,esize:String,efont:String,color4:String,
                       color5:String, opacity:String)

  val ReNetForm: Form[ReNetData] =Form(
    mapping (
      "dot"->text,
      "pthres"->text,
      "cthres"->text,
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

    val elements=Json.obj("gshape"->data.gshape,"color1"->data.color1,"gopa"->data.gopa,"gsize"->data.gsize,"gfont"->data.gfont,"color2"->data.color2,"eshape"->data.eshape,"color3"->data.color3,"eopa"->data.eopa,"esize"->data.esize,"efont"->data.efont,"color4"->data.color4,"color5"->data.color5,"opacity"->data.opacity,"dot"->data.dot,"pthres"->data.pthres,"cthres"->data.cthres).toString()
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
          FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"")
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
          creatZip(dutyDir)
          val finish=dutyController.updateFini(id,data.taskname)
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

    val elements=jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)
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
      if(data.cs=="") "" else " -cs " + data.cs.split(",").mkString(":#FFFFFF:") + ":#E41A1C:#FFC0CB:#1E90FF:#00BFFF:#FF8C00:#FFDEAD:#4DAF4A:#90EE90:#9692C3:#CDB4FF:#40E0D0:#00FFFF"

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
      Utils.pdf2Png(dutyDir+"/out/"+in+".pdf",dutyDir+"/out/"+in+".png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/"+in+".pdf",dutyDir+"/out/"+in+".tiff") //替换图片
      creatZip(dutyDir) //替换压缩文件包
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
    Ok(Json.toJson(Array(if (checkfile.head > 6) {
      Json.obj("error" -> "文件中列数过多！")
    } else {
      val matrix = buffer.map(_.split("[\t|;|,]"))
      val head = matrix.head //head=sample
      val char = Array("A", "B", "C", "D", "E", "F")
      val body = (0 to matrix.head.length).map { x =>
        matrix.map { y =>
          if (y.length > x) y(x) else ""
        }.distinct.init
      }.toBuffer
      //body=array(每一列数据为一行array,,,)
//      println(body.toList)

      val result = body.flatMap { x =>
        x.map { y =>
          val key = body.filter(_.contains(y)).map(z => char(body.indexOf(z))).mkString
//          println((key,y))
          (key, y)
        }.distinct
      }.distinct

      //result=>array((headers,gene),(),(),())  headers 是该元素的sample对应ABCD
//      println(result.toList)

      val data = result.groupBy(_._1).map(x => x._1 -> x._2.map(_._2))
      val name = head.zipWithIndex.map(x => char(x._2) -> x._1).toMap
      val values = result.groupBy(_._1).map(x => x._1 -> x._2.map(_._2).length)

      Json.obj("data" -> data, "name" -> name, "values" -> values)
    })))
  }

  //getVennData2
  def getJvennData2 = Action(parse.multipartFormData) { implicit request =>
    val file = request.body.file("browse_upload2").get.ref.file
    val buffer = FileUtils.readLines(file).asScala.filter(_!="")
    val file2 = request.body.file("browse_upload3").get.ref.file
    val head = FileUtils.readLines(file2).asScala.filter(_!="")

    val sample=buffer(0).split("\t")
    val char = Array("A", "B", "C", "D", "E", "F")

    val result=buffer.map{x=>
      val body=x.trim.split("\t")
      val gene=x.trim.split("\t")(0).trim
      val key=head.map{y=>
        val test = y.trim.split(",").map{z=>
          val index=sample.indexOf(z.trim)
          if(body(index)!="0") z.trim else ""
        }
        if(!test.contains("")) char(head.indexOf(y)) else ""
      }.mkString
      (key,gene)
    }.filter(r=>r._1!="")

    val data = result.groupBy(_._1).map(x => x._1 -> x._2.map(_._2))
    val name = head.zipWithIndex.map(x => char(x._2) -> x._1).toMap
    val values = result.groupBy(_._1).map(x => x._1 -> x._2.map(_._2).length)
    Ok(Json.obj("data" -> data, "name" -> name, "values" -> values))
  }



  //innergroup correlation
  case class InnerGroupData(taskname:String,anatype:String)

  val InnerGroupForm: Form[InnerGroupData] =Form(
    mapping (
      "taskname"->text,
      "anatype"->text
    )(InnerGroupData.apply)(InnerGroupData.unapply)
  )

  def doIGC(tablenum:Int)=Action(parse.multipartFormData) { implicit request =>
    val data = InnerGroupForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val tableFile1=new File(dutyDir,"table1.txt")
    val tableFile2=new File(dutyDir,"table2.txt")

    val file1 = request.body.file("table1").get
    val file2 = request.body.file("table2")
    val (input,tablepath) =
      if(file2.isEmpty) {
        file1.ref.moveTo(tableFile1)
        (file1.filename," -i1 "+tableFile1.getAbsolutePath)
      } else {
        file1.ref.moveTo(tableFile1)
        file2.get.ref.moveTo(tableFile2)
        (file1.filename+"/" + file2.get.filename," -i1 "+tableFile1.getAbsolutePath+" -i2 "+tableFile2.getAbsolutePath)
      }

    val param= "分析类型：" + data.anatype

    val elements=Json.obj("smt"->"full","cluster_rows"->"FALSE","crm"->"complete",
      "rp"->"1","cluster_cols"->"FALSE","ccm"->"complete","cp"->"1","sc"->"col","lg"->"none",
      "color"->"#E41A1C:#FFFF00:#1E90FF","cc"->"30","nc"->"#DDDDDD","hasborder"->"white",
      "cbc"->"#ffffff","hasnum"->"FALSE","hasrname"->"TRUE","hascname"->"TRUE",
      "rtree"->"50","ctree"->"50","xfs"->"10","yfs"->"10","xfa"->"90","fn"->"15","lfi"->"TRUE").toString() + "/" +
      Json.obj("gshape"->"ellipse","netcolor1"->"#555555","gopa"->"1","gsize"->"5",
        "gfont"->"20","netcolor2"->"#ffffff","eshape"->"diamond","netcolor3"->"#5da5fb","eopa"->"1",
        "esize"->"10","efont"->"20","netcolor4"->"#ffffff","netcolor5"->"#737373","opacity"->"0.6",
        "dot"->"3","pthres"->"0.1","cthres"->"0.5").toString()


    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"IGC","组内相关性分析",input,param,elements)
    //矩阵文件读取写入任务文件下table.txt

    Future{

      val command1 = "Rscript "+Utils.path+"R/igc/cor_pvalue_calculate.R" + tablepath +
        " -o " + dutyDir + "/out" + " -m " + data.anatype

      val command2 = "Rscript "+Utils.path+"R/heatmap/heatMap.R -i "+ dutyDir+"/out/cor.xls" +
        " -o " +dutyDir+"/out -c #E41A1C:#FFFF00:#1E90FF -lfi " + dutyDir+"/out/p_star.xls" + " -if pdf"

      println(command2)

      val commandpack1=Array(command1,command2)

      val execCommand = new ExecCommand
      execCommand.exec(commandpack1)

      if (execCommand.isSuccess) {
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"")
        Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.png")
        val finish=dutyController.updateFini(id,data.taskname)
        Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.tiff")
        creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readIGC(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val splitele=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements.split("/")
    val elements1=jsonToMap(splitele(0))
    val elements2=jsonToMap(splitele(1))
    val head=FileUtils.readFileToString(new File(path+"/out/cor.xls")).trim.split("\n")
    val rnum=head.length-1
    val cnum=head(1).trim.split("\t").length-1

    //获取图片
    val pics=getReDrawPics(path)
    val (downpics,downpng,downtiffs)=(path+"/out/heatmap.pdf",path+"/out/heatmap.png",path+"/out/heatmap.tiff")

    val tableFile1=new File(path,"table1.txt")
    val tableFile2=new File(path,"table2.txt")
    val g=FileUtils.readLines(tableFile1).asScala.head.trim.split("\t")
    val e=
      if(tableFile2.exists()) {
        FileUtils.readLines(tableFile2).asScala.head.trim.split("\t")
      }else g

    val list=e.drop(1)++g.drop(1)
    var count=0;
    val nodes=list.map{x=>
      count=count+1
      val id=list.indexOf(x).toString
      val xy=Json.obj("x"->Math.random()*500,"y"->Math.random()*500)
      val (group,score)=
        if(tableFile2.exists()){
          if(count<=e.drop(1).length) ("evi",elements2("esize").toDouble) //环境node
          else ("gene",elements2("gsize").toDouble) //基因node
        }else ("gene",elements2("gsize").toDouble)
      val data=Json.obj("id"->id,"name"->x,"score"->score,"group"->group)
      Json.obj("data"->data,"position"->xy,"group"->"nodes")
    }


    val result=FileUtils.readLines(new File(path+"/out/pandv.xls")).asScala
    var eid=0;

    var soutar:List[List[String]]=List(List(""))
    var resultFilter = Array("")
    result.drop(1).foreach{x=>
      val ei = x.split("\"").filter(_.trim!="")
      val source=ei(1)
      val target=ei(2)
      val w=ei(3).trim.split("\t").last.toDouble
      val c=ei(3).trim.split("\t").head.toDouble
      if((!soutar.contains(List(source,target)) || !soutar.contains(List(target,source))) && source!=target && w<elements2("pthres").toDouble && Math.abs(c)<elements2("cthres").toDouble) {
        soutar=soutar:+List(source,target):+List(target,source)
        resultFilter=resultFilter:+x
      }
    }

    val edges=resultFilter.drop(1).map{x=>
      val ei = x.split("\"").filter(_.trim!="")
      val source=list.indexOf(ei(1))
      val target=list.indexOf(ei(2))
      eid=eid+1
      val id="e"+eid
      val weight=ei(3).trim.split("\t").last.toDouble
      val cc=ei(3).trim.split("\t").head.toDouble
      val lab="c="+cc.formatted("%."+elements2("dot")+"f")+"；p="+weight.formatted("%."+elements2("dot")+"f")
      val data=Json.obj("source"->source,"target"->target,"weight"->weight,"label"->lab)
      Json.obj("data"->data,"group"->"edges","id"->id)
    }

    val rows=nodes++edges

    val node=Json.obj("selector"->"node", "style"->Json.obj("width"->"mapData(score, 0, 10, 10, 100)", "height"->"mapData(score, 0, 10, 10, 100)", "content"-> "data(name)", "font-size"-> "12px", "text-valign"-> "center", "text-halign"-> "center", "text-outline-width"-> "2px"))

    val font1=elements2("gfont")+"px"
    val nodegene=Json.obj("selector"-> "node[group='gene']","style"->Json.obj( "shape"-> elements2("gshape"), "background-color"-> elements2("netcolor1"), "text-outline-color"-> elements2("netcolor1"), "opacity"-> elements2("gopa"), "font-size"->font1,"color"->elements2("netcolor2")))

    val font2=elements2("efont")+"px"
    val nodeevi=Json.obj("selector"-> "node[group='evi']","style"->Json.obj("shape"-> elements2("eshape"), "background-color"-> elements2("netcolor3"), "text-outline-color"-> elements2("netcolor3"), "opacity"-> elements2("eopa"), "font-size"->font2,"color"->elements2("netcolor4")))

    val nodesele=Json.obj("selector"-> "node:selected","style"->Json.obj("border-width"-> "6px", "border-color"-> "#AAD8FF", "border-opacity"-> "0.5", "background-color"-> "#993399", "text-outline-color"-> "#993399"))

    //    val edgehigh=Json.obj("selector"-> "edge.highlighted","style"->Json.obj("line-color"-> "#2a6cd6", "target-arrow-color"-> "#2a6cd6", "opacity"-> 0.7))

    val edgehigh=Json.obj("selector"-> "edge.highlighted","style"->Json.obj("line-color"-> "#2a6cd6", "target-arrow-color"-> "#2a6cd6", "opacity"-> 0.7, "label"-> "data(label)", "edge-text-rotation"-> "autorotate"))


    //    val edgesele=Json.obj("selector"-> "edge[label]:selected","style"->Json.obj("line-color"-> "#2a6cd6", "target-arrow-color"-> "#2a6cd6", "opacity"-> 0.7, "label"-> "data(label)", "edge-text-rotation"-> "autorotate"))

    val edge=Json.obj("selector"-> "edge","style"->Json.obj("curve-style"-> "haystack", "haystack-radius"-> "0.5", "opacity"-> elements2("opacity"), "line-color"-> elements2("netcolor5"), "width"-> "mapData(weight, 0, 1, 1, 8)", "overlay-padding"-> "3px"))

    val selector=Array(node,nodegene,nodeevi,nodesele,edge,edgehigh)

    Ok(Json.obj("pics"->pics,"downpng"->downpng,"downpics"->downpics,"downtiffs"->downtiffs,"rnum"->rnum,"cnum"->cnum,"elements1"->elements1,"elements2"->elements2,"allcol"->(head(1).trim.split("\t").length-1),"allrow"->(head.length-1),"rows"->rows,"selector"->selector,"table2"->tableFile2.exists()))
  }


  case class ReIGCHeatData(smt:String,lfi:String,cluster_rows:String,crm:String,rp:String, cluster_cols:String,
                           ccm:String, cp:String,designcolor:String,lg:String,color:String,cc:String,nc:String,
                           hasborder:String,cbc:String,hasrname:String, hascname:String,hasnum:String)

  val ReIGCHeatForm: Form[ReIGCHeatData] =Form(
    mapping (
      "smt"->text,
      "lfi"->text,
      "cluster_rows"->text,
      "crm"->text,
      "rp"->text,
      "cluster_cols"->text,
      "ccm"->text,
      "cp"->text,
      "designcolor"->text,
      "lg"->text,
      "color"->text,
      "cc"->text,
      "nc"->text,
      "hasborder"->text,
      "cbc"->text,
      "hasrname"->text,
      "hascname"->text,
      "hasnum"->text
    )(ReIGCHeatData.apply)(ReIGCHeatData.unapply)
  )

  def redrawIGCHeat(taskname:String,rtree:String,ctree:String,xfs:String,yfs:String,sc:String,xfa:String,fn:String)=Action(parse.multipartFormData) { implicit request =>
    val data=ReIGCHeatForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname

    val tableFile=new File(dutyDir+"/out","cor.xls")
    val tagFile=new File(dutyDir+"/out","p_star.xls")

    val lfi=
      if(data.lfi=="TRUE") " -lfi " + tagFile.getAbsolutePath
      else ""

    val rowclu=
      if(data.cluster_rows=="TRUE") " -crw " + data.cluster_rows + " -crm " + data.crm + " -rp " + data.rp
      else " -crw " + data.cluster_rows
    val colclu=
      if(data.cluster_cols=="TRUE") " -ccl " + data.cluster_cols + " -ccm " + data.ccm + " -cp " + data.cp
      else " -ccl " + data.cluster_cols

    val color= if(data.color=="0") data.designcolor else data.color
    val cbc= if(data.hasborder=="white") " -cbc " + data.cbc else " -cbc " + data.hasborder

    val elenet=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements.split("/")(1)

    val elements=Json.obj("smt"->data.smt,"cluster_rows"->data.cluster_rows,"crm"->data.crm,
      "rp"->data.rp,"cluster_cols"->data.cluster_cols,"ccm"->data.ccm,"cp"->data.cp,"sc"->sc,"lg"->data.lg,
      "color"->color,"cc"->data.cc,"nc"->data.nc,"hasborder"->data.hasborder,"cbc"->data.cbc,
      "hasnum"->data.hasnum, "hasrname"->data.hasrname, "hascname"->data.hascname,"rtree"->rtree,
      "ctree"->ctree,"xfs"->xfs, "yfs"->yfs,"xfa"->xfa,"fn"->fn,"lfi"->data.lfi).toString()+"/"+elenet


    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    val head=FileUtils.readFileToString(tableFile).trim.split("\n")
      val rnum = (head.length-1).toString
      val cnum = (head(1).trim.split("\t").length-1).toString

      val command = "Rscript "+Utils.path+"R/heatmap/heatMap.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out"+ " -smt " + data.smt + lfi + rowclu + colclu + " -sc " + sc +
        " -lg " + data.lg + " -c " + color + " -cc " + data.cc + " -nc " + data.nc + cbc +
        " -sn " + data.hasrname + ":" + data.hascname + ":" + data.hasnum + " -th " +
        rtree + ":" + ctree + " -fs " + yfs + ":" + xfs + " -if pdf -cln TRUE -xfa " + xfa + " -fn " + fn

      println(command)

      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (execCommand.isSuccess) {
        Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.png") //替换图片
        Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.tiff") //替换图片
        creatZip(dutyDir) //替换压缩文件包
        val pics=dutyDir+"/out/heatmap.png"
        Ok(Json.obj("valid"->"true","pics"->pics,"cnum"->cnum,"rnum"->rnum))
      } else {
        Ok(Json.obj("valid"->"false","message"->execCommand.getErrStr))
      }

  }

  case class ReIGCNetData(dot:String,pthres:String,cthres:String,gshape:String, netcolor1:String,gopa:String,gsize:String,gfont:String,
                       netcolor2:String,eshape:String,netcolor3:String,eopa:String,esize:String,
                       efont:String,netcolor4:String,netcolor5:String, opacity:String)

  val ReIGCNetForm: Form[ReIGCNetData] =Form(
    mapping (
      "dot"->text,
      "pthres"->text,
      "cthres"->text,
      "gshape"->text,
      "netcolor1"->text,
      "gopa"->text,
      "gsize"->text,
      "gfont"->text,
      "netcolor2"->text,
      "eshape"->text,
      "netcolor3"->text,
      "eopa"->text,
      "esize"->text,
      "efont"->text,
      "netcolor4"->text,
      "netcolor5"->text,
      "opacity"->text
    )(ReIGCNetData.apply)(ReIGCNetData.unapply)
  )

  def redrawIGCNet(taskname:String)=Action(parse.multipartFormData) { implicit request =>
    val data=ReIGCNetForm.bindFromRequest.get
    val id=request.session.get("userId").get

    val eleheat=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements.split("/")(0)

    val elements=eleheat+"/"+Json.obj("gshape"->data.gshape,"netcolor1"->data.netcolor1,"gopa"->data.gopa,"gsize"->data.gsize,"gfont"->data.gfont,"netcolor2"->data.netcolor2,"eshape"->data.eshape,"netcolor3"->data.netcolor3,"eopa"->data.eopa,"esize"->data.esize,"efont"->data.efont,"netcolor4"->data.netcolor4,"netcolor5"->data.netcolor5,"opacity"->data.opacity,"dot"->data.dot,"pthres"->data.pthres,"cthres"->data.cthres).toString()
    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    Ok(Json.obj("valid"->"true"))
  }

  //Tax4Fun
  case class TaxFunData(taskname:String)

  val TaxFunForm: Form[TaxFunData] =Form(
    mapping (
      "taskname"->text
    )(TaxFunData.apply)(TaxFunData.unapply)
  )

  def doTax4Fun=Action(parse.multipartFormData) { implicit request =>
    val data = TaxFunForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val otuFile=new File(dutyDir,"otu.biom")

    val file1 = request.body.file("table1").get
    file1.ref.moveTo(otuFile)

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"TAX","Tax4Fun功能预测",file1.filename,"/","")
    //矩阵文件读取写入任务文件下table.txt

    Future{
      val command = "java -jar "+Utils.path+"R/Tax4Fun/Tax4Fun.jar -i " + otuFile.getAbsolutePath +
        " -o " + dutyDir + "/out"

      println(command)

      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (execCommand.isSuccess) {
        Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.png")
        Utils.pdf2Png(dutyDir+"/out/kegg_L1.pdf",dutyDir+"/out/kegg_L1.png")
        val finish=dutyController.updateFini(id,data.taskname)
        Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.tiff")
        Utils.pdf2Png(dutyDir+"/out/kegg_L1.pdf",dutyDir+"/out/kegg_L1.tiff")

        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"")
        creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readTax4Fun(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    //获取图片
    val (pics,downpics,downpngs,downtiffs)=
      ((path+"/out/pca.png",path+"/out/kegg_L1.png"),(path+"/out/pca.pdf",path+"/out/kegg_L1.pdf"),(path+"/out/pca.png",path+"/out/kegg_L1.png"),(path+"/out/pca.tiff",path+"/out/kegg_L1.tiff"))
    Ok(Json.obj("pics"->pics,"downpng"->downpngs,"downpics"->downpics,"downtiffs"->downtiffs))
  }


  def doFAPROTAX=Action(parse.multipartFormData) { implicit request =>
    val data = TaxFunForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val otuFile=new File(dutyDir,"otu.biom")
    val file1 = request.body.file("table").get
    file1.ref.moveTo(otuFile)

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"FAP","FAPROTAX功能预测",file1.filename,"/","")
    //矩阵文件读取写入任务文件下table.txt

    Future{
      val command = "java -jar "+Utils.path+"R/FAPROTAX/FAPROTAX-1.0-SNAPSHOT.jar -i " + otuFile.getAbsolutePath +
        " -o " + dutyDir + "/out"

      println(command)

      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (execCommand.isSuccess) {
        val finish=dutyController.updateFini(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"")
        val target=dutyDir+"/out/outTables.zip"
        new File(target).createNewFile()
        CompressUtil.zip(dutyDir+"/out/out",target)
        creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }


  case class PicrustData(taskname:String,in_traits:String,stratified:String)

  val PicrustForm: Form[PicrustData] =Form(
    mapping (
      "taskname"->text,
      "in_traits"->text,
      "stratified"->text
    )(PicrustData.apply)(PicrustData.unapply)
  )

  def doPICRUST=Action(parse.multipartFormData) { implicit request =>
    val data = PicrustForm.bindFromRequest.get
    val id=request.session.get("userId").get
    new File(userDutyDir+id+"/"+data.taskname).mkdir()
    new File(userDutyDir+id+"/"+data.taskname+"/temp").mkdir()
    val dutyDir=userDutyDir+id+"/"+data.taskname
    //在用户下创建任务文件夹和结果文件夹
    val otuFile=new File(dutyDir,"otu.biom")
    val seqFile=new File(dutyDir,"seq.fasta")
    val file1 = request.body.file("table1").get
    val file2 = request.body.file("table2").get
    file1.ref.moveTo(otuFile)
    file2.ref.moveTo(seqFile)

    val param="选择数据基因家族:"+data.in_traits+"/是否在各层级产生分层:"+data.stratified

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"PIC","PICRUST2功能预测",file1.filename+"/"+file2.filename,param,"")
    //矩阵文件读取写入任务文件下table.txt

    Future{
      val stratified=if(data.stratified=="yes") " --stratified " else ""
      val command1 = "picrust2_pipeline.py -i " + otuFile.getAbsolutePath +
        " -s " + seqFile.getAbsolutePath + " -o " + dutyDir + "/out" + " --processes 20 " + stratified +
        " --in_traits " + data.in_traits
      val command="cd /root/miniconda3/bin\n" +
        ". ./activate\n"+
        "conda activate picrust2\n"+
        command1+ "\n"+
        "conda deactivate"

      //先放入sh，在运行
      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

      if (execCommand.isSuccess) {
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"")
        new File(dutyDir+"/out/intermediate").delete()
        new File(Utils.path+"/users/"+id+"/"+data.taskname+"/out").listFiles().filter(_.isDirectory).foreach{x=>
          val dname=x.getName
          creatZip(dutyDir+"/out/"+dname+".zip",x.getAbsolutePath)
        }
        creatZip(dutyDir)
        val finish=dutyController.updateFini(id,data.taskname)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  //Bar
  def doBar=Action(parse.multipartFormData) { implicit request =>
    val data = TaxFunForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")
    val file1 = request.body.file("table1").get
    file1.ref.moveTo(tableFile)
    val file2 = request.body.file("table2").get
    file2.ref.moveTo(groupFile)

    val elements=Json.obj("pe"->"TRUE","color"->"#FF0000:#FFC913:#FFFF00:#008000:#00FFFF:#297EFF:#800080:#FFC0CB",
      "width"->"20","height"->"20","dpi"->"300","lp"->"right","lts"->"15","ls"->"15","xts"->"18","yts"->"16",
      "xls"->"20","yls"->"20","xtext"->"","ytext"->"","lms"->"19","lmtext"->"","ms"->"22","mstext"->"").toString()

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"BAR","柱状图",file1.filename+"/"+file2.filename,"/",elements)
    //矩阵文件读取写入任务文件下table.txt

    Future{
      val command = "Rscript " + Utils.path+"R/bar/bar_group.R -i " + tableFile.getAbsolutePath + " -g " +
        groupFile.getAbsolutePath + " -o " + dutyDir + "/out -cs #FF0000:#FFC913:#FFFF00:#008000:#00FFFF:#297EFF:#800080:#FFC0CB"
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (execCommand.isSuccess) {
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"")
        Utils.pdf2Png(dutyDir+"/out/bar_group.pdf",dutyDir+"/out/bar_group.png")
        val finish=dutyController.updateFini(id,data.taskname)
        Utils.pdf2Png(dutyDir+"/out/bar_group.pdf",dutyDir+"/out/bar_group.tiff")
        creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readBarData(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val elements=jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)

    val color=elements("color").split(":")

    val genenum=FileUtils.readLines(new File(path+"/table.txt")).asScala.length-1

    //获取图片
    val pics=getReDrawPics(path)
    Ok(Json.obj("pics"->pics,"elements"->elements,"color"->color,"genenum"->genenum))
  }

  case class ReBarData(pe:String, designcolor:String, lp:String, dpi:String, width:String, height:String,
                       xts:String, xls:String, xtext:String, yts:String, yls:String, ytext:String,
                       lts:String, lms:String, lmtext:String, ls:String,ms:String,mstext:String)

  val ReBarForm: Form[ReBarData] =Form(
    mapping (
      "pe"->text,
      "designcolor"->text,
      "lp"->text,
      "dpi"->text,
      "width"->text,
      "height"->text,
      "xts"->text,
      "xls"->text,
      "xtext"->text,
      "yts"->text,
      "yls"->text,
      "ytext"->text,
      "lts"->text,
      "lms"->text,
      "lmtext"->text,
      "ls"->text,
      "ms"->text,
      "mstext"->text,
    )(ReBarData.apply)(ReBarData.unapply)
  )

  def redrawBar(taskname:String)=Action(parse.multipartFormData) { implicit request =>
    val data=ReBarForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname

    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")

    val elements=Json.obj("pe"->data.pe,"color"->data.designcolor,"width"->data.width,
      "height"->data.height,"dpi"->data.dpi,"lp"->data.lp,"lts"->data.lts,"ls"->data.ls,"xts"->data.xts,
      "yts"->data.yts,"xls"->data.xls,"yls"->data.yls,"xtext"->data.xtext,"ytext"->data.ytext,
      "lms"->data.lms,"lmtext"->data.lmtext,"ms"->data.ms,"mstext"->data.mstext).toString()

    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    val ytext=
      if(data.pe=="TRUE") ""
      else if(!data.ytext.equals(""))" -yls sans:bold.italic:" + data.yls + ":\"" + data.ytext + "\""
      else " -yls sans:bold.italic:" + data.yls + ":\" \""
    val xtext=
      if(!data.xtext.equals("")) " -xls sans:bold.italic:" + data.xls + ":\"" + data.xtext + "\""
      else ""
    val lms=
      if(!data.lmtext.equals("")) " -lms sans:bold.italic:" + data.lms + ":\"" + data.lmtext + "\""
      else ""
    val ms=
      if(!data.mstext.equals("")) " -ms sans:bold.italic:" + data.ms + ":\"" + data.mstext + "\""
      else ""

    val command = "Rscript " + Utils.path+"R/bar/bar_group.R -i " + tableFile.getAbsolutePath + " -g " +
      groupFile.getAbsolutePath + " -o " + dutyDir + "/out -cs \"" + data.designcolor + "\" -pe " + data.pe +
    " -lp " + data.lp + " -dpi " + data.dpi + " -is " + data.width + ":" + data.height + " -lts sans:bold.italic:" +
      data.lts + " -ls " + data.ls + " -xts sans:bold.italic:" + data.xts + xtext + " -yts sans:bold.italic:" +
      data.yts + ytext + lms + ms

    println(command)

    //先放入sh，在运行
    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/bar_group.pdf",dutyDir+"/out/bar_group.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/bar_group.pdf",dutyDir+"/out/bar_group.tiff") //替换图片
      creatZip(dutyDir) //替换压缩文件包
      val pics=dutyDir+"/out/bar_group.png"
      Ok(Json.obj("valid"->"true","pics"->pics))
    } else {
      Ok(Json.obj("valid"->"false","message"->execCommand.getErrStr))
    }

  }

  //Lefse
  case class LefseData(taskname:String,l:String)

  val LefseForm: Form[LefseData] =Form(
    mapping (
      "taskname"->text,
      "l"->text
    )(LefseData.apply)(LefseData.unapply)
  )

  def doLefse(filenum:Int)=Action(parse.multipartFormData) { implicit request =>
    val data = LefseForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)
    new File(userDutyDir+id+"/"+data.taskname+"/tax_summary_a").mkdir()
    val inputDir=userDutyDir+id+"/"+data.taskname+"/tax_summary_a"
    var input=""
    var i=1
    while(i<=filenum){
      val otuFile=new File(inputDir,"otu_taxa_table_L"+i.toString+".txt")
      val file1 = request.body.file("table"+i.toString).get
      input=input+file1.filename+"/"
      file1.ref.moveTo(otuFile)
      otuFile.setExecutable(true,false)
      otuFile.setReadable(true,false)
      otuFile.setWritable(true,false)
      i=i+1
    }
    val groupFile=new File(dutyDir,"map.txt")
    val file2 = request.body.file("group").get
    input+=file2.filename
    file2.ref.moveTo(groupFile)
    groupFile.setExecutable(true,false)
    groupFile.setReadable(true,false)
    groupFile.setWritable(true,false)

    val param="对数绝对值的阈值："+data.l

    val start=dutyController.insertDuty(data.taskname,id,"LEF","lefse分析",input,param,"")

    Future{
      val command1 = "java -jar "+Utils.path+"R/lefse/lefse-data.jar -i "+inputDir+"/ -o "+dutyDir+"/out -m "+dutyDir+"/map.txt -l 2"

      val command2= "java -jar "+Utils.path+"R/lefse/lefse-plot.jar -i "+dutyDir+"/out/lefse_LDA.xls -o "+dutyDir+"/out/"

      val commandpack=Array(command1,command2)
      println(command1)
      println(command2)

      val execCommand = new ExecCommand
      execCommand.exec(commandpack)

      if (execCommand.isSuccess) {
        Utils.pdf2Png(dutyDir+"/out/lefse_LDA.cladogram.pdf",dutyDir+"/out/lefse_LDA.cladogram.png")
        Utils.pdf2Png(dutyDir+"/out/lefse_LDA.pdf",dutyDir+"/out/lefse_LDA.png")
        val finish=dutyController.updateFini(id,data.taskname)
        Utils.pdf2Png(dutyDir+"/out/lefse_LDA.cladogram.pdf",dutyDir+"/out/lefse_LDA.cladogram.tiff")
        Utils.pdf2Png(dutyDir+"/out/lefse_LDA.pdf",dutyDir+"/out/lefse_LDA.tiff")
        println(execCommand.getOutStr)
        println(execCommand.getErrStr)
        creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)

        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readLefse(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname
    println(new File(path+"/out/lefse_LDA.png").length())
    val lefse_LDA= if(new File(path+"/out/lefse_LDA.png").length()==0) "false" else "true"
    //获取图片
    val pics= ((path+"/out/lefse_LDA.png",path+"/out/lefse_LDA.cladogram.png"))
    Ok(Json.obj("pics"->pics,"lefse_LDA"->lefse_LDA))
  }

  //Abiview
  case class AbiviewData(taskname:String,graph:String,osformat:String,window:String,startbase:String,
                         endbase:String,bases:String,separate:String,sequence:String,
                         yticks:String)

  val AbiviewForm: Form[AbiviewData] =Form(
    mapping (
      "taskname"->text,
      "graph"->text,
      "osformat"->text,
      "window"->text,
      "startbase"->text,
      "endbase"->text,
      "bases"->text,
      "separate"->text,
      "sequence"->text,
      "yticks"->text
    )(AbiviewData.apply)(AbiviewData.unapply)
  )

  def doAbiview=Action(parse.multipartFormData) { implicit request =>
    val data = AbiviewForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)

    val abiFile=new File(dutyDir,"abiview.ab1")
    val file = request.body.file("table1").get
    val input=file.filename
    file.ref.moveTo(abiFile)
    abiFile.setExecutable(true,false)
    abiFile.setReadable(true,false)
    abiFile.setWritable(true,false)

    val param="输出图片类型：" + data.graph + "/输出序列格式：" + data.osformat + "/一个窗口显示" + data.window +
      "个碱基" + "/绘制第" + data.startbase + "-" + data.endbase + "个碱基" + "/绘制迹线的碱基：" + data.bases +
      "/是否分开绘制不同碱基的迹线：" + data.separate + "/是否将序列显示在图片上：" + data.sequence +
      "/是否绘制Y轴刻度：" + data.yticks

    val element=Json.obj("graph"->data.graph, "osformat"->data.osformat, "window"->data.window,
      "startbase"->data.startbase, "endbase"->data.endbase, "bases"->data.bases, "separate"->data.separate,
      "sequence"->data.sequence, "yticks"->data.yticks,"gtitle"->"","gsubtitle"->"","gxtitle"->"").toString()

    val start=dutyController.insertDuty(data.taskname,id,"ABI","Emboss Abiview",input,param,element)

    Future{
      val command1 = Utils.path+"R/EMBOSS/EMBOSS-6.6.0/emboss/abiview -infile "+ abiFile.getAbsolutePath +
        " -outseq " + dutyDir + "/out/sequence." + data.osformat + " -osformat2 " +  data.osformat +
        " -startbase " + data.startbase + " -endbase " + data.endbase + " -yticks " + data.yticks +
        " -sequence " + data.sequence + " -window " + data.window + " -bases " + data.bases +
        " -graph ps -gdirectory " + dutyDir + "/out" + " -separate " + data.separate


      val command2= Utils.path+"R/EMBOSS/EMBOSS-6.6.0/emboss/abiview -infile "+
        abiFile.getAbsolutePath + " -outseq "+dutyDir+"/out/sequence." + data.osformat +
        " -osformat2 " + data.osformat + " -startbase " + data.startbase + " -endbase " +
        data.endbase + " -yticks " + data.yticks + " -sequence " + data.sequence +
        " -window " + data.window + " -bases " + data.bases + " -graph " + data.graph +
        " -gdirectory " + dutyDir + "/out" + " -separate " + data.separate

      val command3="ps2pdf " + dutyDir + "/out/abiview.ps " + dutyDir + "/out/abiview.pdf"

      val commandpack= if(data.graph=="ps") Array(command1, command3) else Array(command1, command2, command3)

      println(command1)
      println(command2)
      println(command3)

      val execCommand = new ExecCommand
      execCommand.exec(commandpack)

      if (execCommand.isSuccess) {
        val finish=dutyController.updateFini(id,data.taskname)
        creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readAbiview(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname
    val elements=jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)
    val pics= path+"/out/abiview.pdf"
    Ok(Json.obj("pdfUrl"->pics,"elements"->elements))
  }

  case class ReAbiviewData(window:String,startbase:String,endbase:String,bases:String,
                           separate:String,sequence:String,yticks:String,gtitle:String,
                           gsubtitle:String,gxtitle:String)

  val ReAbiviewForm: Form[ReAbiviewData] =Form(
    mapping (
      "window"->text,
      "startbase"->text,
      "endbase"->text,
      "bases"->text,
      "separate"->text,
      "sequence"->text,
      "yticks"->text,
      "gtitle"->text,
      "gsubtitle"->text,
      "gxtitle"->text
    )(ReAbiviewData.apply)(ReAbiviewData.unapply)
  )

  def redrawAbiview(taskname:String)=Action(parse.multipartFormData) { implicit request =>
    val data = ReAbiviewForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"/users/"+id+"/"+taskname
    val abiFile=new File(dutyDir,"abiview.ab1")

    val oldelements=jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)
    val graph=oldelements.get("graph").get
    val osformat=oldelements.get("osformat").get
    val gtitle=if(data.gxtitle=="") "" else " -gtitle \"" + data.gtitle + "\""
    val gsubtitle=if(data.gsubtitle=="") "" else " -gsubtitle \"" + data.gsubtitle + "\""
    val gxtitle=if(data.gxtitle=="") "" else " -gxtitle \"" + data.gxtitle + "\""

    val elements=Json.obj("graph"->graph, "osformat"->osformat, "window"->data.window,
      "startbase"->data.startbase, "endbase"->data.endbase, "bases"->data.bases, "separate"->data.separate,
      "sequence"->data.sequence, "yticks"->data.yticks,"gtitle"->data.gtitle,"gsubtitle"->data.gsubtitle,
      "gxtitle"->data.gxtitle).toString()
    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    val command1 = Utils.path+"R/EMBOSS/EMBOSS-6.6.0/emboss/abiview -infile "+ abiFile.getAbsolutePath +
      " -outseq " + dutyDir + "/out/sequence." + osformat + " -osformat2 " +  osformat +
      " -startbase " + data.startbase + " -endbase " + data.endbase + " -yticks " + data.yticks +
      " -sequence " + data.sequence + " -window " + data.window + " -bases " + data.bases +
      " -graph ps -gdirectory " + dutyDir + "/out" + " -separate " + data.separate + gtitle +
      gsubtitle + gxtitle

    val command2= Utils.path+"R/EMBOSS/EMBOSS-6.6.0/emboss/abiview -infile "+
      abiFile.getAbsolutePath + " -outseq "+dutyDir+"/out/sequence." + osformat +
      " -osformat2 " + osformat + " -startbase " + data.startbase + " -endbase " +
      data.endbase + " -yticks " + data.yticks + " -sequence " + data.sequence +
      " -window " + data.window + " -bases " + data.bases + " -graph " + graph +
      " -gdirectory " + dutyDir + "/out" + " -separate " + data.separate + gtitle +
      gsubtitle + gxtitle

    val command3="ps2pdf " + dutyDir + "/out/abiview.ps " + dutyDir + "/out/abiview.pdf"

    val commandpack =
      if(graph=="ps") FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command1+"\n"+command3)
      else FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command1+"\n"+command2+"\n"+command3)

    println(command1)
    println(command2)
    println(command3)

    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    println(execCommand.getErrStr)
    println(execCommand.getOutStr)

    if (execCommand.isSuccess) {
      creatZip(dutyDir) //替换压缩文件包
      Ok(Json.obj("valid"->"true"))
    } else {
      Ok(Json.obj("valid"->"false"))
    }
  }


  //Revseq
  case class RevseqData(taskname:String,osformat:String,reverse:String,tag:String,complement:String)

  val RevseqForm: Form[RevseqData] =Form(
    mapping (
      "taskname"->text,
      "osformat"->text,
      "reverse"->text,
      "tag"->text,
      "complement"->text
    )(RevseqData.apply)(RevseqData.unapply)
  )

  def doRevseq=Action(parse.multipartFormData) { implicit request =>
    val data = RevseqForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)

    val seqFile=new File(dutyDir,"seq.txt")
    val file = request.body.file("table1").get
    val input=file.filename
    file.ref.moveTo(seqFile)
    seqFile.setExecutable(true,false)
    seqFile.setReadable(true,false)
    seqFile.setWritable(true,false)

    val tag=if(data.reverse=="N") "" else "/是否在输出序列中加“Reversed:”标签：" + data.tag

    val param="输出序列格式：" + data.osformat + "/输出序列是否要反转：" + data.reverse + tag +
      "/是否要补足输出序列：" + data.complement

    val start=dutyController.insertDuty(data.taskname,id,"RSQ","Emboss Revseq",input,param,"")

    Future{
      val command = Utils.path+"R/EMBOSS/EMBOSS-6.6.0/emboss/revseq -sequence "+ seqFile.getAbsolutePath +
        " -outseq " + dutyDir + "/out/sequence." + data.osformat + " -osformat2 " +  data.osformat +
        " -reverse " + data.reverse + " -tag " + data.tag + " -complement " + data.complement

      println(command)

      val execCommand = new ExecCommand
      execCommand.exec(command)

      if (execCommand.isSuccess) {
        val finish=dutyController.updateFini(id,data.taskname)
        creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }


  //Getorf
  case class GetorfData(taskname:String,osformat:String,table:String,minsize:String,maxsize:String,
                        find:String,methionine:String,circular:String,reverse:String,flanking:String)

  val GetorfForm: Form[GetorfData] =Form(
    mapping (
      "taskname"->text,
      "osformat"->text,
      "table"->text,
      "minsize"->text,
      "maxsize"->text,
      "find"->text,
      "methionine"->text,
      "circular"->text,
      "reverse"->text,
      "flanking"->text
    )(GetorfData.apply)(GetorfData.unapply)
  )

  def doGetorf=Action(parse.multipartFormData) { implicit request =>
    val data = GetorfForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)

    val seqFile=new File(dutyDir,"seq.txt")
    val file = request.body.file("table1").get
    val input=file.filename
    file.ref.moveTo(seqFile)
    seqFile.setExecutable(true,false)
    seqFile.setReadable(true,false)
    seqFile.setWritable(true,false)

    val table=data.table.split(":")
    val find=data.find.split(":")

    val param="输出序列格式：" + data.osformat + "/选择使用参考：" + table(1) +
      "/ORF显示的最小核苷酸大小：" + data.minsize + "/ORF显示的最大核苷酸大小：" + data.maxsize +
      "/输出类型：" + find(1) + "/是否将初始START密码子更改为蛋氨酸：" + data.methionine +
      "/序列是否为循环的：" + data.circular + "/是否以相反的顺序找到ORF：" + data.reverse +
      "/报告侧翼核苷酸的数量：" + data.flanking

    val start=dutyController.insertDuty(data.taskname,id,"GTF","Emboss Getorf",input,param,"")

    Future{
      val command = Utils.path+"R/EMBOSS/EMBOSS-6.6.0/emboss/getorf -sequence "+ seqFile.getAbsolutePath +
        " -outseq " + dutyDir + "/out/sequence." + data.osformat + " -osformat2 " +  data.osformat +
        " -table " + table(0) + " -minsize " + data.minsize + " -maxsize " + data.maxsize + " -find " +
        find(0) + " -methionine " + data.methionine + " -circular " + data.circular + " -reverse " +
        data.reverse + " -flanking " + data.flanking

      println(command)

      val execCommand = new ExecCommand
      execCommand.exec(command)

      if (execCommand.isSuccess) {
        val finish=dutyController.updateFini(id,data.taskname)
        creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }


  //Eprimer
  case class EprimerData(taskname:String,primer:String,task:String,hybridprobe:String,numreturn:String,
                         includedregion:String,targetregion:String,excludedregion:String,
                         forwardinput:String,reverseinput:String,gcclamp:String,optsize:String,
                         minsize:String,maxsize:String,opttm:String,mintm:String,maxtm:String,
                         maxdifftm:String,ogcpercent:String,mingc:String,maxgc:String,saltconc:String)

  val EprimerForm: Form[EprimerData] =Form(
    mapping (
      "taskname"->text,
      "primer"->text,
      "task"->text,
      "hybridprobe"->text,
      "numreturn"->text,
      "includedregion"->text,
      "targetregion"->text,
      "excludedregion"->text,
      "forwardinput"->text,
      "reverseinput"->text,
      "gcclamp"->text,
      "optsize"->text,
      "minsize"->text,
      "maxsize"->text,
      "opttm"->text,
      "mintm"->text,
      "maxtm"->text,
      "maxdifftm"->text,
      "ogcpercent"->text,
      "mingc"->text,
      "maxgc"->text,
      "saltconc"->text
    )(EprimerData.apply)(EprimerData.unapply)
  )

  case class EprimerData2(dnaconc:String,maxpolyx:String,psizeopt:String,prange:String,ptmopt:String,
                          ptmmin:String,ptmmax:String,oexcludedregion:String,
                          oligoinput:String,osizeopt:String,ominsize:String,omaxsize:String,
                          otmopt:String,otmmin:String,otmmax:String,ogcopt:String,ogcmin:String,
                          ogcmax:String,osaltconc:String,odnaconc:String,oanyself:String,oendself:String)

  val EprimerForm2: Form[EprimerData2] =Form(
    mapping (
      "dnaconc"->text,
      "maxpolyx"->text,
      "psizeopt"->text,
      "prange"->text,
      "ptmopt"->text,
      "ptmmin"->text,
      "ptmmax"->text,
      "excludedregion"->text,
      "oligoinput"->text,
      "osizeopt"->text,
      "ominsize"->text,
      "omaxsize"->text,
      "otmopt"->text,
      "otmmin"->text,
      "otmmax"->text,
      "ogcopt"->text,
      "ogcmin"->text,
      "ogcmax"->text,
      "osaltconc"->text,
      "odnaconc"->text,
      "oanyself"->text,
      "oendself"->text
    )(EprimerData2.apply)(EprimerData2.unapply)
  )

  case class EprimerData3(opolyxmax:String,omishybmax:String,explainflag:String,fileflag:String,
                          pickanyway:String,maxmispriming:String,pairmaxmispriming:String,
                          numnsaccepted:String,selfany:String,selfend:String,maxendstability:String,
                          okleftregion:String,okrightregion:String,scorrection:String,tmformula:String)

  val EprimerForm3: Form[EprimerData3] =Form(
    mapping (
      "opolyxmax"->text,
      "omishybmax"->text,
      "explainflag"->text,
      "fileflag"->text,
      "pickanyway"->text,
      "maxmispriming"->text,
      "pairmaxmispriming"->text,
      "numnsaccepted"->text,
      "selfany"->text,
      "selfend"->text,
      "maxendstability"->text,
      "okleftregion"->text,
      "okrightregion"->text,
      "scorrection"->text,
      "tmformula"->text
    )(EprimerData3.apply)(EprimerData3.unapply)
  )


  def doEprimer=Action(parse.multipartFormData) { implicit request =>
    val data = EprimerForm.bindFromRequest.get
    val data2 = EprimerForm2.bindFromRequest.get
    val data3 = EprimerForm3.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)

    val seqFile=new File(dutyDir,"seq.txt")
    val mishybFile=new File(dutyDir,"mishyb.txt")
    val mispriFile=new File(dutyDir,"mispri.txt")

    val file = request.body.file("table1").get
    file.ref.moveTo(seqFile)
    val (input,mishyblibraryfile,mispriminglibraryfile)=
      if(request.body.file("table2").isEmpty && request.body.file("table3").isEmpty)
        (file.filename,"","")
      else if(request.body.file("table2").isEmpty) {
        request.body.file("table3").get.ref.moveTo(mispriFile)
        (file.filename+"/"+request.body.file("table3").get.filename,""," -mispriminglibraryfile "+mispriFile.getAbsolutePath)
      } else if(request.body.file("table3").isEmpty) {
        request.body.file("table2").get.ref.moveTo(mishybFile)
        (file.filename+"/"+request.body.file("table2").get.filename," -mishyblibraryfile "+mishybFile.getAbsolutePath,"")
      } else {
        request.body.file("table3").get.ref.moveTo(mispriFile)
        request.body.file("table2").get.ref.moveTo(mishybFile)
        (file.filename+"/"+request.body.file("table2").get.filename+"/"+request.body.file("table3").get.filename," -mishyblibraryfile "+mishybFile.getAbsolutePath," -mispriminglibraryfile "+mispriFile.getAbsolutePath)
      }

    seqFile.setExecutable(true,false)
    seqFile.setReadable(true,false)
    seqFile.setWritable(true,false)
    mishybFile.setExecutable(true,false)
    mishybFile.setReadable(true,false)
    mishybFile.setWritable(true,false)
    mispriFile.setExecutable(true,false)
    mispriFile.setReadable(true,false)
    mispriFile.setWritable(true,false)

    val task=data.task.split(":")
    val scorrection=data3.scorrection.split(":")
    val tmformula=data3.tmformula.split(":")
    val includedregion=if(data.includedregion=="") "" else "/Included region(s)：" + data.includedregion
    val targetregion=if(data.targetregion=="") "" else "/Target region(s)：" + data.targetregion
    val excludedregion=if(data.excludedregion=="") "" else "/Excluded region(s)：" + data.excludedregion
    val forwardinput=if(data.forwardinput=="") "" else "/Forward input primer sequence to check：" + data.forwardinput
    val reverseinput=if(data.reverseinput=="") "" else "/Reverse input primer sequence to check：" + data.reverseinput
    val okleftregion=if(data3.okleftregion=="") "" else "/Possible left primer of pair location：" + data3.okleftregion
    val okrightregion=if(data3.okrightregion=="") "" else "/Possible right primer of pair location：" + data3.okrightregion
    val oexcludedregion=if(data2.oexcludedregion=="") "" else "/Internal oligo excluded region：" + data2.oexcludedregion
    val oligoinput=if(data2.oligoinput=="") "" else "/Internal oligo input sequence (if any)：" + data2.oligoinput

    val param="Pick PCR primer(s)：" + data.primer + "/Select task：" + task(1) +
      "/Pick hybridization probe：" + data.hybridprobe + "/Number of results to return：" +
      data.numreturn + includedregion + targetregion + excludedregion + forwardinput +
      reverseinput + okleftregion + okrightregion +
      "/GC clamp：" + data.gcclamp + "/Primer optimum size：" + data.optsize +
      "/Primer minimum size：" + data.minsize + "/Primer maximum size：" + data.maxsize +
      "/Primer optimum Tm：" + data.opttm + "/Primer minimum Tm：" + data.mintm +
      "/Primer maximum Tm：" + data.maxtm + "/Maximum difference in Tm of primers：" + data.maxdifftm +
      "/Primer optimum GC percent：" + data.ogcpercent + "/Primer minimum GC percent：" + data.mingc +
      "/Primer maximum GC percent：" + data.maxgc + "/Salt concentration (mM)：" + data.saltconc +
      "/DNA concentration (nM)：" + data2.dnaconc + "/Maximum polynucleotide repeat：" + data2.maxpolyx +
      "/Product optimum size：" + data2.psizeopt + "/Product size range：" + data2.prange +
      "/Product optimum Tm：" + data2.ptmopt + "/Product minimum Tm：" + data2.ptmmin +
      oexcludedregion + oligoinput + "/Internal oligo optimum size：" + data2.osizeopt +
      "/Internal oligo minimum size：" + data2.ominsize + "/Internal oligo maximum size：" +
      data2.omaxsize + "/Internal oligo optimum Tm：" + data2.otmopt +
      "/Internal oligo minimum Tm：" + data2.otmmin + "/Internal oligo maximum Tm：" + data2.otmmax +
      "/Internal oligo optimum GC percent：" + data2.ogcopt + "/Internal oligo minimum GC：" + data2.ogcmin +
      "/Internal oligo maximum GC：" + data2.ogcmax + "/Internal oligo salt concentration (mM)：" +
      data2.osaltconc + "/Internal oligo DNA concentration (nM)：" + data2.odnaconc +
      "/Internal oligo maximum self complementarity：" + data2.oanyself +
      "/Internal oligo maximum 3' self complementarity：" + data2.oendself +
      "/Internal oligo maximum polynucleotide repeat：" + data3.opolyxmax +
      "/Internal oligo maximum mishybridization：" + data3.omishybmax + "/Explain flag：" +
      data3.explainflag + "/Create results files for each sequence：" +
      data3.fileflag + "/Pick anyway：" + data3.pickanyway + "/Primer maximum mispriming：" +
      data3.maxmispriming + "/Primer pair maximum mispriming：" + data3.pairmaxmispriming +
      "/Maximum Ns accepted in a primer：" + data3.numnsaccepted + "/Maximum self complementarity：" +
      data3.selfany + "/Maximum 3' self complementarity：" + data3.selfend + "/Select Task：" +
      scorrection(1)  + "/Select formula：" + tmformula(1) + "/Maximum 3' end stability：" +
      data3.maxendstability



    val start=dutyController.insertDuty(data.taskname,id,"PMR","Emboss Eprimer3",input,param,"")

    Future{

      val includedregion_command=
        if(data.includedregion=="") "" else " -includedregion \"" + data.includedregion + "\""
      val targetregion_command=
        if(data.targetregion=="") "" else " -targetregion \"" + data.targetregion + "\""
      val excludedregion_command=
        if(data.excludedregion=="") "" else " -excludedregion \"" + data.excludedregion + "\""
      val forwardinput_command=
        if(data.forwardinput=="") "" else " -forwardinput \"" + data.forwardinput + "\""
      val reverseinput_command=
        if(data.reverseinput=="") "" else " -reverseinput \"" + data.reverseinput + "\""
      val okleftregion_command=
        if(data3.okleftregion=="") "" else " -okleftregion \"" + data3.okleftregion + "\""
      val okrightregion_command=
        if(data3.okrightregion=="") "" else " -okrightregion \"" + data3.okrightregion + "\""
      val oexcludedregion_command=
        if(data2.oexcludedregion=="") "" else " -oexcludedregion \"" + data2.oexcludedregion + "\""
      val oligoinput_command=
        if(data2.oligoinput=="") "" else " -oligoinput \"" + data2.oligoinput + "\""

      val command = Utils.path+"R/EMBOSS/EMBOSS-6.6.0/emboss/eprimer32 -sequence " + seqFile.getAbsolutePath +
        " -outfile " + dutyDir + "/out/sequence.eprimer32" + mishyblibraryfile + mispriminglibraryfile +
        " -primer \"" + data.primer + "\" -task \"" + task(0) + "\" -hybridprobe \"" + data.hybridprobe +
        "\" -numreturn \"" + data.numreturn + "\"" + includedregion_command + targetregion_command +
        excludedregion_command + forwardinput_command + reverseinput_command + okleftregion_command +
        okrightregion_command + " -gcclamp \"" + data.gcclamp + "\" -optsize \"" + data.optsize + "\" -minsize \"" +
        data.minsize + "\" -maxsize \"" + data.maxsize + "\" -opttm \"" + data.opttm + "\" -mintm \"" + data.mintm +
        "\" -maxtm \"" + data.maxtm + "\" -maxdifftm \"" + data.maxdifftm + "\" -ogcpercent \"" + data.ogcpercent +
        "\" -mingc \"" + data.mingc + "\" -maxgc \"" + data.maxgc + "\" -saltconc \"" + data.saltconc + "\" -dnaconc \"" +
        data2.dnaconc + "\" -maxpolyx \"" + data2.maxpolyx + "\" -psizeopt \"" + data2.psizeopt + "\" -prange \"" +
        data2.prange + "\" -ptmopt \"" + data2.ptmopt + "\" -ptmmin \"" + data2.ptmmin + "\" -ptmmax \"" +
        data2.ptmmax + "\"" + oexcludedregion_command + oligoinput_command + " -osizeopt \"" + data2.osizeopt +
        "\" -ominsize \"" + data2.ominsize + "\" -omaxsize \"" + data2.omaxsize + "\" -otmopt \"" + data2.otmopt +
        "\" -otmmin \"" + data2.otmmin + "\" -otmmax \"" + data2.otmmax + "\" -ogcopt \"" + data2.ogcopt + "\" -ogcmin \"" +
        data2.ogcmin + "\" -ogcmax \"" + data2.ogcmax + "\" -osaltconc \"" + data2.osaltconc + "\" -odnaconc \"" +
        data2.odnaconc + "\" -oanyself \"" + data2.oanyself + "\" -oendself \"" + data2.oendself + "\" -opolyxmax \"" +
        data3.opolyxmax + "\" -omishybmax \"" + data3.omishybmax + "\" -explainflag \"" + data3.explainflag +
        "\" -fileflag \"" + data3.fileflag + "\" -pickanyway \"" + data3.pickanyway + "\" -maxmispriming \"" +
        data3.maxmispriming + "\" -pairmaxmispriming \"" + data3.pairmaxmispriming + "\" -numnsaccepted \"" +
        data3.numnsaccepted + "\" -selfany \"" + data3.selfany + "\" -selfend \"" + data3.selfend +
        "\" -scorrection \"" + scorrection(0) + "\" -tmformula \"" + tmformula(0) +
        "\" -maxendstability \"" + data3.maxendstability + "\""

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

      if (execCommand.isSuccess) {
        val finish=dutyController.updateFini(id,data.taskname)
        creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }



  //Volcano
  case class VolcanoData(taskname:String,pcl:String,fcl:String)

  val VolcanoForm: Form[VolcanoData] = Form(
    mapping (
      "taskname"->text,
      "pcl"->text,
      "fcl"->text
    )(VolcanoData.apply)(VolcanoData.unapply)
  )

  def doVolcano=Action(parse.multipartFormData){ implicit request=>
    val data=VolcanoForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val file1=request.body.file("table1").get
    val tableFile=new File(dutyDir,"table.txt")
    val input= file1.filename
    val param= "P值阈值："+data.pcl+"/F值阈值："+data.fcl


    val elements= Json.obj("pcl"->data.pcl,"fcl"->data.fcl,"sp"->"TRUE","lc"->"black",
      "cs"->"#B7B7B7:#4DAF4A:#1E90FF:#E41A1C","xts"->"18","yts"->"16","xls"->"20","yls"->"20",
      "xtext"->"log2(FC)","ytext"->"-log10(pvalue)","lp"->"bottom","lt"->"20","width"->"20",
      "height"->"20","dpi"->"300").toString()

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"VOC","火山图（Volcano）",input,param,elements)
    //矩阵文件读取写入任务文件下table.txt
    file1.ref.moveTo(tableFile)

    Future{
      val command = "Rscript "+Utils.path+"R/volcano/mountain.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out" + " -pcl " + data.pcl + " -fcl " + data.fcl + " -if pdf -in volcano -lt 6:20"
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      println(command)

      if (execCommand.isSuccess) {
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"")
        Utils.pdf2Png(dutyDir+"/out/volcano.pdf",dutyDir+"/out/volcano.png")
        val finish=dutyController.updateFini(id,data.taskname)
        Utils.pdf2Png(dutyDir+"/out/volcano.pdf",dutyDir+"/out/volcano.tiff")
        creatZip(dutyDir)

      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readVolcanoData(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val elements=jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)
    val color=elements("cs").split(":")

    val group=("part1","part2","part3","part4")

    //获取图片
    val pics=getReDrawPics(path)
    val (downpics,downpng,downtiffs)=(path+"/out/volcano.pdf",path+"/out/volcano.png",path+"/out/volcano.tiff")
    Ok(Json.obj("group"->group,"pics"->pics,"downpng"->downpng,"downpics"->downpics,"downtiffs"->downtiffs,"elements"->elements,"color"->color))
  }

  case class ReVolcanoData(pcl:String,fcl:String,sp:String,lp:String,width:String,height:String,
                           dpi:String,color4:String,color:String,xts:String, xls:String,
                           xtext:String,yts:String,yls:String,ytext:String,lt:String)

  val ReVolcanoForm: Form[ReVolcanoData] =Form(
    mapping (
      "pcl"->text,
      "fcl"->text,
      "sp"->text,
      "lp"->text,
      "width"->text,
      "height"->text,
      "dpi"->text,
      "color4"->text,
      "color"->text,
      "xts"->text,
      "xls"->text,
      "xtext"->text,
      "yts"->text,
      "yls"->text,
      "ytext"->text,
      "lt"->text
    )(ReVolcanoData.apply)(ReVolcanoData.unapply)
  )

  def redrawVolcano(taskname:String)=Action(parse.multipartFormData) { implicit request =>
    val data=ReVolcanoForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname

    val tableFile=new File(dutyDir,"table.txt")

    val elements= Json.obj("pcl"->data.pcl,"fcl"->data.fcl,"sp"->data.sp,"lc"->data.color4,
      "cs"->data.color,"xts"->data.xts,"yts"->data.yts,"xls"->data.xls,"yls"->data.yls,
      "xtext"->data.xtext,"ytext"->data.ytext,"lp"->data.lp,"lt"->data.lt,"width"->data.width,
      "height"->data.height,"dpi"->data.dpi).toString()

    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    val command = "Rscript "+Utils.path+"R/volcano/mountain.R -i "+ tableFile.getAbsolutePath +
      " -o " +dutyDir+"/out" + " -pcl " + data.pcl + " -fcl " + data.fcl + " -sp " + data.sp +
      " -lc \"" + data.color4 + "\" -cs \"" + data.color + "\" -xts sans:bold.italic:" + data.xts +
      " -xls \"sans:bold.italic:" + data.xls + ":" + data.xtext + "\" -yts sans:bold.italic:" +
      data.yts + " -yls \"sans:bold.italic:" + data.yls + ":" + data.ytext + "\" -lp " + data.lp +
      " -lt \"6:" + data.lt + "\" -is \"" + data.width + ":" + data.height + "\" -dpi " + data.dpi +
      " -if pdf -in volcano"

    println(command)

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/volcano.pdf",dutyDir+"/out/volcano.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/volcano.pdf",dutyDir+"/out/volcano.tiff") //替换图片
      creatZip(dutyDir) //替换压缩文件包
      val pics=dutyDir+"/out/volcano.png"
      val pdfs=dutyDir+"/out/volcano.pdf"
      val tiffs=dutyDir+"/out/volcano.tiff"
      Ok(Json.obj("valid"->"true","pics"->pics,"downpics"->pdfs,"downtiffs"->tiffs))
    } else {
      Ok(Json.obj("valid"->"false"))
    }

  }


  //Manhattan
  def doManhattan(samnum:Int,sam:String)=Action(parse.multipartFormData){ implicit request=>
    val data=TaxFunForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val file1=request.body.file("table1").get
    val tableFile=new File(dutyDir,"table.txt")
    val input= file1.filename

    val (cs,ahpos,ahcolo,ahsize,xtext,ytext,width,height)=
      if(samnum>1)
        ("#3A89CC:#769C30:#CD0000:#D99536:#7B0078:#BFBC3B:#6E8B3D:#00688B:#C10077:#CAAA76:#474700:#458B00:#8B4513:#008B8B:#6E8B3D:#8B7D6B:#7FFF00:#CDBA96:#ADFF2F",
          "20","#66ff33","1","","","13","7")
      else ("#1E90FF:#E41A1C","1.5","#ffcc00","0.8",sam,"LOD","12","11")

    val elements= Json.obj("sam"->sam,"samnum"->samnum.toString,"cs"->cs,"ahpos"->ahpos,
      "ahcolo"->ahcolo,"ahsize"->ahsize,"dt"->"point","p"->"FALSE","yrmax"->"","yrmin"->"",
      "lts"->"14","xts"->"13","yts"->"12","xls"->"16","xtext"->xtext,"yls"->"16","ytext"->ytext,
      "ms"->"16","mstext"->"","width"->width,"height"->height,"dpi"->"300","ps"->"2","lbs"->"4",
      "lbscolo"->"#CA9197","lis"->"3").toString()

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"MHT","曼哈顿图（Manhattan）",input,"/",elements)
    //矩阵文件读取写入任务文件下table.txt
    file1.ref.moveTo(tableFile)

    Future{
      val command =
        if(samnum>1) "Rscript "+Utils.path+"R/manhadun/mahadun_plot.R -i "+ tableFile.getAbsolutePath + " -o " +dutyDir+"/out" + " -if pdf -in manhattan"
        else "Rscript "+Utils.path+"R/manhadun/mahadun_virtual_plot.R -i "+ tableFile.getAbsolutePath + " -o " +dutyDir+"/out" + " -if pdf -in manhattan"

      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      println(command)

      if (execCommand.isSuccess) {
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"")
        Utils.pdf2Png(dutyDir+"/out/manhattan.pdf",dutyDir+"/out/manhattan.png")
        val finish=dutyController.updateFini(id,data.taskname)
        Utils.pdf2Png(dutyDir+"/out/manhattan.pdf",dutyDir+"/out/manhattan.tiff")
        creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readManhattanData(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val elements=jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)

    val samnum=elements("samnum").toInt
    val group=if(samnum>1) elements("sam").split(",").toArray++Array("h") else Array("down","up")
    val color=elements("cs").split(":")

    //获取图片
    val pics=getReDrawPics(path)
    val (downpics,downpng,downtiffs)=(path+"/out/manhattan.pdf",path+"/out/manhattan.png",path+"/out/manhattan.tiff")
    Ok(Json.obj("group"->group,"sam"->elements("sam"),"samnum"->samnum,"pics"->pics,"elements"->elements,"color"->color))
  }

  case class ReManhattanData1(yrmin:String,yrmax:String,ahpos:String,dt:String,p:String,dpi:String,
                             width:String,height:String,color:String,ahcolo:String,lbscolo:String,
                             ps:String,lbs:String,ahsize:String,xts:String,xls:String,xtext:String,
                             yts:String,yls:String,ytext:String)

  val ReManhattanForm1: Form[ReManhattanData1] =Form(
    mapping (
      "yrmin"->text,
      "yrmax"->text,
      "ahpos"->text,
      "dt"->text,
      "p"->text,
      "dpi"->text,
      "width"->text,
      "height"->text,
      "color"->text,
      "ahcolo"->text,
      "lbscolo"->text,
      "ps"->text,
      "lbs"->text,
      "ahsize"->text,
      "xts"->text,
      "xls"->text,
      "xtext"->text,
      "yts"->text,
      "yls"->text,
      "ytext"->text
    )(ReManhattanData1.apply)(ReManhattanData1.unapply)
  )

  case class ReManhattanData2(lts:String,lis:String,ms:String,mstext:String)

  val ReManhattanForm2: Form[ReManhattanData2] =Form(
    mapping (
      "lts"->text,
      "lis"->text,
      "ms"->text,
      "mstext"->text
    )(ReManhattanData2.apply)(ReManhattanData2.unapply)
  )

  def redrawManhattan(taskname:String,samnum:Int,sam:String)=Action(parse.multipartFormData) { implicit request =>
    val data1=ReManhattanForm1.bindFromRequest.get
    val data2=ReManhattanForm2.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname

    val tableFile=new File(dutyDir,"table.txt")

    val elements= Json.obj("sam"->sam,"samnum"->samnum.toString,"cs"->data1.color,
      "ahpos"->data1.ahpos,"ahcolo"->data1.ahcolo,"ahsize"->data1.ahsize,"dt"->data1.dt,
      "p"->data1.p,"yrmax"->data1.yrmax,"yrmin"->data1.yrmin,"lts"->data2.lts,"xts"->data1.xts,
      "yts"->data1.yts,"xls"->data1.xls,"xtext"->data1.xtext,"yls"->data1.yls,"ytext"->data1.ytext,
      "ms"->data2.ms,"mstext"->data2.mstext,"width"->data1.width,"height"->data1.height,
      "dpi"->data1.dpi,"ps"->data1.ps,"lbs"->data1.lbs,"lbscolo"->data1.lbscolo,
      "lis"->data2.lis).toString()

    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    val yr=if(data1.yrmin=="" || data1.yrmax=="") "all" else data1.yrmin + ":" + data1.yrmax
    val xtext=if(data1.xtext=="") " " else data1.xtext
    val ytext=if(data1.ytext=="") " " else data1.ytext
    val mstext=if(data2.mstext=="") " " else data2.mstext

    val command = if(samnum>1)
      "Rscript " + Utils.path + "R/manhadun/mahadun_plot.R -i " + tableFile.getAbsolutePath +
        " -o " + dutyDir + "/out -cs \"" + data1.color + "\" -ah \"" + data1.ahpos + ":" + data1.ahcolo +
        ":" + data1.ahsize + "\" -dt " + data1.dt + " -p " + data1.p + " -yr " + yr + " -lts sans:plain:" +
        data2.lts + " -xts sans:bold.italic:" + data1.xts + " -yts sans:bold.italic:" + data1.yts +
        " -xls \"sans:bold.italic:" + data1.xls + ":" + xtext + ":black\"" + " -yls \"sans:bold.italic:" +
        data1.yls + ":" + ytext + ":black\"" + " -ms \"sans:bold.italic:" + data2.ms + ":" + mstext +
        ":black\"" + " -ls " + data1.width + ":" + data1.height + " -dpi " + data1.dpi + " -if pdf -in manhattan"
    else
      "Rscript " + Utils.path + "R/manhadun/mahadun_virtual_plot.R -i " + tableFile.getAbsolutePath +
        " -o " + dutyDir + "/out -cs \"" + data1.color + "\"" + " -ps " + data1.ps + " -ah \"" +
        data1.ahpos + ":" + data1.ahcolo + ":" + data1.ahsize + "\" -yr " + yr + " -lbs \"sans:plain:" +
        data1.lbs + ":" + data1.lbscolo + "\" -lis " + data2.lis + " -lts sans:plain:" + data2.lts +
        " -yts sans:bold.italic:" + data1.yts + " -xls \"sans:bold.italic:" + data1.xls + ":" +
        xtext + ":black\"" + " -yls \"sans:bold.italic:" + data1.yls + ":" + ytext + ":black\"" +
        " -ms \"sans:bold.italic:" + data2.ms + ":" + mstext + ":black\"" + " -ls " + data1.width +
        ":" + data1.height + " -dpi " + data1.dpi + " -if pdf -in manhattan"

    println(command)

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/manhattan.pdf",dutyDir+"/out/manhattan.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/manhattan.pdf",dutyDir+"/out/manhattan.tiff") //替换图片
      creatZip(dutyDir) //替换压缩文件包
      val pics=dutyDir+"/out/manhattan.png"
      val pdfs=dutyDir+"/out/manhattan.pdf"
      val tiffs=dutyDir+"/out/manhattan.tiff"
      Ok(Json.obj("valid"->"true","pics"->pics,"downpics"->pdfs,"downtiffs"->tiffs))
    } else {
      Ok(Json.obj("valid"->"false"))
    }

  }



  //Treemap
  def doTreemap(groups:String)=Action(parse.multipartFormData) { implicit request =>
    val data = TaxFunForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val tableFile=new File(dutyDir,"table.tre")
    val groupFile=new File(dutyDir,"group.txt")
    val file1 = request.body.file("table1").get
    file1.ref.moveTo(tableFile)
    val file2 = request.body.file("table2")
    val (input,group)=if(!file2.isEmpty) {
      file2.get.ref.moveTo(groupFile)
      (file1.filename+"/"+file2.get.filename," -g " + groupFile.getAbsolutePath)
    }else (file1.filename,"")

    val elements=Json.obj("groups"->groups,"width"->"20","height"->"23","dpi"->"300", "color"->"#000000:#3A89CC:#769C30:#CD0000:#D99536:#7B0078:#BFBC3B:#6E8B3D:#00688B:#C10077:#CAAA76:#474700:#458B00:#8B4513:#008B8B:#6E8B3D:#8B7D6B:#7FFF00:#CDBA96:#ADFF2F",
      "bw"->"2","fs"->"12","ss"->"1","lsa_width"->"2","lsa_height"->"1","lfs"->"26","ln"->"1","sl"->"TRUE",
      "ssb"->"FALSE").toString()

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"TRM","树状图",input,"/",elements)
    //矩阵文件读取写入任务文件下table.txt

    Future{
      val command = "Rscript " + Utils.path+"R/tree/tree2.0.R -i " + tableFile.getAbsolutePath + group +
        " -o " + dutyDir + "/out -if pdf -in treemap"
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (execCommand.isSuccess) {
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"")
        Utils.pdf2Png(dutyDir+"/out/treemap.pdf",dutyDir+"/out/treemap.png")
        val finish=dutyController.updateFini(id,data.taskname)
        Utils.pdf2Png(dutyDir+"/out/treemap.pdf",dutyDir+"/out/treemap.tiff")
        creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readTreemapData(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val elements=jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)
    val groupcolo=if(elements("groups")==",,") Array("0") else "0"+:elements("groups").split(",").toArray
    val color=elements("color").split(":")

    //获取图片
    val pics=getReDrawPics(path)
    Ok(Json.obj("pics"->pics,"elements"->elements,"groups"->elements("groups"),"groupcolo"->groupcolo,"color"->color))
  }

  case class ReTreemapData(ssb:String, sl:String, ln:String, lsa_width:String, lsa_height:String, dpi:String,
                           width:String, height:String, color:String, bw:String, lfs:String, fs:String, ss:String)

  val ReTreemapForm: Form[ReTreemapData] =Form(
    mapping (
      "ssb"->text,
      "sl"->text,
      "ln"->text,
      "lsa_width"->text,
      "lsa_height"->text,
      "dpi"->text,
      "width"->text,
      "height"->text,
      "color"->text,
      "bw"->text,
      "lfs"->text,
      "fs"->text,
      "ss"->text
    )(ReTreemapData.apply)(ReTreemapData.unapply)
  )

  def redrawTreemap(taskname:String,groups:String)=Action(parse.multipartFormData) { implicit request =>
    val data=ReTreemapForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname

    val tableFile=new File(dutyDir,"table.tre")
    val groupFile=new File(dutyDir,"group.txt")

    val elements=Json.obj("groups"->groups,"width"->data.width,"height"->data.height,"dpi"->data.dpi,
      "color"->data.color,"bw"->data.bw,"fs"->data.fs,"ss"->data.ss,"lsa_width"->data.lsa_width,
      "lsa_height"->data.lsa_height,"lfs"->data.lfs,"ln"->data.ln,"sl"->data.sl,"ssb"->data.ssb).toString()

    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    val group=if(groupFile.exists()) " -g " + groupFile.getAbsolutePath else ""

    val command = "Rscript " + Utils.path+"R/tree/tree2.0.R -i " + tableFile.getAbsolutePath + group +
      " -o " + dutyDir + "/out -if pdf -in treemap -is " + data.width + ":" + data.height + " -cs \"" +
      data.color + "\" -dpi " + data.dpi + " -bw " + data.bw + " -fs " + data.fs + " -ss " + data.ss +
      " -lsa " + data.lsa_width + ":" + data.lsa_height + " -lfs " + data.lfs + " -ln " + data.ln +
      " -sl " + data.sl + " -ssb " + data.ssb

    println(command)
    val execCommand = new ExecCommand
    execCommand.exect(command,dutyDir+"/temp")

    //先放入sh，在运行
//    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
//    val execCommand = new ExecCommand
//    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/treemap.pdf",dutyDir+"/out/treemap.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/treemap.pdf",dutyDir+"/out/treemap.tiff") //替换图片
      creatZip(dutyDir) //替换压缩文件包
      val pics=dutyDir+"/out/treemap.png"
      Ok(Json.obj("valid"->"true","pics"->pics))
    } else {
      Ok(Json.obj("valid"->"false","message"->execCommand.getErrStr))
    }

  }


  //Violin
  case class ViolinData(taskname:String,un:String,fp:String,txdata1:String,txdata2:String)

  val ViolinForm: Form[ViolinData] =Form(
    mapping (
      "taskname"->text,
      "un"->text,
      "fp"->text,
      "txdata1"->text,
      "txdata2"->text
    )(ViolinData.apply)(ViolinData.unapply)
  )

  def doViolin(isgroup:Boolean,table:String,group:String)=Action(parse.multipartFormData){implicit request=>
    val data=ViolinForm.bindFromRequest.get
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
        if(isgroup && group=="2") file1.filename+"/"+ request.body.file("table2").get.filename
        else file1.filename
      } else{
        FileUtils.writeStringToFile(tableFile, data.txdata1)
        if(isgroup && group=="2") request.body.file("table2").get.filename
        else "无"
      }

    val (unpara,un)=if(data.un=="") ("all","-1") else (data.un,data.un)

    val param=
      if(isgroup) ("是否分组绘图：" + isgroup + "/读取数据行数：" + unpara + "/图片展示方式：" + data.fp)
      else ("是否分组绘图：" + isgroup + "/读取数据行数：" + unpara + "/图片展示方式：" + data.fp)

    val elements= Json.obj("un"->un, "bw"->"0.1", "color"->"#B2182B:#E69F00:#56B4E9:#009E73:#F0E442:#0072B2:#D55E00:#CC79A7:#CC6666:#9999CC:#66CC99:#999999:#ADD1E5",
      "yrmin"->"", "yrmax"->"", "fp"->data.fp, "xts"->"18", "yts"->"16", "xls"->"20", "yls"->"20", "xtext"->"",
      "ytext"->"", "sts"->"16", "lts"->"14", "lms"->"15", "lmtext"->"", "dpi"->"300", "width" -> "20",
      "height"->"20").toString()

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"VIO","小提琴图",input,param,elements)

    Future{
      //是否有group文件
      val groupdata=
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

      val command = "Rscript "+Utils.path+"R/violin/violin.R -i "+ tableFile.getAbsolutePath + groupdata +
        " -o " +dutyDir+"/out" + " -sca TRUE"

      println(command)

      val execCommand = new ExecCommand
      //exec需要指定结果输出路径的时候，不指定默认本地任务路径
      execCommand.exect(command,dutyDir+"/temp")

      if (execCommand.isSuccess) {
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"")
        Utils.pdf2Png(dutyDir+"/out/violin.pdf",dutyDir+"/out/violin.png")
        val finish=dutyController.updateFini(id,data.taskname)
        Utils.pdf2Png(dutyDir+"/out/violin.pdf",dutyDir+"/out/violin.tiff")
        creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readViolinData(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val elements=jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)
    val color=elements("color").split(":")

    val head=FileUtils.readFileToString(new File(path+"/table.txt")).trim.split("\n")
    val gnum=head(0).trim.split("\t").drop(1).length
    //获取分组
    val group=
      if(new File(path+"/group.txt").exists()) {
        val f=FileUtils.readLines(new File(path+"/group.txt")).asScala
        val g=f.map{_.split('\t').head}.distinct.drop(1)
        g.toArray
      }else Array(",,")

    //获取图片
    val pics=getReDrawPics(path)
    Ok(Json.obj("group"->group,"pics"->pics,"elements"->elements,"color"->color))
  }

  case class ReViolinData(xdata:String,ydata:String,showname:String,showerro:String,color:String,
                       width:String, length:String,resolution:String,xts:String,yts:String,
                       xls:String,yls:String,lts:String,lms:String,lmtext:String,ms:String,
                       mstext:String,c:String)

  val ReViolinForm: Form[ReViolinData] =Form(
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
      "lms"->text,
      "lmtext"->text,
      "ms"->text,
      "mstext"->text,
      "c"->text
    )(ReViolinData.apply)(ReViolinData.unapply)
  )

  case class ReViolinData2(big:String,xdamin:String,xdamax:String,ydamin:String,ydamax:String)
  val ReViolinForm2: Form[ReViolinData2] =Form(
    mapping (
      "big"->text,
      "xdamin"->text,
      "xdamax"->text,
      "ydamin"->text,
      "ydamax"->text
    )(ReViolinData2.apply)(ReViolinData2.unapply)
  )

  def redrawViolin(taskname:String)=Action(parse.multipartFormData) { implicit request =>
    val data=RePCAForm.bindFromRequest.get
    val data2=RePCAForm2.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname
    val groupFile=new File(dutyDir,"group.txt")

    val elements= Json.obj("xdata"->data.xdata,"ydata"->data.ydata,"width"->"15","length"->"12",
      "showname"->data.showname,"showerro"->data.showerro,"color"->data.color,"resolution"->data.resolution,
      "xts"->data.xts,"yts"->data.yts,"xls"->data.xls,"yls"->data.yls,"lts"->data.lts,"lms"->data.lms,
      "lmtext"->data.lmtext,"ms"->data.ms,"mstext"->data.mstext,"c"->data.c,"big"->data2.big,
      "xdamin"->data2.xdamin,"xdamax"->data2.xdamax,"ydamin"->data2.ydamin,
      "ydamax"->data2.ydamax).toString()

    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    //读取log.txt 包括-g -b
    val log=FileUtils.readFileToString(new File(dutyDir+"/log.txt"))
    val c=
      if(!groupFile.exists()) " -oc \""+data.color+"\""
      else " -cs \""+ data.color+"\""

    val name=if(data.showname.equals("TRUE") && groupFile.exists()){
      val f=FileUtils.readLines(groupFile).asScala
      val n=f.map{_.split('\t').last}.distinct.drop(1).mkString(",")
      " -b " + n
    }else if(data.showname.equals("TRUE") && !groupFile.exists()){
      " -sl TRUE"
    } else ""
    val big=if(data2.big=="no") ""
    else if(data2.big=="x")
      " -da x:"+data2.xdamin+","+data2.xdamax
    else " -da y:"+data2.ydamin+","+data2.ydamax

    println(data.ydata)

    val lms=if(!data.lmtext.equals("")) " -lms sans:bold.italic:" + data.lms + ":\"" + data.lmtext+"\"" else ""
    val ms=if(!data.mstext.equals("")) " -ms sans:plain:" + data.ms + ":\"" + data.mstext+"\"" else ""
    val command = "Rscript "+Utils.path+"R/pca/pca_plot.R -i "+ dutyDir+"/out/pca.x.xls" + log +
      " -si " + dutyDir+"/out/pca.sdev.xls" + " -o " +dutyDir+"/out" + " -pxy "+ data.xdata+":"+ data.ydata +
      " -is "+ data.width + ":" + data.length + c + " -dpi " + data.resolution + " -xts " + "sans:plain:"+data.xts +
      " -yts " + "sans:plain:"+data.yts + " -xls " + "sans:plain:"+data.xls + " -yls " + "sans:plain:"+data.yls +
      " -lts " + "sans:plain:" + data.lts + name + " -if pdf -ss " + data.showerro + lms + ms +
      " -c " + data.c + big

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")
    //    execCommand.exect(command,dutyDir+"/temp")

    println(command)
    println(execCommand.getOutStr)
    println(execCommand.getErrStr)

    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.tiff") //替换图片
      creatZip(dutyDir) //替换压缩文件包
      val pics=dutyDir+"/out/pca.png"
      val pdfs=dutyDir+"/out/pca.pdf"
      val tiffs=dutyDir+"/out/pca.tiff"
      Ok(Json.obj("valid"->"true","pics"->pics,"downpics"->pdfs,"downtiffs"->tiffs))
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

  def creatZip(target:String,path:String): Unit ={
    new File(target).createNewFile()
    CompressUtil.zip(path,target)
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

  var IGC: Array[String] =Array("cor.xls","pvalue.xls","pandv.xls","p_star.xls","heatmap.pdf")
  var IGCins: Array[String] =Array("相关性系数矩阵","p值矩阵","相关性系数c值和p值分析结果","根据p值生成的星星矩阵","热图")

  var TAX: Array[String] =Array("ko_table.xls","kegg_L1.txt","kegg_L2.txt","kegg_L3.txt","kegg_enzyme.txt","kegg_pathway.txt","kegg_L1.pdf","pca.pdf")
  var TAXins: Array[String] =Array("kegg丰度表","kegg pathway 第一个层级丰度表","kegg pathway 第二个层级丰度表","kegg pathway 第三个层级丰度表","kegg enzyme丰度表","kegg pathway丰度表","箱线图","PCA图")

  var BAR: Array[String] =Array("bar_group.pdf")
  var BARins: Array[String] =Array("柱状图")

  var FAP: Array[String] =Array("functional_table.xls","functional_table.biom","outTables.zip","functional_otu.txt")
  var FAPins: Array[String] =Array("功能丰度表","biom格式的功能丰度表","每个功能相关的OTU的丰度表压缩包","与功能相关的otu列表，对outTables.zip中信息的综合统计")

  var PIC: Array[String] = Array("out.tre",
    "marker_predicted_and_nsti.tsv.gz",
    "pathways_out.zip",
    "EC_predicted.tsv.gz",
    "EC_metagenome_out.zip",
    "COG_predicted.tsv.gz",
    "COG_metagenome_out.zip",
    "KO_predicted.tsv.gz",
    "KO_metagenome_out.zip",
    "PFAM_predicted.tsv.gz",
    "PFAM_metagenome_out.zip",
    "TIGRFAM_predicted.tsv.gz",
    "TIGRFAM_metagenome_out.zip"
  )
  var PICins: Array[String] = Array("参考序列的树文件",
    "16S预测的拷贝数和NSTI",
    "压缩文件包括预测的通路丰度",
    "每个ASV/OTU中预测的EC数量",
    "EC结果,包括：预测EC数量(pred_metagenome_unstrat.tsv)、基于预测16S拷贝数校正的特征表(seqtab_norm.tsv)、每个样本的NSTI权重(weighted_nsti.tsv)、每个样本的功能对应每个物种的数量，选择在各层级产生分层时生成(pred_metagenome_contrib.tsv)",
    "每个ASV/OTU中预测的COG数量",
    "COG结果,包括：预测COG数量(pred_metagenome_unstrat.tsv)、基于预测16S拷贝数校正的特征表(seqtab_norm.tsv)、每个样本的NSTI权重(weighted_nsti.tsv)、每个样本的功能对应每个物种的数量，选择在各层级产生分层时生成(pred_metagenome_contrib.tsv)",
    "每个ASV/OTU中预测的KO数量",
    "KO结果,包括：预测KO数量(pred_metagenome_unstrat.tsv)、基于预测16S拷贝数校正的特征表(seqtab_norm.tsv)、每个样本的NSTI权重(weighted_nsti.tsv)、每个样本的功能对应每个物种的数量，选择在各层级产生分层时生成(pred_metagenome_contrib.tsv)",
    "每个ASV/OTU中预测的PFAM数量",
    "PFAM结果,包括：预测PFAM数量(pred_metagenome_unstrat.tsv)、基于预测16S拷贝数校正的特征表(seqtab_norm.tsv)、每个样本的NSTI权重(weighted_nsti.tsv)、每个样本的功能对应每个物种的数量，选择在各层级产生分层时生成(pred_metagenome_contrib.tsv)",
    "每个ASV/OTU中预测的TIGRFAM数量",
    "TIGRFAM结果,包括：预测TIGRFAM数量(pred_metagenome_unstrat.tsv)、基于预测16S拷贝数校正的特征表(seqtab_norm.tsv)、每个样本的NSTI权重(weighted_nsti.tsv)、每个样本的功能对应每个物种的数量，选择在各层级产生分层时生成(pred_metagenome_contrib.tsv)"
  )

  var LEF: Array[String] =Array("lefse_LDA.xls","lefse_LDA.pdf","lefse_LDA.cladogram.pdf")
  var LEFins: Array[String] =Array("LDA判别分析结果","LDA分析柱图","进化分支图")

  var PMR: Array[String] =Array("sequence.eprimer32")
  var PMRins: Array[String] =Array("eprimer32结果序列")

  var VOC: Array[String] =Array("volcano.pdf")
  var VOCins: Array[String] =Array("火山图")

  var MHT: Array[String] =Array("manhattan.pdf")
  var MHTins: Array[String] =Array("曼哈顿图")

  var TRM: Array[String] =Array("treemap.pdf")
  var TRMins: Array[String] =Array("树状图")

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
      else if(soft=="IGC") (IGC,IGCins)
      else if(soft=="TAX") (TAX,TAXins)
      else if(soft=="BAR") (BAR,BARins)
      else if(soft=="FAP") (FAP,FAPins)
      else if(soft=="PIC") (PIC,PICins)
      else if(soft=="LEF") {
        val path=Utils.path+"/users/"+id+"/"+taskname
        if(new File(path+"/out/lefse_LDA.png").length()==0)
          (Array("lefse_LDA.xls","lefse_LDA.cladogram.pdf"),Array("LDA判别分析结果","进化分支图"))
        else (LEF,LEFins)
      }
      else if(soft=="ABI"){
        val path=Utils.path+"/users/"+id+"/"+taskname
        val seq=new File(path+"/out").listFiles().filter(_.getName.contains("sequence")).map(_.getName).head
        val seqins="序列文件"
        val pics=new File(path+"/out").listFiles().filter(x=>x.getName.contains("abiview")&&(!x.getName.contains("pdf"))).map(_.getName)
        val out=
          if(pics.length==1) "abiview.ps"
          else pics.filter(!_.contains("ps")).head
        val outins="abiview结果图"
        (Array(seq,out,"abiview.pdf"),Array(seqins,outins,"abiview结果图pdf文件"))
      }
      else if(soft=="RSQ" || soft=="GTF"){
        val path=Utils.path+"/users/"+id+"/"+taskname
        val seq=new File(path+"/out").listFiles().filter(_.getName.contains("sequence")).map(_.getName).head
        val seqins="输出序列文件"
        (Array(seq),Array(seqins))
      }
      else if(soft=="PMR") (PMR,PMRins)
      else if(soft=="VOC") (VOC,VOCins)
      else if(soft=="MHT") (MHT,MHTins)
      else if(soft=="TRM") (TRM,TRMins)
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

  def download(taskname:String,picname:String,suffix:String,num:Double): Action[AnyContent] = Action{ implicit request=>
    val file=new File(userDutyDir+request.session.get("userId").get+"/"+taskname+"/out/"+picname+"."+suffix)
    Ok.sendFile(file).withHeaders(
      //缓存
      CACHE_CONTROL -> "max-age=3600",
      CONTENT_DISPOSITION -> ("attachment; filename=" + file.getName),
      CONTENT_TYPE -> "application/x-download"
    )
  }

  def downloadFile(taskname:String,filename:String,num:Double): Action[AnyContent] =Action{ implicit request=>
    val path=new File(userDutyDir+request.session.get("userId").get+"/"+taskname+"/out/"+filename)
    Ok.sendFile(path).withHeaders(
      //缓存
      CACHE_CONTROL -> "max-age=3600",
      CONTENT_DISPOSITION -> ("attachment; filename=" + path.getName),
      CONTENT_TYPE -> "application/x-download"
    )
  }

  def downloadZip(taskname:String,num:Double): Action[AnyContent] =Action{ implicit request=>
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

  def getPdf(fileUrl:String): Action[AnyContent] = Action{ implicit request=>
    SendPdf(new File(fileUrl),request.headers)
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

  def SendPdf(file: File,headers:Headers): Result = {
    val lastModifiedStr = file.lastModified().toString
    val byteArray = Files.readAllBytes(file.toPath)
    val ifModifiedSinceStr = headers.get(IF_MODIFIED_SINCE)
    if (ifModifiedSinceStr.isDefined && ifModifiedSinceStr.get == lastModifiedStr) {
      NotModified
    } else {
      Ok(byteArray).withHeaders(LAST_MODIFIED -> file.lastModified().toString)
    }
  }

  def jsonToMap(json:String): Map[String, String] = {
    scala.util.parsing.json.JSON.parseFull(json).get.asInstanceOf[Map[String, String]]
  }

  def mapToJson(map:Map[String,String]): String = {
    Json.toJson(map).toString()
  }

}
