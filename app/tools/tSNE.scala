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

object tSNE extends MyFile with MyStringTool{

  def Run(dutyDir: String)(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")
    val file1 = request.body.file("table1").get
    file1.ref.getPath.moveToFile(tableFile)
    val file2 = request.body.file("table2").get
    file2.ref.getPath.moveToFile(groupFile)
    val filepara = file1.filename + "/" + file2.filename

    val elements= Json.obj("width"->"15","length"->"12", "color"->"#CD0000:#3A89CC:#769C30:#D99536:#7B0078:#BFBC3B:#E2609F:#00688B:#C10077:#CAAA76:#EEEE00:#458B00:#8B4513:#008B8B:#6E8B3D:#8B7D6B:#7FFF00:#CDBA96:#ADFF2F",
      "resolution"->"300","xts"->"16","yts"->"16","xls"->"18","yls"->"18","ltes"->"16","lts"->"18","ltstext"->"","ts"->"20",
      "tstext"->"","big"->"no", "xdamin"->"","xdamax"->"","ydamin"->"","ydamax"->"").toString()

    try {
      val command1 = "Rscript " + Utils.path + "R/tsne/tsne.R -i " + tableFile.getAbsolutePath + " -g " +
        groupFile.getAbsolutePath + " -o " + dutyDir + "/out"
      val command2 = "Rscript " + Utils.path + "R/tsne/tsne-plot.R -i " + dutyDir + "/out/tsne.xls -g " +
        groupFile.getAbsolutePath + " -o " + dutyDir + "/out"

      println(command1 + "\n" + command2)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command1 + " &&\n" + command2)
      val execCommand = new ExecCommand
      execCommand.exect(Array(command1,command2),new File(dutyDir+"/temp"))

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/tsne.pdf",dutyDir+"/out/tsne.png")
        Utils.pdf2Png(dutyDir+"/out/tsne.pdf",dutyDir+"/out/tsne.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "tSNE", filepara, "/", elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val color=elements("color").split(":")
    val f=FileUtils.readLines(new File(dutyDir+"/group.txt")).asScala
    val group= f.map{_.split('\t').last}.distinct.toArray.sorted
    Json.obj("group"->group,"elements"->elements,"color"->color)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val tableFile=new File(dutyDir,"out/tsne.xls")
    val groupFile=new File(dutyDir,"group.txt")

    val newelements= Json.obj("width"->elements("width"),"length"->elements("length"),"color"->elements("color"),
      "resolution"->elements("resolution"),"xts"->elements("xts"),"yts"->elements("yts"),"xls"->elements("xls"),
      "yls"->elements("yls"),"ltes"->elements("ltes"),"lts"->elements("lts"),"ltstext"->elements("ltstext"),
      "ts"->elements("ts"),"tstext"->elements("tstext"),"big"->elements("big"),"xdamin"->elements("xdamin"),
      "xdamax"->elements("xdamax"),"ydamin"->elements("ydamin"),"ydamax"->elements("ydamax")).toString()

    val big=if(elements("big")=="no") ""
    else if(elements("big")=="x")
      " -da x:"+elements("xdamin")+","+elements("xdamax")
    else " -da y:"+elements("ydamin")+","+elements("ydamax")

    val lts=if(!elements("ltstext").equals("")) " -lts sans:bold:" + elements("lts") + ":\"" + elements("ltstext")+"\"" else ""
    val ts=if(!elements("tstext").equals("")) " -ts sans:bold:" + elements("ts") + ":\"" + elements("tstext")+"\"" else ""

    val command = "Rscript " + Utils.path + "R/tsne/tsne-plot.R -i " + tableFile.getAbsolutePath + " -g " +
      groupFile.getAbsolutePath + " -o " + dutyDir + "/out" + " -is "+ elements("width") + ":" +
      elements("length") + " -cs \"" + elements("color") + "\"" + " -dpi " + elements("resolution") + " -xts " +
      "sans:plain:" + elements("xts") + " -yts " + "sans:plain:" + elements("yts") + " -xls " + "sans:bold:" +
      elements("xls") + ":tSNE1 -yls " + "sans:bold:" + elements("yls") + ":tSNE2 -ltes " + "sans:plain:" + elements("ltes") +
      " -if pdf" + lts + ts + big

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    println(command)

    var valid = ""
    var pics = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/tsne.pdf",dutyDir+"/out/tsne.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/tsne.pdf",dutyDir+"/out/tsne.tiff") //替换图片
      pics=dutyDir+"/out/tsne.png"
      valid = "true"
    } else {
      valid = "false"
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements)
  }


}
