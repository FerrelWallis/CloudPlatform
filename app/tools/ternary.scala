package tools

import java.io.File

import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, MyFile, MyMapTool, MyStringTool, Utils}

import scala.collection.JavaConverters._
import scala.collection.mutable

object ternary extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val tableFile=new File(dutyDir,"table.txt")
    val file1=request.body.file("table1").get
    file1.ref.getPath.moveToFile(tableFile)
    val groupFile=new File(dutyDir,"group.txt")
    val file2=request.body.file("table2").get
    file2.ref.getPath.moveToFile(groupFile)
    val tagFile=new File(dutyDir,"tag.txt")
    val file3=request.body.file("table3").get
    file3.ref.getPath.moveToFile(tagFile)

    val psz = if(params("psz") == "0") "none" else if(params("psz") == "1") "log2" else "log10"

    val elements= Json.obj("color"->"#CD0000:#3A89CC:#769C30:#D99536:#7B0078:#BFBC3B:#E2609F:#00688B:#C10077:#4daf4a:#984ea3:#a65628:#999999",
      "psz"->params("psz"), "ml"->"", "mlpos"->"0.5", "xl"->"", "yl"->"", "zl"->"", "ll"->"", "width"->"7",
      "height"->"7", "dpi"->"300").toString()

    try {
      val command = "Rscript "+Utils.path+"R/ternary/ternary.R -i " + tableFile.getAbsolutePath +
        " -g " + groupFile.getAbsolutePath + " -t " + tagFile.getAbsolutePath + " -o " + dutyDir +
        "/out -in ternary -if pdf -psz " + params("psz")

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/ternary.pdf",dutyDir+"/out/ternary.png")
        Utils.pdf2Png(dutyDir+"/out/ternary.pdf",dutyDir+"/out/ternary.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "三元图", file1.filename + "/" + file2.filename + "/" + file3.filename,"点大小转换：" + psz,elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val group=FileUtils.readLines(new File(dutyDir+"/tag.txt")).asScala.map(_.split("\t").last).tail.distinct.sorted
    val color = elements("color").split(":")
    Json.obj("elements"->elements,"color"->color,"group"->group)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")
    val tagFile=new File(dutyDir,"tag.txt")
    
    val newelements= Json.obj("color"->elements("color"), "psz"->elements("psz"), "ml"->elements("ml"),
      "mlpos"->elements("mlpos"), "xl"->elements("xl"), "yl"->elements("yl"), "zl"->elements("zl"), "ll"->elements("ll"),
      "width"->elements("width"), "height"->elements("height"), "dpi"->elements("dpi")).toString()

    val xl=if(elements("xl")=="") "" else " -xl \"" + elements("xl") + "\""
    val yl=if(elements("yl")=="") "" else " -yl \"" + elements("yl") + "\""
    val zl=if(elements("zl")=="") "" else " -zl \"" + elements("zl") + "\""
    val ml=if(elements("ml")=="") " " else elements("ml")
    val ll=if(elements("ll")=="") "" else " -ll \"" + elements("ll") + "\""

    val command = "Rscript "+Utils.path+"R/ternary/ternary.R -i " + tableFile.getAbsolutePath +
      " -g " + groupFile.getAbsolutePath + " -t " + tagFile.getAbsolutePath + " -o " + dutyDir +
      "/out -in ternary -if pdf -pcs \"" + elements("color") + "\" -psz " + elements("psz") + " -ml \"" + ml +
      ":" + elements("mlpos") + "\"" + xl + yl + zl + ll + " -is " + elements("width") + ":" + elements("height") +
      " -dpi " + elements("dpi")

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")
    println(command)

    var valid = ""
    var pics = ""
    var msg = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/ternary.pdf",dutyDir+"/out/ternary.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/ternary.pdf",dutyDir+"/out/ternary.tiff") //替换图片
      pics=dutyDir+"/out/ternary.png"
      valid = "true"
    } else {
      valid = "false"
      msg = execCommand.getErrStr
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements, "message"->msg)
  }

}
