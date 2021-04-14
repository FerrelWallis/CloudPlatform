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

object tree extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val isgroup = (params("isgrouptext") == "TRUE")
    val groups = params("groups")
    val tableFile=new File(dutyDir,"table.tre")
    val groupFile=new File(dutyDir,"group.txt")
    val file1 = request.body.file("table1").get
    file1.ref.getPath.moveToFile(tableFile)
    val file2 = request.body.file("table2")
    val (input,group)=if(isgroup) {
      file2.get.ref.getPath.moveToFile(groupFile)
      (file1.filename+"/"+file2.get.filename," -g " + groupFile.getAbsolutePath)
    }else (file1.filename,"")

    val elements=Json.obj("groups"->groups,"width"->"20","height"->"23","dpi"->"300", "color"->"#000000:#3A89CC:#769C30:#CD0000:#D99536:#7B0078:#BFBC3B:#6E8B3D:#00688B:#C10077:#CAAA76:#474700:#458B00:#8B4513:#008B8B:#6E8B3D:#8B7D6B:#7FFF00:#CDBA96:#ADFF2F",
      "bw"->"2","fs"->"12","ss"->"1","lsa_width"->"2","lsa_height"->"1","lfs"->"26","ln"->"1","sl"->"TRUE",
      "ssb"->"FALSE").toString()

    try {
      val command = "Rscript " + Utils.path+"R/tree/tree2.0.R -i " + tableFile.getAbsolutePath + group +
        " -o " + dutyDir + "/out -if pdf -in treemap"

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/treemap.pdf",dutyDir+"/out/treemap.png")
        Utils.pdf2Png(dutyDir+"/out/treemap.pdf",dutyDir+"/out/treemap.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg,"树状图", input, "/", elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val groupcolo=if(elements("groups")==",,") Array("0") else "0"+:elements("groups").split(",").toArray
    val color=elements("color").split(":")
    Json.obj("elements"->elements,"groups"->elements("groups"),"groupcolo"->groupcolo,"color"->color)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String], oldelements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val tableFile=new File(dutyDir,"table.tre")
    val groupFile=new File(dutyDir,"group.txt")

    val newelements=Json.obj("groups"->oldelements("groups"),"width"->elements("width"),"height"->elements("height"),
      "dpi"->elements("dpi"),"color"->elements("color"),"bw"->elements("bw"),"fs"->elements("fs"),"ss"->elements("ss"),
      "lsa_width"->elements("lsa_width"),"lsa_height"->elements("lsa_height"),"lfs"->elements("lfs"),"ln"->elements("ln"),
      "sl"->elements("sl"),"ssb"->elements("ssb")).toString()

    val group=if(groupFile.exists()) " -g " + groupFile.getAbsolutePath else ""

    val command = "Rscript " + Utils.path+"R/tree/tree2.0.R -i " + tableFile.getAbsolutePath + group +
      " -o " + dutyDir + "/out -if pdf -in treemap -is " + elements("width") + ":" + elements("height") + " -cs \"" +
      elements("color") + "\" -dpi " + elements("dpi") + " -bw " + elements("bw") + " -fs " + elements("fs") + " -ss " +
      elements("ss") + " -lsa " + elements("lsa_width") + ":" + elements("lsa_height") + " -lfs " + elements("lfs") +
      " -ln " + elements("ln") + " -sl " + elements("sl") + " -ssb " + elements("ssb")

    println(command)

    //先放入sh，在运行
    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    var valid = ""
    var pics = ""
    var msg = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/treemap.pdf",dutyDir+"/out/treemap.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/treemap.pdf",dutyDir+"/out/treemap.tiff") //替换图片
      pics=dutyDir+"/out/treemap.png"
      valid = "true"
    } else {
      valid = "false"
      msg = execCommand.getErrStr
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements, "message"->msg)
  }

}