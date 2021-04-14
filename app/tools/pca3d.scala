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

object pca3d extends MyFile with MyStringTool{

  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")
    val file1 = request.body.file("table1").get
    file1.ref.getPath.moveToFile(tableFile)
    val file2 = request.body.file("table2")
    val (group,filepara)=if(params("isgrouptext") == "TRUE") {
      file2.get.ref.getPath.moveToFile(groupFile)
      (" -g " + groupFile.getAbsolutePath,file1.filename+"/"+file2.get.filename)
    } else ("",file1.filename)

    val elements=Json.obj("color"->"#CD0000:#3A89CC:#769C30:#D99536:#7B0078:#BFBC3B:#E2609F:#00688B:#C10077:#CAAA76:#EEEE00:#458B00:#8B4513:#008B8B:#6E8B3D:#8B7D6B:#7FFF00:#CDBA96:#ADFF2F",
      "ac"->"black", "gc"->"lightgrey", "xdata"->"PC1", "ydata"->"PC2", "zdata"->"PC3", "ps"->"2", "an"->"30",
      "as"->"1.1", "ls"->"1.5", "les"->"1.2", "lec"->"1", "mt"->"", "sy"->"1", "xlmin"->"", "xlmax"->"",
      "ylmin"->"","ylmax"->"", "zlmin"->"", "zlmax"->"", "width"->"12", "height"->"12", "lep" -> "topleft",
      "llt"->"FALSE", "lls"->"0.7", "llp"->"3").toString()
    
    try {
      val command = "Rscript " + Utils.path + "R/pca/pca_data.R -i " + tableFile.getAbsolutePath +
        " -o " + dutyDir + "/out" + " -sca TRUE && \n Rscript " + Utils.path + "R/3dpca/pca_3d_plot.R -pc " + dutyDir+"/out/pca.x.xls -sd " +
        dutyDir + "/out/pca.sdev.xls" + group + " -o " + dutyDir + "/out"

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"), command)
      val execCommand = new ExecCommand
      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/pca_3d.pdf",dutyDir+"/out/pca_3d.png")
        Utils.pdf2Png(dutyDir+"/out/pca_3d.pdf",dutyDir+"/out/pca_3d.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "三维PCA", filepara, "/", elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val color=elements("color").split(":")
    val head=FileUtils.readFileToString(new File(dutyDir+"/table.txt")).trim.split("\n")
    val gnum=head(0).trim.split("\t").drop(1).length
    val group=
      if(new File(dutyDir+"/group.txt").exists()) {
        val f=FileUtils.readLines(new File(dutyDir+"/group.txt")).asScala
        f.map{_.split('\t').last}.distinct.toArray
      }else Array("nogroup")
    val col=FileUtils.readLines(new File(dutyDir+"/out/pca.x.xls")).get(0).split("\"").filter(_.trim!="").map(_.trim)
    Json.obj("group"->group,"cols"->col,"elements"->elements,"color"->color)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val groupFile=new File(dutyDir,"group.txt")
    val group=if(groupFile.exists()) " -g " + groupFile.getAbsolutePath else ""

    val newelements=Json.obj("color"->elements("color"), "ac"->elements("ac"), "gc"->elements("gc"), "xdata"->elements("xdata"),
      "ydata"->elements("ydata"), "zdata"->elements("zdata"), "ps"->elements("ps"), "an"->elements("an"), "as"->elements("as"), "ls"->elements("ls"),
      "les"->elements("les"), "lec"->elements("lec"), "mt"->elements("mt"), "sy"->elements("sy"), "xlmin"->elements("xlmin"), "xlmax"->elements("xlmax"),
      "ylmin"->elements("ylmin"),"ylmax"->elements("ylmax"), "zlmin"->elements("zlmin"), "zlmax"->elements("zlmax"), "width"->elements("width"),
      "height"->elements("height"), "lep" -> elements("lep"), "llt"->elements("llt"), "lls"->elements("lls"), "llp"->elements("llp")).toString()

    val xlim = if(elements("xlmin") == "") "" else " -xl " + elements("xlmin") + ":" + elements("xlmax")
    val ylim = if(elements("ylmin") == "") "" else " -yl " + elements("ylmin") + ":" + elements("ylmax")
    val zlim = if(elements("zlmin") == "") "" else " -zl " + elements("zlmin") + ":" + elements("zlmax")

    val command = "Rscript " + Utils.path + "R/3dpca/pca_3d_plot.R -pc " + dutyDir+"/out/pca.x.xls -sd " +
      dutyDir + "/out/pca.sdev.xls" + group + " -o " + dutyDir + "/out -cs \"" + elements("color") + "\" -ac \"" + elements("ac") +
      "\" -gc \"" + elements("gc") + "\" -xyz " + elements("xdata") + ":" + elements("ydata") + ":" + elements("zdata") + " -ps " + elements("ps") +
      " -an " + elements("an") + " -as " + elements("as") + " -ls " + elements("ls") + " -les " + elements("les") + " -lec " + elements("lec") +
      " -mt \"" + elements("mt") + "\" -sy " + elements("sy") + xlim + ylim + zlim + " -is " + elements("height") + ":" + elements("width") +
      " -lep " + elements("lep") + " -llt " + elements("llt") + " -lls " + elements("lls") + " -llp " + elements("llp")

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    println(command)
    
    var valid = ""
    var pics = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/pca_3d.pdf",dutyDir+"/out/pca_3d.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/pca_3d.pdf",dutyDir+"/out/pca_3d.tiff") //替换图片
      pics=dutyDir+"/out/pca_3d.png"
      valid = "true"
    } else {
      valid = "false"
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements)
  }


}
