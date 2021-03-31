package controllers

import java.io.{File, FileInputStream}
import java.nio.file.{Files, Path}

import dao.dutyDao
import javax.inject.Inject
import jdk.nashorn.internal.objects.Global
import org.apache.commons.io.FileUtils
import org.json.JSONObject
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
import scala.util.parsing.json
import scala.util.parsing.json.JSONObject


class RFunctionController @Inject()(cc: ControllerComponents, dutydao: dutyDao, rservice: RService, dutyController: DutyController)(implicit exec: ExecutionContext) extends AbstractController(cc) with MyStringTool {

  val userDutyDir: String =Utils.path+"users/"

  case class TaxFunData(taskname:String)

  val TaxFunForm: Form[TaxFunData] =Form(
    mapping (
      "taskname"->text
    )(TaxFunData.apply)(TaxFunData.unapply)
  )


  //GO && KEGG
//  case class GoData(taskname:String,species:String,txdata1:String)
//
//  val GoForm: Form[GoData] =Form(
//    mapping (
//      "taskname"->text,
//      "species"->text,
//      "txdata1"->text
//    )(GoData.apply)(GoData.unapply)
//  )
//
//  def doGo(types:String,refer:String,table:String)=Action(parse.multipartFormData){implicit request=>
//    val data=GoForm.bindFromRequest.get
//    val id=request.session.get("userId").get
//    val dutyDir=rservice.creatUserDir(id,data.taskname)
//
//    val tableFile=new File(dutyDir,"table.txt")
//    val koFile=new File(dutyDir,"kogo.txt")
//    val input=
//      if(table=="2"){
//        val file1=request.body.file("table1").get
//        rservice.fileTrimMove(file1.ref, tableFile)
////        file1.ref.moveTo(tableFile)
//        if(refer=="TRUE"){
//          file1.filename
//        }else{
//          val file2=request.body.file("table2").get
//          rservice.fileTrimMove(file2.ref, koFile)
////          file2.ref.moveTo(koFile)
//          file1.filename+"/"+file2.filename
//        }
//      } else {
//        FileUtils.writeStringToFile(tableFile, data.txdata1)
//        if(refer=="TRUE"){
//          "无"
//        }else{
//          val file3=request.body.file("table2").get
//          rservice.fileTrimMove(file3.ref, koFile)
////          file3.ref.moveTo(koFile)
//          file3.filename
//        }
//      }
//
//    val (param,model)=
//      if(refer=="TRUE") {
//        if(types=="ko") ("使用已有参考:"+refer+"/选择物种："+data.species,"ko_data.jar -m "+data.species)
//        else ("使用已有参考:"+refer+"/选择物种："+data.species,"go_data.jar -m "+data.species)
//      }
//      else {
//        if(types=="ko") ("使用已有参考:"+refer,"ko_data_file.jar -pathway "+koFile.getAbsolutePath)
//        else ("使用已有参考:"+refer,"Go_data_file.jar -go "+koFile.getAbsolutePath)
//      }
//
//    val elements= Json.obj("n"->"15","br"->"0.9","g"->"FALSE","sm"->"50","width"->"20",
//      "height"->"14", "xts"->"13","yts"->"14","lts"->"15","dpi"->"300").toString()
//
//    Future{
//      val (command1,command2,start)=
//        if(types=="ko")
//          ("java -jar " + Utils.path + "R/gokegg/data/" + model + " -i " + tableFile.getAbsolutePath +
//            " -o " + dutyDir + "/out/ko" , "Rscript " + Utils.path + "R/gokegg/plot/Ko_dodge_plot.R -i " + dutyDir +
//            "/out/ko.Ko.bar.dat" + " -o " + dutyDir + "/out" + " -in ko_dodge -if pdf -sm 50 -n 15",
//            dutyController.insertDuty(data.taskname,id,"KEGG","KEGG富集分析",input,param,elements))
//        else
//          ("java -jar " + Utils.path + "R/gokegg/data/" + model + " -i " + tableFile.getAbsolutePath +
//            " -o " + dutyDir + "/out/go" , "Rscript " + Utils.path + "R/gokegg/plot/Go_stack_plot.R -i " + dutyDir +
//            "/out/go.Go.bar.dat" + " -o " + dutyDir + "/out" + " -in go_stack -if pdf -sm 50 -n 15",
//            dutyController.insertDuty(data.taskname,id,"GO","GO富集分析",input,param,elements))
//      val command3 = "dos2unix " + tableFile.getAbsolutePath
//      val command4 = "dos2unix " + koFile.getAbsolutePath
//      val commandpack1= if(koFile.exists()) Array(command3,command4) else Array(command3)
//      val commandpack2=Array(command1,command2)
//      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),commandpack1.mkString(" && \n")+" && \n"+commandpack2.mkString(" && \n"))
//
//      //      val execCommand1 = new ExecCommand
//      //      execCommand1.exec(commandpack1)
//
//      val execCommand = new ExecCommand
//      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")
//
//      if(execCommand.isSuccess){
//        //        FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),commandpack1.mkString(" && \n")+" && \n"+commandpack2.mkString(" && \n"))
//        println(command1)
//        println(command2)
//
//        if(types=="ko"){
//          Utils.pdf2Png(dutyDir+"/out/ko.Ko.enrich.pdf",dutyDir+"/out/ko.Ko.enrich.png")
//          Utils.pdf2Png(dutyDir+"/out/ko.Ko.enrich.pdf",dutyDir+"/out/ko.Ko.enrich.tiff")
//          Utils.pdf2Png(dutyDir+"/out/ko_dodge.pdf",dutyDir+"/out/ko_dodge.png")
//          Utils.pdf2Png(dutyDir+"/out/ko_dodge.pdf",dutyDir+"/out/ko_dodge.tiff")
//        }else {
//          Utils.pdf2Png(dutyDir+"/out/go.Go.enrich.pdf",dutyDir+"/out/go.Go.enrich.png")
//          Utils.pdf2Png(dutyDir+"/out/go.Go.enrich.pdf",dutyDir+"/out/go.Go.enrich.tiff")
//          Utils.pdf2Png(dutyDir+"/out/go_stack.pdf",dutyDir+"/out/go_stack.png")
//          Utils.pdf2Png(dutyDir+"/out/go_stack.pdf",dutyDir+"/out/go_stack.tiff")
//        }
//        rservice.creatZip(dutyDir)
//        val finish=dutyController.updateFini(id,data.taskname)
//        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\nFinish Time:"+finish+"\n\n运行成功！")
//      }else{
//        dutydao.updateFailed(id,data.taskname)
//        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
//      }
//    }
//    Ok(Json.obj("valid" -> "运行中！"))
//  }
//
//  def readGoData(types:String,taskname:String): Action[AnyContent] =Action{ implicit request=>
//    val id=request.session.get("userId").get
//    val path=Utils.path+"/users/"+id+"/"+taskname
//
//    val elements=rservice.jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)
//    val (groupname,groupcolor) = FileUtils.readLines(new File(path+"/out/color.xls")).asScala.map{line=>
//      (line.split("\t")(0).replaceAll("\"",""),
//        line.split("\t")(1).replaceAll("\"",""))
//    }.unzip
//
//    val pics= if(types=="ko") (path+"/out/ko_dodge.png",path+"/out/ko.Ko.enrich.png") else (path+"/out/go_stack.png",path+"/out/go.Go.enrich.png")
//    Ok(Json.obj("pics"->pics,"elements"->elements,"groupname"->groupname.tail,"groupcolor"->groupcolor.tail))
//  }
//
//  case class ReGoData(g:String,n:String,sm:String,br:String,color:String,width:String,height:String,dpi:String,xts:String,yts:String,lts:String)
//
//  val ReGoForm: Form[ReGoData] =Form(
//    mapping (
//      "g"->text,
//      "n"->text,
//      "sm"->text,
//      "br"->text,
//      "color"->text,
//      "width"->text,
//      "height"->text,
//      "dpi"->text,
//      "xts"->text,
//      "yts"->text,
//      "lts"->text
//    )(ReGoData.apply)(ReGoData.unapply)
//  )
//
//  def redrawGO(taskname:String,types:String)=Action(parse.multipartFormData) { implicit request =>
//    val data=ReGoForm.bindFromRequest.get
//    val id=request.session.get("userId").get
//    val dutyDir=Utils.path+"users/"+id+"/"+taskname
//
//    val (r,i,g,in)=
//      if(types=="ko")
//        ("R/gokegg/plot/Ko_dodge_plot.R",dutyDir + "/out/ko.Ko.bar.dat"," -g " + data.g,"ko_dodge")
//      else ("R/gokegg/plot/Go_stack_plot.R",dutyDir + "/out/go.Go.bar.dat","","go_stack")
//
//    val elements= Json.obj("n"->data.n,"br"->data.br,"g"->data.g,"sm"->data.sm,"width"->data.width,
//      "height"->data.height,"xts"->data.xts,"yts"->data.yts,"lts"->data.lts,"dpi"->data.dpi).toString()
//    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)
//
//    val cs=data.color+":#E41A1C:#FFC0CB:#1E90FF:#00BFFF:#FF8C00:#FFDEAD:#4DAF4A:#90EE90:#9692C3:#CDB4FF:#40E0D0:#00FFFF"
//
//    val command = "Rscript " + Utils.path + r + " -i "+ i + " -o " + dutyDir + "/out -n " + data.n + " -sm " +
//      data.sm + " -br " + data.br + " -cs " + cs + " -is " + data.width + ":" + data.height + " -dpi " +
//      data.dpi + " -xts sans:bold.italic:" + data.xts + " -yts sans:bold.italic:" + data.yts + " -lts sans:bold.italic:" +
//      data.lts + " -in " + in + g + " -if pdf"
//
//    println(command)
//    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
//    val execCommand = new ExecCommand
//    execCommand.exect(command,dutyDir+"/temp")
//
//    if (execCommand.isSuccess) {
//      val (groupname,groupcolor) = FileUtils.readLines(new File(dutyDir + "/out/color.xls")).asScala.map{line=>
//        (line.split("\t")(0).replaceAll("\"",""),line.split("\t")(1).replaceAll("\"",""))
//      }.unzip
//      Utils.pdf2Png(dutyDir+"/out/"+in+".pdf",dutyDir+"/out/"+in+".png") //替换图片
//      Utils.pdf2Png(dutyDir+"/out/"+in+".pdf",dutyDir+"/out/"+in+".tiff") //替换图片
//      rservice.creatZip(dutyDir) //替换压缩文件包
//      val pics=dutyDir+"/out/"+in+".png"
//      Ok(Json.obj("valid"->"true","pics"->pics,"groupname"->groupname.tail,"groupcolor"->groupcolor.tail))
//    } else {
//      Ok(Json.obj("valid"->"false", "message"->execCommand.getErrStr))
//    }
//  }






}
