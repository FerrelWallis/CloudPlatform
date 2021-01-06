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

class RSequenceController @Inject()(cc: ControllerComponents, dutydao: dutyDao, rservice: RService, dutyController: DutyController)(implicit exec: ExecutionContext) extends AbstractController(cc) with MyStringTool {

  val userDutyDir: String =Utils.path+"users/"

  case class TaxFunData(taskname:String)

  val TaxFunForm: Form[TaxFunData] =Form(
    mapping (
      "taskname"->text
    )(TaxFunData.apply)(TaxFunData.unapply)
  )


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
    val dutyDir=rservice.creatUserDir(id,data.taskname)

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

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exec(command)

      if (execCommand.isSuccess) {
        val finish=dutyController.updateFini(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\nFinish Time:"+finish+"\n\n运行成功！")
        rservice.creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }



}
