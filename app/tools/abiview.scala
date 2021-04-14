package tools

import java.io.File

import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, MyFile, MyMapTool, MyStringTool, Utils}

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object abiview extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val abiFile=new File(dutyDir,"abiview.ab1")
    val file = request.body.file("table1").get
    val input=file.filename
    file.ref.getPath.moveToFile(abiFile)
    abiFile.setExecutable(true,false)
    abiFile.setReadable(true,false)
    abiFile.setWritable(true,false)

    val param="输出图片类型：" + params("graph") + "/输出序列格式：" + params("osformat") + "/一个窗口显示" + params("window") +
      "个碱基" + "/绘制第" + params("startbase") + "-" + params("endbase") + "个碱基" + "/绘制迹线的碱基：" + params("bases") +
      "/是否分开绘制不同碱基的迹线：" + params("separate") + "/是否将序列显示在图片上：" + params("sequence") +
      "/是否绘制Y轴刻度：" + params("yticks")

    val elements=Json.obj("graph"->params("graph"), "osformat"->params("osformat"), "window"->params("window"),
      "startbase"->params("startbase"), "endbase"->params("endbase"), "bases"->params("bases"), "separate"->params("separate"),
      "sequence"->params("sequence"), "yticks"->params("yticks"),"gtitle"->"","gsubtitle"->"","gxtitle"->"").toString()

    try {
      val command1 = Utils.path+"R/EMBOSS/EMBOSS-6.6.0/emboss/abiview -infile "+ abiFile.getAbsolutePath +
        " -outseq " + dutyDir + "/out/sequence." + params("osformat") + " -osformat2 " +  params("osformat") +
        " -startbase " + params("startbase") + " -endbase " + params("endbase") + " -yticks " + params("yticks") +
        " -sequence " + params("sequence") + " -window " + params("window") + " -bases " + params("bases") +
        " -graph ps -gdirectory " + dutyDir + "/out" + " -separate " + params("separate")
      
      val command2= Utils.path+"R/EMBOSS/EMBOSS-6.6.0/emboss/abiview -infile "+
        abiFile.getAbsolutePath + " -outseq "+dutyDir+"/out/sequence." + params("osformat") +
        " -osformat2 " + params("osformat") + " -startbase " + params("startbase") + " -endbase " +
        params("endbase") + " -yticks " + params("yticks") + " -sequence " + params("sequence") +
        " -window " + params("window") + " -bases " + params("bases") + " -graph " + params("graph") +
        " -gdirectory " + dutyDir + "/out" + " -separate " + params("separate")
      
      val command3="ps2pdf " + dutyDir + "/out/abiview.ps " + dutyDir + "/out/abiview.pdf"
      val commandpack= if(params("graph")=="ps") Array(command1, command3) else Array(command1, command2, command3)
      println(command1)
      println(command2)
      println(command3)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),commandpack.mkString("\n"))
      val execCommand = new ExecCommand
      execCommand.exec(commandpack)

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "Emboss Abiview", input, param, elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val pics = dutyDir + "/out/abiview.pdf"
    Json.obj("pdfUrl"->pics,"elements"->elements)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String], oldelements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val abiFile=new File(dutyDir,"abiview.ab1")
    val graph=oldelements("graph")
    val osformat=oldelements("osformat")
    val gtitle=if(elements("gxtitle")=="") "" else " -gtitle \"" + elements("gtitle") + "\""
    val gsubtitle=if(elements("gsubtitle")=="") "" else " -gsubtitle \"" + elements("gsubtitle") + "\""
    val gxtitle=if(elements("gxtitle")=="") "" else " -gxtitle \"" + elements("gxtitle") + "\""

    val newelements = Json.obj("graph"->graph, "osformat"->osformat, "window"->elements("window"),
      "startbase"->elements("startbase"), "endbase"->elements("endbase"), "bases"->elements("bases"), "separate"->elements("separate"),
      "sequence"->elements("sequence"), "yticks"->elements("yticks"),"gtitle"->elements("gtitle"),"gsubtitle"->elements("gsubtitle"),
      "gxtitle"->elements("gxtitle")).toString()

    val command1 = Utils.path+"R/EMBOSS/EMBOSS-6.6.0/emboss/abiview -infile "+ abiFile.getAbsolutePath +
      " -outseq " + dutyDir + "/out/sequence." + osformat + " -osformat2 " +  osformat +
      " -startbase " + elements("startbase") + " -endbase " + elements("endbase") + " -yticks " + elements("yticks") +
      " -sequence " + elements("sequence") + " -window " + elements("window") + " -bases " + elements("bases") +
      " -graph ps -gdirectory " + dutyDir + "/out" + " -separate " + elements("separate") + gtitle +
      gsubtitle + gxtitle

    val command2= Utils.path+"R/EMBOSS/EMBOSS-6.6.0/emboss/abiview -infile "+
      abiFile.getAbsolutePath + " -outseq "+dutyDir+"/out/sequence." + osformat +
      " -osformat2 " + osformat + " -startbase " + elements("startbase") + " -endbase " +
      elements("endbase") + " -yticks " + elements("yticks") + " -sequence " + elements("sequence") +
      " -window " + elements("window") + " -bases " + elements("bases") + " -graph " + graph +
      " -gdirectory " + dutyDir + "/out" + " -separate " + elements("separate") + gtitle +
      gsubtitle + gxtitle

    val command3="ps2pdf " + dutyDir + "/out/abiview.ps " + dutyDir + "/out/abiview.pdf"

    if(graph=="ps") FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command1+"\n"+command3)
    else FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command1+"\n"+command2+"\n"+command3)

    println(command1)
    println(command2)
    println(command3)

    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    var valid = ""
    var msg = ""
    if (execCommand.isSuccess) {
      valid = "true"
    } else {
      valid = "false"
      msg = execCommand.getErrStr
    }
    Json.obj("valid"->valid, "elements"->newelements, "message"->msg)
  }

}
