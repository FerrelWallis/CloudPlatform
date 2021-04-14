package tools

import java.io.File

import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, MyFile, MyMapTool, MyStringTool, Utils}

import scala.collection.JavaConverters._

object bubble extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"
    
    val tableFile=new File(dutyDir,"table.txt")
    val file1=request.body.file("table1").get
    file1.ref.getPath.moveToFile(tableFile)
    val elements= Json.obj("ps"->"2", "color"->"#E41A1C:#1E90FF:#4DAF4A:#984EA3:#ADD1E5:#999999:#66CC99:#9999CC:#CC6666:#FF8C00",
      "xtp"->"60", "xts"->"11", "xls"->"14", "xtext"->"", "yts"->"11", "yls"->"14", "ytext"->"",
      "ms"->"16", "mstext"->"", "width"->"10", "height"->"10", "dpi"->"300", "dl"->"50",
      "top"->params("top"), "filetype"->params("filetype")).toString()

    val param= s"文件类型：${params("filetype")}/用于做图的行数：${params("top")}"

    try {
      val command = "Rscript "+Utils.path+"R/bubble/bubble.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out -if pdf -in bubble -dt " + params("filetype") + " -top " + params("top") +
        " -is 10:10"

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/bubble.pdf",dutyDir+"/out/bubble.png")
        Utils.pdf2Png(dutyDir+"/out/bubble.pdf",dutyDir+"/out/bubble.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "气泡图", file1.filename, param, elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val color=elements("color").split(":")
    val group=FileUtils.readLines(new File(dutyDir+"/out/standard_table.txt")).asScala.map{_.split("\t")(1)}.tail.distinct.sorted
    val row=FileUtils.readLines(new File(dutyDir+"/table.txt")).asScala.length - 1
    Json.obj("elements"->elements,"color"->color,"group"->group,"row"->row)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val tableFile=new File(dutyDir,"table.txt")
    val newelements= Json.obj("ps"->elements("ps"), "color"->elements("color"), "xtp"->elements("xtp"),
      "xts"->elements("xts"), "xls"->elements("xls"), "xtext"->elements("xtext"), "yts"->elements("yts"), "yls"->elements("yls"),
      "ytext"->elements("ytext"), "ms"->elements("ms"), "mstext"->elements("mstext"), "width"->elements("width"),
      "height"->elements("height"), "dpi"->elements("dpi"), "dl"->elements("dl"), "top"->elements("top"),
      "filetype"->elements("filetype")).toString()

    val xtext=if(elements("xtext")=="") " " else elements("xtext")
    val ytext=if(elements("ytext")=="") " " else elements("ytext")
    val mstext=if(elements("mstext")=="") " " else elements("mstext")

    val command = "Rscript "+Utils.path+"R/bubble/bubble.R -i "+ tableFile.getAbsolutePath +
      " -o " +dutyDir+"/out -if pdf -in bubble -ps " + elements("ps") + " -pc \"" + elements("color") +
      ":#E41A1C:#1E90FF:#4DAF4A:#984EA3:#ADD1E5:#999999:#66CC99:#9999CC:#CC6666:#FF8C00" + "\"" +
      " -xtp " + elements("xtp") + ":1" + " -xts sans:bold.italic:" + elements("xts") +
      " -xls \"sans:bold.italic:" + elements("xls") + ":" + xtext + ":black\"" + " -yts sans:bold.italic:" +
      elements("yts") + " -yls \"sans:bold.italic:" + elements("yls") + ":" + ytext +
      ":black\" -ms \"sans:bold.italic:" + elements("ms") + ":" + mstext +
      ":black\" -is " + elements("width") + ":" + elements("height") + " -dpi " + elements("dpi") +
      " -dl " + elements("dl") + " -top " + elements("top") + " -dt " + elements("filetype")

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    println(command)

    val group=FileUtils.readLines(new File(dutyDir+"/out/standard_table.txt")).asScala.map{_.split("\t")(1)}.tail.distinct.sorted
    val color=(elements("color") + ":#E41A1C:#1E90FF:#4DAF4A:#984EA3:#ADD1E5:#999999:#66CC99:#9999CC:#CC6666:#FF8C00").split(":")

    var valid = ""
    var pics = ""
    var msg = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/bubble.pdf",dutyDir+"/out/bubble.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/bubble.pdf",dutyDir+"/out/bubble.tiff") //替换图片
      pics=dutyDir+"/out/bubble.png"
      valid = "true"
    } else {
      valid = "false"
      msg = execCommand.getErrStr
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements, "message"->msg, "color"->color, "group"->group)
  }


}