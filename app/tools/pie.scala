package tools

import java.io.File

import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, MyFile, MyMapTool, MyStringTool, Utils}

import scala.collection.JavaConverters._

object pie extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val tableFile=new File(dutyDir,"table.txt")
    val file1=request.body.file("table1").get
    file1.ref.getPath.moveToFile(tableFile)

    val elements= Json.obj("pt"->params("pt"), "width"->"7", "height"->"7", "tsname"->"",
      "tssize"->"18", "dpi"->"300").toString()

    val pt=if(params("pt")=="full") "圆形饼图" else "环形饼图"
    val param= s"饼图形状：$pt"

    try {
      val command = "Rscript "+Utils.path+"R/pie/pie_full_ring.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out -if pdf -pt " + params("pt")

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/pie.pdf",dutyDir+"/out/pie.png")
        Utils.pdf2Png(dutyDir+"/out/pie.pdf",dutyDir+"/out/pie.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "饼图", file1.filename, param, elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val (group, color)=FileUtils.readLines(new File(dutyDir+"/out/col.xls")).asScala.map{line => (line.split("\t").head, line.split("\t").last)}.unzip
    Json.obj("elements"->elements,"color"->color,"group"->group)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val tableFile=new File(dutyDir,"table.txt")

    val newelements= Json.obj("pt"->elements("pt"), "width"->elements("width"), "height"->elements("height"),
      "tsname"->elements("tsname"), "tssize"->elements("tssize"), "dpi"->elements("dpi")).toString()
    
    val ts=if(elements("tsname")=="") " -ts NULL:NULL:NULL" else " -ts \"" + elements("tsname") + ":" + elements("tssize") + ":NULL\""

    val command = "Rscript "+Utils.path+"R/pie/pie_full_ring.R -i "+ tableFile.getAbsolutePath +
      " -o " +dutyDir+"/out -if pdf -pt " + elements("pt") + " -c \"" + elements("color") + "\" -is " +
      elements("width") + ":" + elements("height") + ts + " -dpi " + elements("dpi")

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")
    println(command)
    
    var valid = ""
    var pics = ""
    var msg = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/pie.pdf",dutyDir+"/out/pie.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/pie.pdf",dutyDir+"/out/pie.tiff") //替换图片
      pics=dutyDir+"/out/pie.png"
      valid = "true"
    } else {
      valid = "false"
      msg = execCommand.getErrStr
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements, "message"->msg)
  }

}

