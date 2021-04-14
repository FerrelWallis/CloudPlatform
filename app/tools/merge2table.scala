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

object merge2table extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val file1=request.body.file("table1").get
    val file2=request.body.file("table2").get
    val tableFile1=new File(dutyDir,file1.filename)
    val tableFile2=new File(dutyDir,file2.filename)
    file1.ref.getPath.moveToFile(tableFile1)
    file2.ref.getPath.moveToFile(tableFile2)
    val input=file1.filename+"/"+file2.filename

    val param="合并参照列 (矩阵1 - 矩阵2) ：第" + params("b1") + "列 : 第" + params("b2") + "列/设置合并方式：" + params("ct") +
      "/缺省值设置：" + (if(params("sn")=="default") params("mysn") else if(params("sn")=="N/A") "NA" else params("sn"))

    try {
      val sn=if(params("sn")=="default") " -sn \"" + params("mysn") + "\"" else " -sn \"" + params("sn") + "\""
      val command = "Rscript " + Utils.path + "R/table_two/table_two2.R -i1 " + tableFile1.getAbsolutePath +
        " -i2 " + tableFile2.getAbsolutePath + " -o " + dutyDir + "/out/merge2table.xls -b " + params("b1") +
        ":" + params("b2") + " -ct " + params("ct") + sn

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
    (state, msg, "两表格合并", input, param, "")
  }

}
