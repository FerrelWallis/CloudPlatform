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

class RClusterController @Inject()(cc: ControllerComponents, dutydao: dutyDao, rservice: RService, dutyController: DutyController)(implicit exec: ExecutionContext) extends AbstractController(cc) with MyStringTool {

  val userDutyDir: String =Utils.path+"users/"

  case class TaxFunData(taskname:String)

  val TaxFunForm: Form[TaxFunData] =Form(
    mapping (
      "taskname"->text
    )(TaxFunData.apply)(TaxFunData.unapply)
  )


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

  def doPCA(isgroup:Boolean,table:String,group:String,abbre:String)=Action(parse.multipartFormData){implicit request=>
    val data=PCA1Form.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=rservice.creatUserDir(id,data.taskname)
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

    val (xdata,ydata,sabbrename,sname)=if(abbre=="PCA") ("PC1","PC2","PCA","主成分分析（PCA）") else ("PCOA1","PCOA2","PCO","PCoA")

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

    val elements= Json.obj("xdata"->xdata,"ydata"->ydata,"width"->"15","length"->"12","showname"->data.showname,
      "showerro"->data.showerro,"color"->co,"resolution"->"300","xts"->"15","yts"->"15","xls"->"17","yls"->"17",
      "lts"->"14","lms"->"15","lmtext"->"","ms"->"17","mstext"->"","c"->"FALSE","big"->"no",
      "xdamin"->"","xdamax"->"","ydamin"->"","ydamax"->"").toString()

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,sabbrename,sname,input,param,elements)

    Future{
      val datascript=if(abbre=="PCA") "R/pca/pca_data.R" else "R/pcoa/pcoa-data.R"
      val command1 = "Rscript " + Utils.path + datascript + " -i " + tableFile.getAbsolutePath +
        " -o " + dutyDir + "/out" + " -sca TRUE"

      println(command1)

      val execCommand1 = new ExecCommand
      //exec需要指定结果输出路径的时候，不指定默认本地任务路径
      execCommand1.exect(command1,dutyDir+"/temp")

      val plotscript=
        if(abbre=="PCA") "R/pca/pca_plot.R -i " + dutyDir+"/out/pca.x.xls" + " -si " + dutyDir + "/out/pca.sdev.xls"
        else "R/pcoa/pcoa-plot.R -i " + dutyDir+"/out/PCOA.x.xls" + " -si " + dutyDir + "/out/PCOA.sdev.xls"

      val command2 = "Rscript " + Utils.path + plotscript + groupdata + " -o " +dutyDir+"/out" + name +
        " -if pdf -ss " + data.showerro + " -sl " + data.showname
      println(command2)

      val execCommand2 = new ExecCommand
      execCommand2.exect(command2,dutyDir+"/temp")

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command1+" && \n"+command2)

      if (execCommand1.isSuccess && execCommand2.isSuccess) {
        Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.png")
        val finish=dutyController.updateFini(id,data.taskname)
        Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.tiff")
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\nFinish Time:"+finish+"\n\n运行成功！")
        rservice.creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand1.getErrStr+"\n\n"+execCommand2.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readPCAData(taskname:String,abbre:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val elements=rservice.jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)
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
    val filepath=if(abbre=="PCA") path+"/out/pca.x.xls" else path+"/out/PCOA.x.xls"
    val data=FileUtils.readLines(new File(filepath))
    val col=data.get(0).split("\"").filter(_.trim!="").map(_.trim)
    //获取图片
    val pics=rservice.getReDrawPics(path)
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

  def redrawPCA(taskname:String,abbre:String)=Action(parse.multipartFormData) { implicit request =>
    val data=RePCAForm.bindFromRequest.get
    val data2=RePCAForm2.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname
    val groupFile=new File(dutyDir,"group.txt")

    //    val ele=jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)
    //    val groupdata=ele("groupdata")
    val groupdata=if(groupFile.exists()) " -g " + groupFile.getAbsolutePath else ""

    val elements= Json.obj("xdata"->data.xdata,"ydata"->data.ydata,"width"->"15","length"->"12",
      "showname"->data.showname,"showerro"->data.showerro,"color"->data.color,"resolution"->data.resolution,
      "xts"->data.xts,"yts"->data.yts,"xls"->data.xls,"yls"->data.yls,"lts"->data.lts,"lms"->data.lms,
      "lmtext"->data.lmtext,"ms"->data.ms,"mstext"->data.mstext,"c"->data.c,"big"->data2.big,
      "xdamin"->data2.xdamin,"xdamax"->data2.xdamax,"ydamin"->data2.ydamin,
      "ydamax"->data2.ydamax).toString()

    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    //    val log=FileUtils.readFileToString(new File(dutyDir+"/log.txt"))
    val c=
      if(!groupFile.exists()) " -oc \""+data.color+"\""
      else " -cs \""+ data.color+"\""

    //    val name=if(data.showname.equals("TRUE") && groupFile.exists()){
    //      val f=FileUtils.readLines(groupFile).asScala
    //      val n=f.map{_.split('\t').last}.distinct.drop(1).mkString(",")
    //      " -b " + n
    //    }else if(data.showname.equals("TRUE") && !groupFile.exists()){
    //      " -sl TRUE"
    //    } else ""

    val name=if(data.showname.equals("TRUE") && groupFile.exists()){
      val f=FileUtils.readLines(groupFile).asScala
      val n=f.map{_.split('\t').last}.distinct.drop(1).mkString(",")
      " -b " + n
    }else if(data.showname.equals("TRUE") && !groupFile.exists()){
      " -sl TRUE"
    }
    else ""

    val big=if(data2.big=="no") ""
    else if(data2.big=="x")
      " -da x:"+data2.xdamin+","+data2.xdamax
    else " -da y:"+data2.ydamin+","+data2.ydamax

    println(data.ydata)

    val lms=if(!data.lmtext.equals("")) " -lms sans:bold.italic:" + data.lms + ":\"" + data.lmtext+"\"" else ""
    val ms=if(!data.mstext.equals("")) " -ms sans:plain:" + data.ms + ":\"" + data.mstext+"\"" else ""

    val script=
      if(abbre=="PCA") "R/pca/pca_plot.R" + " -i " + dutyDir + "/out/pca.x.xls" + " -si " + dutyDir+"/out/pca.sdev.xls"
      else "R/pcoa/pcoa-plot.R" + " -i " + dutyDir + "/out/PCOA.x.xls" + " -si " + dutyDir+"/out/PCOA.sdev.xls"

    val command = "Rscript " + Utils.path + script + groupdata + " -o " +dutyDir+"/out" + " -pxy "+ data.xdata +
      ":" + data.ydata + " -is "+ data.width + ":" + data.length + c + " -dpi " + data.resolution + " -xts " +
      "sans:plain:"+data.xts + " -yts " + "sans:plain:"+data.yts + " -xls " + "sans:plain:"+data.xls + " -yls " +
      "sans:plain:"+data.yls + " -lts " + "sans:plain:" + data.lts + " -if pdf -ss " + data.showerro + name +
      lms + ms + " -c " + data.c + big

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    //    val execCommand = new ExecCommand
    //    execCommand.exect(command,dutyDir+"/temp")

    println(command)
    println(execCommand.getOutStr)
    println(execCommand.getErrStr)

    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.tiff") //替换图片
      rservice.creatZip(dutyDir) //替换压缩文件包
      val pics=dutyDir+"/out/pca.png"
      val pdfs=dutyDir+"/out/pca.pdf"
      val tiffs=dutyDir+"/out/pca.tiff"
      Ok(Json.obj("valid"->"true","pics"->pics,"downpics"->pdfs,"downtiffs"->tiffs))
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
    val dutyDir=rservice.creatUserDir(id,data.taskname)
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

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command1+" && \n"+command2)
      val execCommand2 = new ExecCommand
      execCommand2.exect(command2,dutyDir+"/temp")

      if (execCommand1.isSuccess&&execCommand2.isSuccess) {
        Utils.pdf2Png(dutyDir+"/out/rdacca.pdf",dutyDir+"/out/rdacca.png")
        val finish=dutyController.updateFini(id,data.taskname)
        Utils.pdf2Png(dutyDir+"/out/rdacca.pdf",dutyDir+"/out/rdacca.tiff")
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\nFinish Time:"+finish+"\n\n运行成功！")
        rservice.creatZip(dutyDir)
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

    val elements=rservice.jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)

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
    val pics=rservice.getReDrawPics(path)
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
      rservice.creatZip(dutyDir) //替换压缩文件包
      val pics=dutyDir+"/out/rdacca.png"
      val pdfs=dutyDir+"/out/rdacca.pdf"
      val tiffs=dutyDir+"/out/rdacca.tiff"
      Ok(Json.obj("valid"->"true","pics"->pics,"downpics"->pdfs,"downtiffs"->tiffs))
    } else {
      Ok(Json.obj("valid"->"false"))
    }
  }




  //nmds
  case class nmdsData(taskname:String,dim:String)

  val nmdsForm: Form[nmdsData] =Form(
    mapping (
      "taskname"->text,
      "dim"->text
    )(nmdsData.apply)(nmdsData.unapply)
  )

  def doNmds(isgroup:Boolean)=Action(parse.multipartFormData){implicit request=>
    val data=nmdsForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=rservice.creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")
    val file1 = request.body.file("table1").get
    file1.ref.moveTo(tableFile)
    val file2 = request.body.file("table2")
    val (group,filepara)=if(isgroup) {
      file2.get.ref.moveTo(groupFile)
      (" -g " + groupFile.getAbsolutePath,file1.filename+"/"+file2.get.filename)
    } else ("",file1.filename)

    val param = "NMDS空间维度：" + data.dim
    val co = if(isgroup)
      "#CD0000:#3A89CC:#769C30:#D99536:#7B0078:#BFBC3B:#E2609F:#00688B:#C10077:#CAAA76:#EEEE00:#458B00:#8B4513:#008B8B:#6E8B3D:#8B7D6B:#7FFF00:#CDBA96:#ADFF2F"
      else "#48FF75"

    val elements= Json.obj("width"->"15","length"->"12","showname"->"FALSE",
      "showerro"->"FALSE","color"->co,"resolution"->"300","xts"->"15","yts"->"15","xls"->"17","yls"->"17",
      "lts"->"14","lms"->"15","lmtext"->"","ms"->"17","mstext"->"","c"->"FALSE","big"->"no",
      "xdamin"->"","xdamax"->"","ydamin"->"","ydamax"->"").toString()

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"NMD","NMDS",filepara,param,elements)

    Future{
      val command = "Rscript " + Utils.path + "R/nmds/nmds.R -i " + tableFile.getAbsolutePath +
        " -o " + dutyDir + "/out -dim " + data.dim + " && \n Rscript " + Utils.path + "R/pcoa/pcoa-plot.R -i " + dutyDir + "/out/nmds_sites.xls" + group +
        " -o " +dutyDir+"/out" + " -if pdf -in nmds -pxy MDS1:MDS2"
      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

      if (execCommand.isSuccess) {
        Utils.pdf2Png(dutyDir+"/out/nmds.pdf",dutyDir+"/out/nmds.png")
        val finish=dutyController.updateFini(id,data.taskname)
        Utils.pdf2Png(dutyDir+"/out/nmds.pdf",dutyDir+"/out/nmds.tiff")
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\nFinish Time:"+finish+"\n\n运行成功！")
        rservice.creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readNmdsData(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val elements=rservice.jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)
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

    //获取图片
    val pics=rservice.getReDrawPics(path)
    Ok(Json.obj("group"->group,"pics"->pics,"elements"->elements,"color"->color))
  }

  case class ReNmdsData(showname:String,showerro:String,color:String, width:String, length:String,
                        resolution:String,xts:String,yts:String, xls:String,yls:String,lts:String,
                        lms:String,lmtext:String,ms:String, mstext:String,c:String,big:String,
                        xdamin:String,xdamax:String,ydamin:String,ydamax:String)

  val ReNmdsForm: Form[ReNmdsData] =Form(
    mapping (
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
      "c"->text,
      "big"->text,
      "xdamin"->text,
      "xdamax"->text,
      "ydamin"->text,
      "ydamax"->text
    )(ReNmdsData.apply)(ReNmdsData.unapply)
  )

  def redrawNmds(taskname:String)=Action(parse.multipartFormData) { implicit request =>
    val data=ReNmdsForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname
    val groupFile=new File(dutyDir,"group.txt")

    val groupdata=if(groupFile.exists()) " -g " + groupFile.getAbsolutePath else ""

    val elements= Json.obj("width"->"15","length"->"12","showname"->data.showname,
      "showerro"->data.showerro,"color"->data.color,"resolution"->data.resolution,
      "xts"->data.xts,"yts"->data.yts,"xls"->data.xls,"yls"->data.yls,"lts"->data.lts,
      "lms"->data.lms,"lmtext"->data.lmtext,"ms"->data.ms,"mstext"->data.mstext,"c"->data.c,
      "big"->data.big,"xdamin"->data.xdamin,"xdamax"->data.xdamax,"ydamin"->data.ydamin,
      "ydamax"->data.ydamax).toString()

    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    val c=
      if(!groupFile.exists()) " -oc \""+data.color+"\""
      else " -cs \""+ data.color+"\""

    val name=if(data.showname.equals("TRUE") && groupFile.exists()){
      val f=FileUtils.readLines(groupFile).asScala
      val n=f.map{_.split('\t').last}.distinct.drop(1).mkString(",")
      " -b " + n
    }else if(data.showname.equals("TRUE") && !groupFile.exists()){
      " -sl TRUE"
    }
    else ""

    val big=if(data.big=="no") ""
    else if(data.big=="x")
      " -da x:"+data.xdamin+","+data.xdamax
    else " -da y:"+data.ydamin+","+data.ydamax

    val lms=if(!data.lmtext.equals("")) " -lms sans:bold.italic:" + data.lms + ":\"" + data.lmtext+"\"" else ""
    val ms=if(!data.mstext.equals("")) " -ms sans:plain:" + data.ms + ":\"" + data.mstext+"\"" else ""

    val command = "Rscript " + Utils.path + "R/pcoa/pcoa-plot.R -i " + dutyDir + "/out/nmds_sites.xls" +
      groupdata + " -o " + dutyDir + "/out" + " -pxy MDS1:MDS2 -is "+ data.width + ":" + data.length + c +
      " -dpi " + data.resolution + " -xts " + "sans:plain:" + data.xts + " -yts " + "sans:plain:" + data.yts +
      " -xls " + "sans:plain:" + data.xls + " -yls " + "sans:plain:" + data.yls + " -lts " + "sans:plain:" +
      data.lts + " -if pdf -ss " + data.showerro + name + lms + ms + " -c " + data.c + big + " -in nmds"

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    println(command)

    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/nmds.pdf",dutyDir+"/out/nmds.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/nmds.pdf",dutyDir+"/out/nmds.tiff") //替换图片
      rservice.creatZip(dutyDir) //替换压缩文件包
      val pics=dutyDir+"/out/nmds.png"
      Ok(Json.obj("valid"->"true","pics"->pics))
    } else {
      Ok(Json.obj("valid"->"false"))
    }
  }




}
