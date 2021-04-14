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

object nmds extends MyFile with MyStringTool{

  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val isgroup = params("isgrouptext")

    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")
    val file1 = request.body.file("table1").get
    file1.ref.getPath.moveToFile(tableFile)
    val file2 = request.body.file("table2")
    val (group,filepara)=if(isgroup  == "TRUE") {
      file2.get.ref.getPath.moveToFile(groupFile)
      (" -g " + groupFile.getAbsolutePath,file1.filename + "/" + file2.get.filename)
    } else ("",file1.filename)

    val param = "计算距离方法：" + params("m")
    val co = if(isgroup  == "TRUE")
      "#CD0000:#3A89CC:#769C30:#D99536:#7B0078:#BFBC3B:#E2609F:#00688B:#C10077:#CAAA76:#EEEE00:#458B00:#8B4513:#008B8B:#6E8B3D:#8B7D6B:#7FFF00:#CDBA96:#ADFF2F"
    else "#48FF75"

    val elements= Json.obj("width"->"15","length"->"12","showname"->"FALSE",
      "showerro"->"TRUE","color"->co,"resolution"->"300","xts"->"15","yts"->"15","xls"->"17","yls"->"17",
      "lts"->"14","lms"->"15","lmtext"->"","ms"->"17","mstext"->"","c"->"FALSE","big"->"no",
      "xdamin"->"","xdamax"->"","ydamin"->"","ydamax"->"").toString()

    try {
      val command =
        "biom convert -i " + tableFile.getAbsolutePath + " -o " + dutyDir + "/temp.biom --table-type=\"OTU table\" --to-json && \n" +
          "beta_diversity.py -i " +  dutyDir + "/temp.biom -o " + dutyDir + "/out -m " + params("m") + " && \n" +
          "Rscript " + Utils.path + "R/nmds/nmds.R -i " + dutyDir + "/out/" + params("m") + "_temp.txt -o " + dutyDir + "/out && \n" +
          "Rscript " + Utils.path + "R/pcoa/pcoa-plot.R -i " + dutyDir + "/out/nmds_sites.xls" + group +
          " -o " +dutyDir+"/out" + " -if pdf -in nmds -pxy MDS1:MDS2"
      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/nmds.pdf",dutyDir+"/out/nmds.png")
        Utils.pdf2Png(dutyDir+"/out/nmds.pdf",dutyDir+"/out/nmds.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "NMDS", filepara, param, elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val color=elements("color").split(":")
    val head=FileUtils.readFileToString(new File(dutyDir+"/table.txt")).trim.split("\n")
    val gnum=head(0).trim.split("\t").drop(1).length
    val group=
      if(new File(dutyDir+"/group.txt").exists()) {
        val f=FileUtils.readLines(new File(dutyDir+"/group.txt")).asScala
        val g=f.map{_.split('\t').last}.distinct.drop(1)
        if(f.map{_.split('\t').head}.drop(1).length<gnum) g.append("nogroup")
        g.toArray
      }else Array("nogroup")
    Json.obj("group"->group,"elements"->elements,"color"->color)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val groupFile=new File(dutyDir,"group.txt")
    val groupdata=if(groupFile.exists()) " -g " + groupFile.getAbsolutePath else ""

    val newelements= Json.obj("width"->elements("width"),"length"->elements("length"),"showname"->elements("showname"),
      "showerro"->elements("showerro"),"color"->elements("color"),"resolution"->elements("resolution"),
      "xts"->elements("xts"),"yts"->elements("yts"),"xls"->elements("xls"),"yls"->elements("yls"),"lts"->elements("lts"),
      "lms"->elements("lms"),"lmtext"->elements("lmtext"),"ms"->elements("ms"),"mstext"->elements("mstext"),"c"->elements("c"),
      "big"->elements("big"),"xdamin"->elements("xdamin"),"xdamax"->elements("xdamax"),"ydamin"->elements("ydamin"),
      "ydamax"->elements("ydamax")).toString()

    val c=
      if(!groupFile.exists()) " -oc \""+elements("color")+"\""
      else " -cs \""+ elements("color")+"\""

    val name=if(elements("showname").equals("TRUE") && groupFile.exists()){
      val f=FileUtils.readLines(groupFile).asScala
      val n=f.map{_.split('\t').last}.distinct.drop(1).mkString(",")
      " -b " + n
    }else if(elements("showname").equals("TRUE") && !groupFile.exists()){
      " -sl TRUE"
    }
    else ""

    val big=if(elements("big")=="no") ""
    else if(elements("big")=="x")
      " -da x:"+elements("xdamin")+","+elements("xdamax")
    else " -da y:"+elements("ydamin")+","+elements("ydamax")

    val lms=if(!elements("lmtext").equals("")) " -lms sans:bold.italic:" + elements("lms") + ":\"" + elements("lmtext")+"\"" else ""
    val ms=if(!elements("mstext").equals("")) " -ms sans:plain:" + elements("ms") + ":\"" + elements("mstext")+"\"" else ""

    val command = "Rscript " + Utils.path + "R/pcoa/pcoa-plot.R -i " + dutyDir + "/out/nmds_sites.xls" +
      groupdata + " -o " + dutyDir + "/out" + " -pxy MDS1:MDS2 -is "+ elements("width") + ":" + elements("length") + c +
      " -dpi " + elements("resolution") + " -xts " + "sans:plain:" + elements("xts") + " -yts " + "sans:plain:" + elements("yts") +
      " -xls " + "sans:plain:" + elements("xls") + " -yls " + "sans:plain:" + elements("yls") + " -lts " + "sans:plain:" +
      elements("lts") + " -if pdf -ss " + elements("showerro") + name + lms + ms + " -c " + elements("c") + big + " -in nmds"

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    println(command)

    var valid = ""
    var pics = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/nmds.pdf",dutyDir+"/out/nmds.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/nmds.pdf",dutyDir+"/out/nmds.tiff") //替换图片
      pics=dutyDir+"/out/nmds.png"
      valid = "true"
    } else {
      valid = "false"
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements)
  }


}