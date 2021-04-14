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

object boxplot extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val file1=request.body.file("table1").get
    val tableFile=new File(dutyDir,"table.txt")
    val input= file1.filename
    val param= "是否绘制盒子内部点："+params("spot")+"/Y轴范围："+params("ymin")+"-"+params("ymax")
    val ylim=
      if(params("ymin")!=""&&params("ymax")!="") " -ymm " + params("ymin")+":"+params("ymax")
      else ""

    val elements= Json.obj("spot"->params("spot"),"ymin"->params("ymin"),"ymax"->params("ymax"),"lp"->"right:top",
      "boxwidth"->"0.7","alp"->"0.8","add"->"1","color"->"#E41A1C:#1E90FF:#FF8C00:#4DAF4A:#984EA3:#40E0D0:#F4AAC4:#DC414B:#957624:#43B43C",
      "width"->"12","length"->"10","dpi"->"300","xts"->"15","xls"->"12","xtext"->"","yts"->"15",
      "yls"->"12","ytext"->"","lts"->"14","lls"->"15","lltext"->"","ms"->"12","mstext"->"",
      "flip"->"TRUE").toString()

    file1.ref.getPath.moveToFile(tableFile)

    try {
      val command = "Rscript "+Utils.path+"R/box/boxplot.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out" + " -sp " + params("spot") + ylim + " -ls 12:10"

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")
      println(command)

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/box.pdf",dutyDir+"/out/box.png")
        Utils.pdf2Png(dutyDir+"/out/box.pdf",dutyDir+"/out/box.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "Boxplot 盒型图", input, param, elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val color=elements("color").split(":")
    val head=FileUtils.readFileToString(new File(dutyDir+"/table.txt")).trim.split("\n")
    val group=head(0).trim.split("\t")
    Json.obj("group"->group,"elements"->elements,"color"->color)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val newelements= Json.obj("lp"->elements("lp"),"spot"->elements("spot"),"ymin"->elements("ymin"),"ymax"->elements("ymax"),
      "boxwidth"->elements("boxwidth"),"alp"->elements("alp"),"add"->elements("add"),"color"->elements("color"), "width"->elements("width"),
      "length"->elements("length"),"dpi"->elements("dpi"),"xts"->elements("xts"),"xls"->elements("xls"),"xtext"->elements("xtext"),
      "yts"->elements("yts"),"yls"->elements("yls"),"ytext"->elements("ytext"),"lts"->elements("lts"),"lls"->elements("lls"),
      "lltext"->elements("lltext"),"ms"->elements("ms"),"mstext"->elements("mstext"),"flip"->elements("flip")).toString()

    val ymm=if(elements("ymin")==""||elements("ymax")=="") "" else " -ymm " + elements("ymin") + ":" + elements("ymax")

    val yls=if(!elements("ytext").equals("")) " -yls sans:bold.italic:" + elements("yls") + ":\"" + elements("ytext") + "\"" else ""
    val xls=if(!elements("xtext").equals("")) " -xls sans:bold.italic:" + elements("xls") + ":\"" + elements("xtext") + "\"" else ""
    val lls=if(!elements("lltext").equals("")) " -lls sans:bold.italic:" + elements("lls") + ":\"" + elements("lltext") + "\"" else ""
    val ms=if(!elements("mstext").equals("")) " -ms sans:bold.italic:" + elements("ms") + ":\"" + elements("mstext") + "\"" else ""

    val command = "Rscript "+Utils.path+"R/box/boxplot.R -i "+ dutyDir+"/table.txt" + " -o " +dutyDir+"/out" +
      " -sp "+ elements("spot") + ymm + " -ls " + elements("width") + ":" + elements("length") + " -dpi " + elements("dpi") + " -bw " +
      elements("boxwidth") + " -alp " + elements("alp") + " -add " + elements("add") + " -xts " + "sans:bold.italic:"+ elements("xts") +
      " -yts " + "sans:bold.italic:" + elements("yts") + " -lts " + "sans:bold.italic:" + elements("lts") + " -cs \"" + elements("color") +
      "\" -lp " + elements("lp") + xls + yls + lls + ms + " -f " + elements("flip")

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    println(command)
    println(execCommand.getOutStr)
    println(execCommand.getErrStr)
    var valid = ""
    var pics = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/box.pdf",dutyDir+"/out/box.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/box.pdf",dutyDir+"/out/box.tiff") //替换图片
      pics=dutyDir+"/out/box.png"
      valid = "true"
    } else {
      valid = "false"
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements)
  }


}
