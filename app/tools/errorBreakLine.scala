package tools

import java.io.File

import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, MyFile, MyMapTool, MyStringTool, Utils}

import scala.collection.JavaConverters._

object errorBreakLine extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val tableFile=new File(dutyDir,"table.txt")
    val file1=request.body.file("table1").get
    file1.ref.getPath.moveToFile(tableFile)

    val elements= Json.obj("barcolor"->"#0000FF", "linecolor"->"#000000",
      "pointcolor"->"#FFFF00", "rsscale"->params("rsscale"), "rscenter"->params("rscenter"),
      "xtp"->"60", "xts"->"13", "xls"->"16", "xtext"->"", "yts"->"12", "yls"->"16",
      "ytext"->"value", "ms"->"16", "mstext"->"", "width"->"7", "height"->"7",
      "dpi"->"300", "lw"->"0.5").toString()

    val param= s"是否归一化：${params("rsscale")}/是否中心化：${params("rscenter")}"
    
    try {
      val command = "Rscript "+Utils.path+"R/error_bar/errorbar_plot.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out -if pdf -rs " + params("rsscale") + ":" + params("rscenter")

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/error_bar.pdf",dutyDir+"/out/error_bar.png")
        Utils.pdf2Png(dutyDir+"/out/error_bar.pdf",dutyDir+"/out/error_bar.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "误差折线图", file1.filename, param, elements)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val tableFile=new File(dutyDir,"table.txt")
    val newelements= Json.obj("barcolor"->elements("barcolor"), "linecolor"->elements("linecolor"),
      "pointcolor"->elements("pointcolor"), "rsscale"->elements("rsscale"), "rscenter"->elements("rscenter"),
      "xtp"->elements("xtp"), "xts"->elements("xts"), "xls"->elements("xls"), "xtext"->elements("xtext"),
      "yts"->elements("yts"), "yls"->elements("yls"), "ytext"->elements("ytext"), "ms"->elements("ms"),
      "mstext"->elements("mstext"), "width"->elements("width"), "height"->elements("height"),
      "dpi"->elements("dpi"), "lw"->elements("lw")).toString()

    val xtext=if(elements("xtext")=="") " " else elements("xtext")
    val ytext=if(elements("ytext")=="") " " else elements("ytext")
    val mstext=if(elements("mstext")=="") " " else elements("mstext")

    val command = "Rscript "+Utils.path+"R/error_bar/errorbar_plot.R -i "+ tableFile.getAbsolutePath +
      " -o " +dutyDir+"/out -if pdf -bc \"" + elements("barcolor") + "\" -lc \"" + elements("linecolor") + "\"" +
      " -pc \"" + elements("pointcolor") + "\" -rs " + elements("rsscale") + ":" + elements("rscenter") + " -xtp " +
      elements("xtp") + ":1 -xts sans:bold.italic:" + elements("xts") + " -xls \"sans:bold.italic:" + elements("xls") +
      ":" + xtext + ":black\"" + " -yts sans:bold.italic:" + elements("yts") + " -yls \"sans:bold.italic:" +
      elements("yls") + ":" + ytext + ":black\" -ms \"sans:bold.italic:" + elements("ms") + ":" + mstext +
      ":black\" -ls " + elements("width") + ":" + elements("height") + " -dpi " + elements("dpi") + " -lw " + elements("lw")

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    println(command)

    var valid = ""
    var pics = ""
    var msg = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/error_bar.pdf",dutyDir+"/out/error_bar.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/error_bar.pdf",dutyDir+"/out/error_bar.tiff") //替换图片
      pics=dutyDir+"/out/error_bar.png"
      valid = "true"
    } else {
      valid = "false"
      msg = execCommand.getErrStr
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements, "message"->msg)
  }

}
