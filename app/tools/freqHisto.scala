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

object freqHisto extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val tableFile=new File(dutyDir,"table.txt")
    val file1=request.body.file("table1").get
    file1.ref.getPath.moveToFile(tableFile)
    tableFile.setExecutable(true,false)
    tableFile.setReadable(true,false)
    tableFile.setWritable(true,false)

    val elements= Json.obj("bw"->params("bw"), "color"->"#0000FF", "xtpangle"->"60", "xtphjust"->"1",
      "xts"->"13", "yts"->"12", "xls"->"16", "yls"->"16","ms"->"16","xtext"->"", "ytext"->"count", "mstext"->"",
      "dpi"->"300", "width" -> "7", "height"->"7").toString()

    try {
      val bw=if(params("bw")=="") "" else " -bw " + params("bw")

      val command = "Rscript "+Utils.path+"R/Frequency_histogram/Frequency.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out -if pdf -c #0000FF" + bw

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      //exec需要指定结果输出路径的时候，不指定默认本地任务路径
      execCommand.exect(command,dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/Frequency_bar.pdf",dutyDir+"/out/Frequency_bar.png")
        Utils.pdf2Png(dutyDir+"/out/Frequency_bar.pdf",dutyDir+"/out/Frequency_bar.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg,"频率直方图",file1.filename,"/",elements)
  }

//  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
//    Json.obj("elements"->elements)
//  }

  def ReDraw(dutyDir: String, elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val tableFile=new File(dutyDir,"table.txt")
    val newelements= Json.obj("bw"->elements("bw"), "color"->elements("color"), "xtpangle"->elements("xtpangle"),
      "xtphjust"->elements("xtphjust"), "xts"->elements("xts"), "yts"->elements("yts"), "xls"->elements("xls"), "yls"->elements("yls"),
      "ms"->elements("ms"), "xtext"->elements("xtext"), "ytext"->elements("ytext"), "mstext"->elements("mstext"), "dpi"->elements("dpi"),
      "width" -> elements("width"), "height"->elements("height")).toString()

    val bw=if(elements("bw")=="") "" else " -bw " + elements("bw")
    val xtext=if(elements("xtext")=="") " " else elements("xtext")
    val ytext=if(elements("ytext")=="") " " else elements("ytext")
    val mstext=if(elements("mstext")=="") " " else elements("mstext")

    val command = "Rscript "+Utils.path+"R/Frequency_histogram/Frequency.R -i "+ tableFile.getAbsolutePath +
      " -o " +dutyDir+"/out -if pdf -c \"" + elements("color") + "\"" + bw + " -xts sans:bold.italic:" + elements("xts") +
      " -xls \"sans:bold.italic:" + elements("xls") + ":" + xtext + ":black\" -yts sans:bold.italic:" + elements("yts") +
      " -yls \"sans:bold.italic:" + elements("yls") + ":" + ytext + ":black\" -xtp \"" + elements("xtpangle") + ":" + elements("xtphjust") +
      "\" -ms \"sans:bold.italic:" + elements("ms") + ":" + mstext + ":black\" -ls " + elements("width") + ":" + elements("height") +
      " -if pdf -dpi " + elements("dpi")

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")
    println(command)

    var valid = ""
    var pics = ""
    var msg = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/Frequency_bar.pdf",dutyDir+"/out/Frequency_bar.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/Frequency_bar.pdf",dutyDir+"/out/Frequency_bar.tiff") //替换图片
      pics=dutyDir+"/out/Frequency_bar.png"
      valid = "true"
    } else {
      valid = "false"
      msg = execCommand.getErrStr
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements, "message"->msg)
  }

}