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

object violin extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val isgroup = (params("isgrouptext") == "TRUE")
    val table = params("tablenum")
    val group = params("groupnum")
    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")

    val input=
      if(table=="2") {
        val file1=request.body.file("table1").get
        file1.ref.getPath.moveToFile(tableFile)
        if(isgroup && group=="2") file1.filename+"/"+ request.body.file("table2").get.filename
        else file1.filename
      } else{
        FileUtils.writeStringToFile(tableFile, params("txdata1").trim.reformFile)
        if(isgroup && group=="2") request.body.file("table2").get.filename
        else "无"
      }

    val (unpara,un)=if(params("un")=="") ("all","") else (params("un")," -un " + params("un"))

    val param=
      if(isgroup) ("是否分组绘图：" + isgroup + "/读取数据行数：" + unpara + "/图片展示方式：" + params("fp"))
      else ("是否分组绘图：" + isgroup + "/读取数据行数：" + unpara + "/图片展示方式：" + params("fp"))

    val elements= Json.obj("un"->params("un"), "bw"->"0.1", "color"->"#B2182B:#E69F00:#56B4E9:#009E73:#F0E442:#0072B2:#D55E00:#CC79A7:#CC6666:#9999CC:#66CC99:#999999:#ADD1E5",
      "yrmin"->"", "yrmax"->"", "fp"->params("fp"), "xts"->"18", "yts"->"16", "xls"->"20", "yls"->"20", "xtext"->"",
      "ytext"->"", "sts"->"16", "lts"->"14", "dpi"->"300", "width" -> "20", "height"->"20").toString()
    
    try {
      val groupdata=
        if(isgroup){
          if(group == "2"){
            val file = request.body.file("table2").get
            val groupdatas = FileUtils.readFileToString(file.ref.file).trim
            FileUtils.writeStringToFile(groupFile, ("#SampleID\tGroup\n"+groupdatas).reformFile)
          }else{
            FileUtils.writeStringToFile(groupFile, ("#SampleID\tGroup\n"+params("txdata2").trim).reformFile)
          }
          " -g "+groupFile.getAbsolutePath
        }else ""

      val command = "Rscript "+Utils.path+"R/violin/violin.R -i "+ tableFile.getAbsolutePath + groupdata +
        " -o " +dutyDir+"/out -if pdf " + un + " -fp " + params("fp")

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/violin.pdf",dutyDir+"/out/violin.png")
        Utils.pdf2Png(dutyDir+"/out/violin.pdf",dutyDir+"/out/violin.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg,"小提琴图", input, param, elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val color=elements("color").split(":")
    val head=FileUtils.readFileToString(new File(dutyDir+"/table.txt")).trim.split("\n")
    val group=head(0).trim.split("\t").drop(1)
    val hasgroup=if(new File(dutyDir+"/group.txt").exists()) "TRUE" else "FALSE"
    Json.obj("group"->group,"elements"->elements,"color"->color,"hasgroup"->hasgroup)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")

    val newelements= Json.obj("un"->elements("un"), "bw"->elements("bw"), "color"->elements("color"),
      "yrmin"->elements("yrmin"), "yrmax"->elements("yrmax"), "fp"->elements("fp"), "xts"->elements("xts"),
      "yts"->elements("yts"), "xls"->elements("xls"), "yls"->elements("yls"), "xtext"->elements("xtext"),
      "ytext"->elements("ytext"), "sts"->elements("sts"), "lts"->elements("lts"),"dpi"->"300",
      "width"->"20", "height"->"20").toString()

    val un=if(elements("un")=="") "" else " -un " + elements("un")
    val groupdata=if(groupFile.exists()) " -g " + groupFile.getAbsolutePath else ""
    val yr=if(elements("yrmin")=="" || elements("yrmax")=="") "" else " -yr " + elements("yrmin") + ":" + elements("yrmax")
    val xtext=if(elements("xtext")=="") " " else elements("xtext")
    val ytext=if(elements("ytext")=="") " " else elements("ytext")

    val command = "Rscript "+Utils.path+"R/violin/violin.R -i "+ tableFile.getAbsolutePath +
      groupdata + " -o " +dutyDir+"/out" + un + " -bw " + elements("bw") + " -cs \"" + elements("color") +
      "\" -fp " + elements("fp") + yr + " -xts sans:bold.italic:" + elements("xts") + " -yts sans:bold.italic:" +
      elements("yts") + " -xls sans:bold.italic:" + elements("xls") + ":\"" + xtext +
      "\" -yls sans:bold.italic:" + elements("yls") + ":\"" + ytext + "\" -sts sans:bold.italic:" +
      elements("sts") + " -lts sans:plain:" + elements("lts") + " -is " + elements("width") + ":" + elements("height") +
      " -dpi " + elements("dpi") + " -if pdf"

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    println(command)

    var valid = ""
    var pics = ""
    var msg = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/violin.pdf",dutyDir+"/out/violin.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/violin.pdf",dutyDir+"/out/violin.tiff") //替换图片
      pics=dutyDir+"/out/violin.png"
      valid = "true"
    } else {
      valid = "false"
      msg = execCommand.getErrStr
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements, "message"->msg)
  }

}