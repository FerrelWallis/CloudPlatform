package tools

import java.io.File

import controllers.{DutyController, RService}
import dao.dutyDao
import org.apache.commons.io.FileUtils
import play.api.libs.Files
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, MyFile, MyStringTool, Utils}
import controllers.RService

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

object ttest extends MyFile with MyStringTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")
    val file1 = request.body.file("table1").get
    val file2 = request.body.file("table2").get
    file1.ref.getPath.moveToFile(tableFile)
    file2.ref.getPath.moveToFile(groupFile)
    val input = file1.filename + "/" + file2.filename

    val ve = if(params("ve") == "FALSE") "异方差双样本检验" else "等方差双样本检验"
    val param= "是否要做配对T检验：" + params("p") + "/进行何种方差检验：" + ve +
      "/多重校验方法：" + params("c") + "/P值检验阈值：" + params("ptn") + "/Q值检验阈值：" + params("qtn")

    try {
      val ptn = if(params("ptn") == "") "" else " -ptn " + params("ptn")
      val qtn = if(params("qtn") == "") "" else " -qtn " + params("qtn")

      val command = "Rscript "+Utils.path+"R/t_test/t-test.R -i " + tableFile.getAbsolutePath +
        " -g " + groupFile.getAbsolutePath + " -o " + dutyDir + "/out" + " -p " + params("p") +
        " -ve " + params("ve") + " -c " + params("c") + ptn + qtn

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")
      println(command)

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "T检验", input, param, "")
  }

}
