package tools

import java.io.File

import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, MyFile, MyMapTool, MyStringTool, Utils}

import scala.collection.JavaConverters._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

object multiLinear extends MyFile with MyStringTool with MyMapTool{

  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val tableFile1=new File(dutyDir,"otu.txt")
    val tableFile2=new File(dutyDir,"env.txt")
    val file1 = request.body.file("table1").get
    val file2 = request.body.file("table2").get
    file1.ref.getPath.moveToFile(tableFile1)
    file2.ref.getPath.moveToFile(tableFile2)
    val param="环境因子文件中作为因变量的列数:" + params("cn")

    try {
      val command = "Rscript " + Utils.path + "R/multiLinear/lm.R -i1 " + tableFile1.getAbsolutePath +
        " -i2 " + tableFile2.getAbsolutePath + " -o " + dutyDir + "/out" + " -cn " + params("cn")

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
    (state, msg, "多元线性回归的分析", file1.filename+"/"+file2.filename, param, "")
  }

}
