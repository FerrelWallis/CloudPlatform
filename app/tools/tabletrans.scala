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

object tabletrans extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String)(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val file1=request.body.file("table1").get
    val tableFile=new File(dutyDir,file1.filename)
    file1.ref.getPath.moveToFile(tableFile)
    val input=file1.filename

    try {
      val command = "Rscript "+Utils.path+"R/table_t/table-t.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out/trans.xls"
      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "表格转置", input, "/", "")
  }

}
