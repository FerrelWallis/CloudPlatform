package controllers

import java.io.{File, FileInputStream}
import java.nio.file.{Files, Path}

import dao.{downfileDao, dutyDao}
import javax.inject.Inject
import org.apache.commons.io.FileUtils
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Headers, Result}
import utils.{CompressUtil, ExecCommand, MyStringTool, Utils}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.collection.JavaConverters._
import utils.Implicits._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class RService @Inject()(cc: ControllerComponents,dutydao:dutyDao,dutyController: DutyController, downfilesDao:downfileDao)(implicit exec: ExecutionContext) extends AbstractController(cc) with MyStringTool{

  val userDutyDir: String =Utils.path+"users/"

  def runSoft(abbre:String, sname:String) = Action(parse.multipartFormData) { implicit request =>
    val params = request.body.asFormUrlEncoded.map { case (key, value) => key -> value.mkString(";")}
    val id=request.session.get("userId").get
    val taskname = params("taskname")
    val dutyDir = creatUserDir(id, taskname)
    val start=dutyController.insertDuty(taskname,id,abbre,sname,"/","/","/")
    Future {
      val result = abbre match {
        case "PCA" => tools.pca.Run(dutyDir, params)
        case "ADB" => tools.boxDiversity.Run(dutyDir, params)
        case "MLN" => tools.multiLinear.Run(dutyDir, params)
        case "PMR" => tools.eprimer.Run(dutyDir, params)
        case "GTF" => tools.getorf.Run(dutyDir, params)
        case "PIC" => tools.picrust.Run(dutyDir, params)
        case "FAP" => tools.faprotax.Run(dutyDir, params)
        case "TAX" => tools.tax4fun.Run(dutyDir, params)
        case "PCO" => tools.pca.Run(dutyDir, params)
        case "LF2" => tools.lefse2.Run(dutyDir, params)
        case "LEF" => tools.lefse.Run(dutyDir, params)
        case x if x == "GO" || x == "KEGG" => tools.gokegg.Run(dutyDir, params, abbre)
        case "TT" => tools.tabletrans.Run(dutyDir)
        case "MTT" => tools.merge2table.Run(dutyDir, params)
        case "MMT" => tools.mergeMultiTable.Run(dutyDir, params)
      }
      val msg = if(result._1 == 1) {
        creatZip(dutyDir)
        "Start Time:"+start+"\n\n运行成功！"
      } else {
        "Start Time:"+start+"\n\n错误信息：\n"+result._2+"\n\n运行失败！"
      }
      dutyController.updateFinish(id, taskname, result._1, result._3, result._4, result._5, result._6)
      FileUtils.writeStringToFile(new File(dutyDir,"log.txt"), msg)
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def getParams(taskname:String,abbre: String) = Action { implicit request =>
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"/users/"+id+"/"+taskname
    val ele = Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements
    val elements =
      if(ele != "") jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)
      else Map(""->"")
    var params = abbre match {
      case "PCA" => tools.pca.GetParams(dutyDir, elements)
      case "ADB" => tools.boxDiversity.GetParams(dutyDir, elements)
      case "TAX" => tools.tax4fun.GetParams(dutyDir)
      case "LF2" => tools.lefse2.GetParams(dutyDir, elements)
      case "LEF" => tools.lefse.GetParams(dutyDir)
      case x if x == "KEGG" || x == "GO" => tools.gokegg.GetParams(dutyDir, elements, abbre)
    }
    val pics=getReDrawPics(dutyDir)
    if(params.toString().indexOf("\"pics\"") < 0) params = params ++ Json.obj("pics" -> pics)
    Ok(params)
  }

  def reDrawRun(taskname:String,abbre:String) = Action(parse.multipartFormData) { implicit request =>
    val newEle = request.body.asFormUrlEncoded.map { case (key, value) => key -> value.mkString(";")}
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"/users/"+id+"/"+taskname
    val oldEle = jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)
    var result = abbre match {
      case "PCA" => tools.pca.ReDraw(dutyDir, newEle)
      case "ADB" => tools.boxDiversity.ReDraw(dutyDir, newEle, oldEle)
      case "LF2_Res" => tools.lefse2.ReDrawRes(dutyDir, newEle, oldEle)
      case "LF2_Cla" => tools.lefse2.ReDrawCla(dutyDir, newEle, oldEle)
      case "LF2_Fea" => tools.lefse2.ReDrawFea(dutyDir, newEle, oldEle)
      case x if x == "KEGG" || x == "GO" => tools.gokegg.ReDraw(dutyDir, newEle, abbre)
    }
    val valid = result.value("valid").toString()
    val elements = result.value("elements").toString()
    if(valid == "true") {
      creatZip(dutyDir)
      Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)
    }
    result = result.-("elements")
    Ok(result)
  }




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


  var LEF: Array[String] =Array("lefse_LDA.xls","lefse_LDA.pdf","lefse_LDA.cladogram.pdf")
  var LEFins: Array[String] =Array("LDA判别分析结果","LDA分析柱图","进化分支图")


  def getDownloadFiles(taskname:String,soft:String): Action[AnyContent] =Action { implicit request=>
    val id=request.session.get("userId").get

    val (outfiles,filesins) =
      if(soft=="LEF") {
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
      else{
        val row = Await.result(downfilesDao.getFilesByAbbre(soft), Duration.Inf)
        (row.file.split(";;").toArray, row.fileins.split(";;").toArray)
      }
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

  def creatDownDuty(taskname:String,uid:String,num:Double): Action[AnyContent] =Action{ implicit request=>
    println()
    val target = Utils.path + "users/" + uid + "/" + taskname + ".zip"
    val path = Utils.path + "users/" + uid + "/" + taskname
    creatZip(target,path)
    Ok.sendFile(new File(target),onClose = () => {
//      println(111)
      Files.delete(new File(target).toPath)
    }).withHeaders(
      //缓存
      CACHE_CONTROL -> "max-age=3600",
      CONTENT_DISPOSITION -> ("attachment; filename=" + taskname + ".zip"),
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

  def fileTrimMove(source:File, dest:File) = {
    //去掉全空的行，去掉前后的空字符串，每行trim，再"//s+"，再制表符分隔
//    val content = FileUtils.readFileToString(source).trim
    val content = FileUtils.readLines(source).asScala.filter(!_.trim.isEmpty).map{line =>
      line.trim.split("\t").mkString("\t")
    }.mkString("\n")
    FileUtils.writeStringToFile(dest, content)

  }

}
