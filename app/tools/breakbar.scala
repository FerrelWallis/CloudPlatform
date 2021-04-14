package tools

import java.io.File

import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, MyFile, MyMapTool, MyStringTool, Utils}

import scala.collection.JavaConverters._

object breakbar extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val tableFile=new File(dutyDir,"table.txt")
    val file1=request.body.file("table1").get
    file1.ref.getPath.moveToFile(tableFile)

    val elements= Json.obj("bspmin"->params("bspmin"), "bspmax"->params("bspmax"), "tps"->"30", "xtpangle"->"90",
      "xtphjust"->"1", "xtpvjust"->"0.5", "color"->"#66C2A5:#FC8D62:#8DA0CB:#E78AC3:#A6D854:#FFD92F:#E5C494:#B3B3B3",
      "xts"->"11", "yts"->"11", "xls"->"14", "yls"->"14", "ms"->"16", "xtext"->"", "ytext"->"",
      "mstext"->"", "dpi"->"300", "width" -> "7", "height"->"7").toString()

    try {
      val bsp = " -bsp " + params("bspmin") + ":" + params("bspmax")
      val command = "Rscript "+Utils.path+"R/break_bar/duanzhou.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out -if pdf -in breakHisto" + bsp

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/breakHisto.pdf",dutyDir+"/out/breakHisto.png")
        Utils.pdf2Png(dutyDir+"/out/breakHisto.pdf",dutyDir+"/out/breakHisto.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "断轴柱状图", file1.filename, "/", elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val color=elements("color").split(":")
    val genenum=FileUtils.readLines(new File(dutyDir+"/table.txt")).asScala.length-1
    Json.obj("elements"->elements,"color"->color,"genenum"->genenum)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val tableFile = new File(dutyDir,"table.txt")
    val newelements = Json.obj("bspmin"->elements("bspmin"), "bspmax"->elements("bspmax"), "tps"->elements("tps"),
      "xtpangle"->elements("xtpangle"), "xtphjust"->elements("xtphjust"), "xtpvjust"->elements("xtpvjust"),
      "color"->elements("designcolor"), "xts"->elements("xts"), "yts"->elements("yts"), "xls"->elements("xls"), "yls"->elements("yls"),
      "ms"->elements("ms"), "xtext"->elements("xtext"), "ytext"->elements("ytext"), "mstext"->elements("mstext"), "dpi"->elements("dpi"),
      "width" -> elements("width"), "height"->elements("height")).toString()

    val bsp = " -bsp " + elements("bspmin") + ":" + elements("bspmax")
    val xtext=if(elements("xtext")=="") " " else elements("xtext")
    val ytext=if(elements("ytext")=="") " " else elements("ytext")
    val mstext=if(elements("mstext")=="") " " else elements("mstext")
    val tps=(elements("tps").toFloat/100).toFloat.toString

    val command =  "Rscript "+Utils.path+"R/break_bar/duanzhou.R -i "+ tableFile.getAbsolutePath +
      " -o " +dutyDir+"/out -if pdf -in breakHisto" + bsp + " -tps " + tps + " -cs \"" + elements("designcolor") +
      "\" -ms \"sans:bold.italic:" + elements("ms") + ":" + mstext + ":black\" -xts sans:bold.italic:" + elements("xts") +
      " -xtp \"" + elements("xtpangle") + ":" + elements("xtphjust") + ":" + elements("xtpvjust") +
      "\" -yts sans:bold.italic:" + elements("yts") + " -xls \"sans:bold.italic:" + elements("xls") + ":black:" + xtext +
      "\" -yls \"sans:bold.italic:" + elements("yls") + ":black:" + ytext + "\" -is " + elements("width") + ":" +
      elements("height") + " -dpi " + elements("dpi")

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    println(command)
    
    var valid = ""
    var pics = ""
    var msg = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/breakHisto.pdf",dutyDir+"/out/breakHisto.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/breakHisto.pdf",dutyDir+"/out/breakHisto.tiff") //替换图片
      pics=dutyDir+"/out/breakHisto.png"
      valid = "true"
    } else {
      valid = "false"
      msg = execCommand.getErrStr
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements, "message"->msg)
  }


}
