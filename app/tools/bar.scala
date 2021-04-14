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

object bar extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val isgroup = (params("isgrouptext") == "TRUE")
    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")
    val file1 = request.body.file("table1").get
    file1.ref.getPath.moveToFile(tableFile)
    val file2 = request.body.file("table2")
    val (group,filepara)=if(isgroup) {
      file2.get.ref.getPath.moveToFile(groupFile)
      (" -g " + groupFile.getAbsolutePath,file1.filename+"/"+file2.get.filename)
    } else ("",file1.filename)

    val elements=Json.obj("pe"->"TRUE","color"->"#FF0000:#FFC913:#FFFF00:#008000:#00FFFF:#297EFF:#800080:#FFC0CB",
      "width"->"20", "height"->"20", "dpi"->"300", "lp"->"right", "lo"->"0", "lts"->"15", "ls"->"15",
      "xts"->"18", "yts"->"16", "xls"->"20", "yls"->"20", "xtext"->"", "ytext"->"", "lms"->"19",
      "lmtext"->"", "ms"->"22", "mstext"->"", "m"->params("m"), "mt"->params("mt"), "bw"->"0.9", "xo"->"",
      "xta" -> "0", "xangle"->"0", "yangle"->"90").toString()

    try {
      val command = "Rscript " + Utils.path + "R/bar/bar_group.R -i " + tableFile.getAbsolutePath + group +
        " -o " + dutyDir + "/out -cs #FF0000:#FFC913:#FFFF00:#008000:#00FFFF:#297EFF:#800080:#FFC0CB -m " +
        params("m") + " -mt " + params("mt")

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/bar_group.pdf",dutyDir+"/out/bar_group.png")
        Utils.pdf2Png(dutyDir+"/out/bar_group.pdf",dutyDir+"/out/bar_group.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "柱状图", filepara, "/", elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val color=elements("color").split(":")
    val genenum = FileUtils.readLines(new File(dutyDir+"/table.txt")).asScala.length-1
    val sample = FileUtils.readLines(new File(dutyDir+"/table.txt")).asScala.head.split("\t").drop(1)
    Json.obj("elements"->elements,"color"->color,"genenum"->genenum,"sample"->sample)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")
    val group = if(groupFile.exists()) " -g " + groupFile.getAbsolutePath else ""

    val newelements=Json.obj("pe"->elements("pe"), "color"->elements("designcolor"), "width"->elements("width"),
      "height"->elements("height"), "dpi"->elements("dpi"), "lp"->elements("lp"), "lo"->elements("lo"), "lts"->elements("lts"),
      "ls"->elements("ls"), "xts"->elements("xts"), "yts"->elements("yts"), "xls"->elements("xls"), "yls"->elements("yls"),
      "xtext"->elements("xtext"), "ytext"->elements("ytext"), "lms"->elements("lms"), "lmtext"->elements("lmtext"),
      "ms"->elements("ms"), "mstext"->elements("mstext"), "m"->elements("m"), "mt"->elements("mt"), "bw" -> elements("bw"),
      "xo" -> elements("xo"), "xta" -> elements("xta"), "xangle" -> elements("xangle"), "yangle" -> elements("yangle")).toString()

    val ytext=
      if(!elements("ytext").equals("")) " -yls sans:bold.italic:" + elements("yls") + ":\"" + elements("ytext") + "\"" + ":" + elements("yangle")
      else ""
    val xtext=
      if(!elements("xtext").equals("")) " -xls sans:bold.italic:" + elements("xls") + ":\"" + elements("xtext") + "\"" + ":" + elements("xangle")
      else ""
    val lms=
      if(!elements("lmtext").equals("")) " -lms sans:bold.italic:" + elements("lms") + ":\"" + elements("lmtext") + "\""
      else ""
    val ms=
      if(!elements("mstext").equals("")) " -ms sans:bold.italic:" + elements("ms") + ":\"" + elements("mstext") + "\""
      else ""
    val xo = if(!elements("xo").equals("")) " -xo " + elements("xo") else ""

    val command = "Rscript " + Utils.path+"R/bar/bar_group.R -i " + tableFile.getAbsolutePath + group +
      " -o " + dutyDir + "/out -cs \"" + elements("designcolor") + "\" -pe " + elements("pe") + " -lp " + elements("lp") +
      " -dpi " + elements("dpi") + " -is " + elements("width") + ":" + elements("height") + " -lts sans:bold.italic:" +
      elements("lts") + " -ls " + elements("ls") + " -xts sans:bold.italic:" + elements("xts") + xtext + " -yts sans:bold.italic:" +
      elements("yts") + ytext + lms + ms + " -lo " + elements("lo") + " -m " + elements("m") + " -mt " + elements("mt") + " -bw " +
      elements("bw") + xo + " -xta " + elements("xta")

    println(command)
    
    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")
    
    var valid = ""
    var pics = ""
    var msg = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/bar_group.pdf",dutyDir+"/out/bar_group.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/bar_group.pdf",dutyDir+"/out/bar_group.tiff") //替换图片
      pics=dutyDir+"/out/bar_group.png"
      valid = "true"
    } else {
      valid = "false"
      msg = execCommand.getErrStr
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements, "message"->msg)
  }

}
