package tools

import java.io.File

import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, MyFile, MyMapTool, MyStringTool, Utils}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object revseq extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val seqFile=new File(dutyDir,"seq.txt")
    val file = request.body.file("table1").get
    val input=file.filename
    file.ref.getPath.moveToFile(seqFile)
    seqFile.setExecutable(true,false)
    seqFile.setReadable(true,false)
    seqFile.setWritable(true,false)

    val tag=if(params("reverse")=="N") "" else "/是否在输出序列中加“Reversed:”标签：" + params("tag")
    val param="输出序列格式：" + params("osformat") + "/输出序列是否要反转：" + params("reverse") + tag +
      "/是否要补足输出序列：" + params("complement")

    try {
      val command = Utils.path+"R/EMBOSS/EMBOSS-6.6.0/emboss/revseq -sequence "+ seqFile.getAbsolutePath +
        " -outseq " + dutyDir + "/out/sequence." + params("osformat") + " -osformat2 " +  params("osformat") +
        " -reverse " + params("reverse") + " -tag " + params("tag") + " -complement " + params("complement")
      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exec(command)

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "Emboss Revseq", input, param, "")
  }

}

