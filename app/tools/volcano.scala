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

object volcano extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val file1=request.body.file("table1").get
    val tableFile=new File(dutyDir,"table.txt")
    val input= file1.filename
    val param= "P值阈值："+params("pcl")+"/F值阈值："+params("fcl")
    val elements= Json.obj("pcl"->params("pcl"),"fcl"->params("fcl"),"xrmin"->"","xrmax"->"",
      "yrmin"->"","yrmax"->"","sp"->"TRUE","color4"->"black","cs"->"blue:red:grey",
      "xts"->"16","yts"->"16","xls"->"18","yls"->"18","ts"->"20","lts"->"16", "ltes"->"12",
      "xtext"->"log2(FC)","ytext"->"-log10(pvalue)","tstext"->"","ltstext"->"","width"->"15",
      "height"->"15","dpi"->"300").toString()
    
    file1.ref.getPath.moveToFile(tableFile)
    
    try {
      val command = "Rscript "+Utils.path+"R/volcano/volcano.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out" + " -pcl " + params("pcl") + " -fcl " + params("fcl") + " -if pdf -in volcano"

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")
      println(command)

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/volcano.pdf",dutyDir+"/out/volcano.png")
        Utils.pdf2Png(dutyDir+"/out/volcano.pdf",dutyDir+"/out/volcano.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "火山图（Volcano）", input, param, elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val color=elements("cs").split(":")
    val group=("DOWN", "UP", "NO")
    Json.obj("group"->group,"color"->color,"elements"->elements)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String], abbre:String)(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val tableFile = if(abbre == "VOC") new File(dutyDir,"table.txt") else new File(dutyDir,"out/edger.gene_diff.txt")
    val newelements=Json.obj("pcl"->elements("pcl"),"fcl"->elements("fcl"),"xrmin"->elements("xrmin"),
      "xrmax"->elements("xrmax"),"yrmin"->elements("yrmin"),"yrmax"->elements("yrmax"),"sp"->elements("sp"),"color4"->elements("color4"),
      "cs"->elements("color"),"xts"->elements("xts"),"yts"->elements("yts"),"xls"->elements("xls"),"yls"->elements("yls"),
      "ts"->elements("ts"),"lts"->elements("lts"), "ltes"->elements("ltes"), "xtext"->elements("xtext"),"ytext"->elements("ytext"),
      "tstext"->elements("tstext"),"ltstext"->elements("ltstext"),"width"->elements("width"), "height"->elements("height"),
      "dpi"->elements("dpi")).toString()

    val xr=if(elements("xrmin")=="" || elements("xrmax")=="") "" else " -xr " + elements("xrmin") + ":" + elements("xrmax")
    val yr=if(elements("yrmin")=="" || elements("yrmax")=="") "" else " -yr " + elements("yrmin") + ":" + elements("yrmax")
    val ltstext = if(elements("ltstext") == "") " " else elements("ltstext")
    val tstext = if(elements("tstext") == "") " " else elements("tstext")

    val command = "Rscript "+Utils.path+"R/volcano/volcano.R -i "+ tableFile.getAbsolutePath +
      " -o " +dutyDir+"/out" + " -pcl " + elements("pcl") + " -fcl " + elements("fcl") + " -sp " + elements("sp") + xr + yr +
      " -lc \"" + elements("color4") + "\" -cs \"" + elements("color") + "\" -xts sans:bold.italic:" + elements("xts") +
      " -xls \"sans:bold.italic:" + elements("xls") + ":" + elements("xtext") + "\" -yts sans:bold.italic:" +
      elements("yts") + " -yls \"sans:bold.italic:" + elements("yls") + ":" + elements("ytext") + "\" -ts \"sans:bold.italic:" +
      elements("ts") + ":" + tstext + "\" -lts \"sans:bold.italic:" + elements("lts") + ":" + ltstext +
      "\" -ltes sans:bold.italic:" + elements("ltes") + " -is \"" + elements("width") + ":" + elements("height") + "\" -dpi " +
      elements("dpi") + " -if pdf -in volcano"

    println(command)

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")
    
    var valid = ""
    var pics = ""
    var msg = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/volcano.pdf",dutyDir+"/out/volcano.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/volcano.pdf",dutyDir+"/out/volcano.tiff") //替换图片
      pics=dutyDir+"/out/volcano.png"
      valid = "true"
    } else {
      valid = "false"
      msg = execCommand.getErrStr
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements, "message"->msg)
  }

}
