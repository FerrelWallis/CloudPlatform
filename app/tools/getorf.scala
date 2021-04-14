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

object getorf extends MyFile with MyStringTool with MyMapTool{
  
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

    val table=params("table").split(":")
    val find=params("find").split(":")

    val param="输出序列格式：" + params("osformat") + "/选择使用参考：" + table(1) +
      "/ORF显示的最小核苷酸大小：" + params("minsize") + "/ORF显示的最大核苷酸大小：" + params("maxsize") +
      "/输出类型：" + find(1) + "/是否将初始START密码子更改为蛋氨酸：" + params("methionine") +
      "/序列是否为循环的：" + params("circular") + "/是否以相反的顺序找到ORF：" + params("reverse") +
      "/报告侧翼核苷酸的数量：" + params("flanking")

    try {
      val command = Utils.path+"R/EMBOSS/EMBOSS-6.6.0/emboss/getorf -sequence "+ seqFile.getAbsolutePath +
        " -outseq " + dutyDir + "/out/sequence." + params("osformat") + " -osformat2 " +  params("osformat") +
        " -table " + table(0) + " -minsize " + params("minsize") + " -maxsize " + params("maxsize") + " -find " +
        find(0) + " -methionine " + params("methionine") + " -circular " + params("circular") + " -reverse " +
        params("reverse") + " -flanking " + params("flanking")

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
    (state, msg, "Emboss Getorf", input, param, "")
  }

}
