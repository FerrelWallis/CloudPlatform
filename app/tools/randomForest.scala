package tools

import java.io.File

import controllers.{DutyController, RService}
import dao.dutyDao
import org.apache.commons.io.FileUtils
import play.api.libs.Files
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, MyFile, MyStringTool, Utils}
import controllers.RService

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

object randomForest extends MyFile with MyStringTool{

  def Run(dutyDir: String)(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")
    val file1 = request.body.file("table1").get
    file1.ref.getPath.moveToFile(tableFile)
    val file2 = request.body.file("table2").get
    val filepara = file1.filename+"/"+file2.filename
    file2.ref.getPath.moveToFile(groupFile)

    val elements=Json.obj("td"->"15", "color"->"#E41A1C:#9B445D:#526E9F:#3C8A9B:#469F6C:#54A453:#747B78:#94539E:#BD6066:#E97422:#FF990A:#FFCF20:#FAF632:#D4AE2D:#AF6729:#BF6357",
      "dl"->"20", "xts"->"12", "yts"->"12", "xls"->"18", "yls"->"18", "ms"->"22",
      "xtext"->"Metabolite", "ytext"->"VarImp", "mstext"->"", "dpi"->"300",
      "width"->"15", "height"->"15").toString()

    try {
      val command = "Rscript " + Utils.path + "R/randomForest/random.R -i " + tableFile.getAbsolutePath +
        " -g " + groupFile.getAbsolutePath + " -o " + dutyDir + "/out" + " && \n" +
        "Rscript " + Utils.path + "R/randomForest/random_plot.R -i " + dutyDir + "/out/VarImp.xls" +
        " -o " + dutyDir + "/out"

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"), command)
      val execCommand = new ExecCommand
      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/randomForest.pdf",dutyDir+"/out/randomForest.png")
        Utils.pdf2Png(dutyDir+"/out/randomForest.pdf",dutyDir+"/out/randomForest.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "随机森林", filepara, "/", elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val color=elements("color").split(":")
    val file=FileUtils.readLines(new File(dutyDir + "/out/VarImp.xls")).asScala.drop(1)
    val gnum = if(elements("td").toInt > file.length) file.length else elements("td").toInt
    val group = file.take(gnum).map{_.split('\t').head}.toArray.reverse
    Json.obj("group"->group,"elements"->elements,"color"->color)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val tableFile=new File(dutyDir + "/out","VarImp.xls")
    val color = elements("color") + ":#E41A1C:#9B445D:#526E9F:#3C8A9B:#469F6C:#54A453:#747B78:#94539E:#BD6066:#E97422:#FF990A:#FFCF20:#FAF632:#D4AE2D:#AF6729:#BF6357"

    val newelements=Json.obj("td"->elements("td"), "color"->color, "dl"->elements("dl"), "xts"->elements("xts"),
      "yts"->elements("yts"), "xls"->elements("xls"), "yls"->elements("yls"), "ms"->elements("ms"), "xtext"->elements("xtext"),
      "ytext"->elements("ytext"), "mstext"->elements("mstext"), "dpi"->elements("dpi"), "width"->elements("width"),
      "height"->elements("height")).toString()

    val mstext = if(elements("mstext") == "") " " else elements("mstext")

    val command = "Rscript " + Utils.path + "R/randomForest/random_plot.R -i " + dutyDir + "/out/VarImp.xls" +
      " -o " + dutyDir + "/out -td " + elements("td") + " -dl " + elements("dl") + " -cs \"" + color +
      "\" -xts sans:bold.italic:" + elements("xts") + " -yts sans:bold.italic:" + elements("yts") +
      " -xls \"sans:bold.italic:" + elements("xls") + ":" + elements("xtext") + "\" -yls \"sans:bold.italic:" +
      elements("yls") + ":" + elements("ytext") + "\" -ms \"sans:bold.italic:" + elements("ms") + ":" + mstext +
      "\" -dpi " + elements("dpi") + " -is " + elements("width") + ":" + elements("height")

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    println(command)

    val file=FileUtils.readLines(new File(dutyDir + "/out/VarImp.xls")).asScala.drop(1)
    val gnum = if(elements("td").toInt > file.length) file.length else elements("td").toInt
    val group = file.take(gnum).map{_.split('\t').head}.toArray.reverse
    val cc=color.split(":")

    var valid = ""
    var pics = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/randomForest.pdf",dutyDir+"/out/randomForest.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/randomForest.pdf",dutyDir+"/out/randomForest.tiff") //替换图片
      pics=dutyDir+"/out/randomForest.png"
      valid = "true"
    } else {
      valid = "false"
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements, "color"->cc, "group"->group)
  }


}

