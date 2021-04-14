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

object cca extends MyFile with MyStringTool{

  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"
    val otuFile=new File(dutyDir,"otu.txt")
    val enviFile=new File(dutyDir,"envi.txt")
    val groupFile=new File(dutyDir,"group.txt")
    val file1=request.body.file("table1").get
    val file2=request.body.file("table2").get
    val (input,co)=
      if(params("isgrouptext") == "TRUE")
        (file1.filename+"/"+file2.filename+"/"+request.body.file("table3").get.filename,"#336666:#996633:#CCCC33:#336633:#990033:#FFCC99:#333366:#669999:#996600")
      else (file1.filename+"/"+file2.filename,"#1E90FF")

    val param= "分析类型：" + params("anatype")
    val (xdata,ydata)=
      if(params("anatype")=="RDA") ("RDA1","RDA2") else ("CCA1","CCA2")

    val elements=
      Json.obj("xdata"->xdata,"ydata"->ydata,"xaxis"->"0","yaxis"->"0","samsize"->"6",
        "color"->co,"showsname"->"true","samfont"->"7","showevi"->"true","color1"->"#E41A1C",
        "evifont"->"7","color2"->"#E41A1C","showspeci"->"true","specifont"->"7","specisize"->"6",
        "color3"->"#FF8C00","width"->"15","height"->"15","dpi"->"300","xts"->"16","yts"->"16",
        "xls"->"18","yls"->"18","lts"->"15","lms"->"19","ms"->"12","mstext"->"").toString()

    file1.ref.getPath.moveToFile(otuFile)
    file2.ref.getPath.moveToFile(enviFile)

    try {
      val command1 = "Rscript "+Utils.path+"R/cca/rda_cca_data.R -pi "+ otuFile.getAbsolutePath +
        " -ei " + enviFile.getAbsoluteFile + " -o " + dutyDir + "/out" + " -m " + params("anatype")

      val group=
        if(params("isgrouptext") == "TRUE"){
          val file3 = request.body.file("table3").get
          file3.ref.getPath.moveToFile(groupFile)
          " -g "+groupFile.getAbsolutePath
        }else ""

      val command2 = "Rscript "+Utils.path+"R/cca/rda_cca_plot.R -sai "+ dutyDir + "/out/samples.xls" +
        " -spi " + dutyDir + "/out/species.xls" + " -ei " + dutyDir + "/out/envi.xls" + " -pci " +
        dutyDir + "/out/percent.xls" + " -o " + dutyDir + "/out" + " -is 15:15" + group

      println(command1)
      println(command2)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command1+" && \n"+command2)
      val execCommand = new ExecCommand
      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/rdacca.pdf",dutyDir+"/out/rdacca.png")
        Utils.pdf2Png(dutyDir+"/out/rdacca.pdf",dutyDir+"/out/rdacca.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "CCA/RDA", input, param, elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val color=elements("color").split(":")
    val head=FileUtils.readFileToString(new File(dutyDir+"/otu.txt")).trim.split("\n")
    val gnum=head(0).trim.split("\t").drop(1).length
    val group=
      if(new File(dutyDir+"/group.txt").exists()) {
        val f=FileUtils.readLines(new File(dutyDir+"/group.txt")).asScala
        val g=f.map{_.split('\t').last}.distinct.drop(1)
        if(f.map{_.split('\t').head}.drop(1).length<gnum) g.append("nogroup")
        g.toArray
      }else Array("无分组")
    val data=FileUtils.readLines(new File(dutyDir+"/out/samples.xls"))
    val col=data.get(0).split("\"").filter(_.trim!="").map(_.trim)
    Json.obj("group"->group,"cols"->col,"elements"->elements,"color"->color)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val groupFile=new File(dutyDir,"group.txt")
    val newelements=
      Json.obj("xdata"->elements("xdata"),"ydata"->elements("ydata"),"xaxis"->elements("xaxis"),
        "yaxis"->elements("yaxis"),"samsize"->elements("samsize"),"color"->elements("color"),
        "showsname"->elements("showsname"),"samfont"->elements("samfont"),"showevi"->elements("showevi"),
        "color1"->elements("color1"),"evifont"->elements("evifont"),"color2"->elements("color2"),
        "showspeci"->elements("showspeci"),"specifont"->elements("specifont"),"specisize"->elements("specisize"),
        "color3"->elements("color3"),"width"->elements("width"),"height"->elements("height"),"dpi"->elements("dpi"),
        "xts"->elements("xts"),"yts"->elements("yts"),"xls"->elements("xls"),"yls"->elements("yls"),"lts"->elements("lts"),
        "lms"->elements("lms"),"ms"->elements("ms"),"mstext"->elements("mstext")).toString()

    val xyr=elements("xdata").substring(3) + ":" + elements("ydata").substring(3)
    val sname=
      if(elements("showsname") == "true") " -sspt \"TRUE:TRUE\"" else " -sspt \"TRUE:FALSE\""
    val sat=
      if(!groupFile.exists&&elements("color")!="")
        " -sat \"" + elements("color") + ":" + elements("samfont") + "\""
      else " -sat \"#1E90FF:" + elements("samfont") + "\""
    val gc=if(groupFile.exists) " -gc \"" + elements("color") + "\"" else ""
    val group=
      if(groupFile.exists) " -g " + groupFile.getAbsolutePath else ""
    val evi=
      if(elements("showevi") == "true") " -sepl \"TRUE:TRUE\"" else " -sepl \"FALSE:FALSE\""
    val speci=
      if(elements("showspeci") == "true") " -sppt \"TRUE:TRUE\"" else " -sppt \"FALSE:FALSE\""

    val ms=if(!elements("mstext").equals("")) " -ms \"sans:bold.italic:" + elements("ms") + ":" + elements("mstext") + "\"" else ""

    val command =
      "Rscript "+Utils.path+"R/cca/rda_cca_plot.R -sai "+ dutyDir + "/out/samples.xls -spi " + dutyDir +
        "/out/species.xls -ei " + dutyDir + "/out/envi.xls -pci " + dutyDir + "/out/percent.xls -o " +
        dutyDir + "/out" + group + gc + " -is \"" + elements("width") + ":" + elements("height") + "\" -xyr \"" + xyr + "\" -op \"" +
        elements("xaxis") + ":" + elements("yaxis") + "\"" + sname + " -sap \"#000000:" + elements("samsize") + "\"" + sat +
        evi + " -ett \"" + elements("color1") + ":" + elements("evifont") + "\" -elc \"" + elements("color2") + "\"" +
        speci + " -spp \"" + elements("color3") + ":" + elements("specisize") + "\" -spt \"" + elements("color3") + ":" +
        elements("specifont") + "\" -dpi \"" + elements("dpi") + "\" -xts  \"sans:bold.italic:" + elements("xts") +
        "\" -yts  \"sans:bold.italic:" + elements("yts") + "\" -xls  \"sans:bold.italic:" + elements("xls") +
        "\" -yls  \"sans:bold.italic:" + elements("yls") + "\" -lts \"sans:bold.italic:" + elements("lts") +
        "\" -lms \"sans:bold.italic:" + elements("lms") + "\"" + ms

    println(command)

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    var valid = ""
    var pics = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/rdacca.pdf",dutyDir+"/out/rdacca.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/rdacca.pdf",dutyDir+"/out/rdacca.tiff") //替换图片
      pics=dutyDir+"/out/rdacca.png"
      valid = "true"
    } else {
      valid = "false"
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements)
  }


}
