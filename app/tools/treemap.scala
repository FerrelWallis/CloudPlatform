package tools

import java.io.File

import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, MyFile, MyMapTool, MyStringTool, Utils}

import scala.collection.JavaConverters._
import scala.collection.mutable

object treemap extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"
    
    val tableFile=new File(dutyDir,"table.txt")
    val file1=request.body.file("table1").get
    file1.ref.getPath.moveToFile(tableFile)

    val elements= Json.obj("sbccolor"->"white", "lbccolor"->"gray", "llccolor"->"black",
      "slccolor"->"#842B00", "xls"->"17", "yls"->"17", "xtext"->"", "ytext"->"", "ms"->"17",
      "mstext"->"", "width"->"18", "height"->"19", "dpi"->"300").toString()
    
    try {
      val command = "Rscript "+Utils.path+"R/treemap/treemap.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out"

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/treemap.pdf",dutyDir+"/out/treemap.png")
        Utils.pdf2Png(dutyDir+"/out/treemap.pdf",dutyDir+"/out/treemap.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "树图（Treemap）", file1.filename, "/", elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val (group,firstcolor)=FileUtils.readLines(new File(dutyDir+"/out/group_color.xls")).asScala.map{lines=>
      (lines.split("\t")(0).replaceAll("\"",""),lines.split("\t")(1).replaceAll("\"",""))
    }.unzip
    val color = if(elements.contains("color")) elements("color").split(":") else firstcolor.toArray
    Json.obj("elements"->elements,"color"->color,"group"->group)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val tableFile=new File(dutyDir,"table.txt")
    val newelements= Json.obj("color"->elements("color"), "sbccolor"->elements("sbccolor"), "lbccolor"->elements("lbccolor"),
      "llccolor"->elements("llccolor"), "slccolor"->elements("slccolor"), "xls"->elements("xls"), "yls"->elements("yls"), "xtext"->elements("xtext"),
      "ytext"->elements("ytext"), "ms"->elements("ms"), "mstext"->elements("mstext"), "width"->elements("width"), "height"->elements("height"),
      "dpi"->elements("dpi")).toString()

    val xtext=if(elements("xtext")=="") " " else elements("xtext")
    val ytext=if(elements("ytext")=="") " " else elements("ytext")
    val mstext=if(elements("mstext")=="") " " else elements("mstext")

    val command = "Rscript "+Utils.path+"R/treemap/treemap.R -i "+ tableFile.getAbsolutePath +
      " -o " +dutyDir+"/out -cs \"" + elements("color") + "\"" + " -sbc \"" + elements("sbccolor") + "\" -lbc \"" +
      elements("lbccolor") + "\" -llc \"" + elements("llccolor") + "\" -slc \"" + elements("slccolor") + "\" -xls \"sans:plain:" +
      elements("xls") + ":" + xtext + ":black\"" + " -yls \"sans:plain:" + elements("yls") + ":" + ytext +
      ":black\" -ms \"sans:plain:" + elements("ms") + ":" + mstext + ":black\" -ls " + elements("width") +
      ":" + elements("height") + " -dpi " + elements("dpi")

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    println(command)
    
    var valid = ""
    var pics = ""
    var msg = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/treemap.pdf",dutyDir+"/out/treemap.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/treemap.pdf",dutyDir+"/out/treemap.tiff") //替换图片
      pics=dutyDir+"/out/treemap.png"
      valid = "true"
    } else {
      valid = "false"
      msg = execCommand.getErrStr
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements, "message"->msg)
  }


}
