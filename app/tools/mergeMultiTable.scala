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

object mergeMultiTable extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"
    val filenum = request.body.files.length
    val (files,input)=(1 to filenum).map{i=>
      val file=request.body.file("table"+i).get
      val tableFile=new File(dutyDir,file.filename)
      file.ref.getPath.moveToFile(tableFile)
      (tableFile.getAbsolutePath,file.filename)
    }.unzip

    val param="合并参照列：" + params("b") + "/设置合并方式：" + params("ct") + "/缺省值设置：" +
      (if(params("sn")=="default") params("mysn") else if(params("sn")=="N/A") "NA" else params("sn"))

    try {
      val sn=if(params("sn")=="default") " -sn \"" + params("mysn") + "\"" else " -sn \"" + params("sn") + "\""

      val b = " -b " + (if(params("b").split(":").length==filenum) params("b") else (1 to filenum).map{x=>params("b")}.mkString(":"))
      println(b)

      val command = "Rscript " + Utils.path + "R/table_more/table-more.R -i " + files.mkString(";") +
        " -o " + dutyDir + "/out/mergeTables.xls" + " -ct " + params("ct") + sn + b

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      //exec需要指定结果输出路径的时候，不指定默认本地任务路径
      execCommand.exect(command,dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "多表格合并", input.mkString("/"), param, "")
  }

}
