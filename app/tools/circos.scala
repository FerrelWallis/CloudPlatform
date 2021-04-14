package tools

import java.io.File

import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, MyFile, MyMapTool, MyStringTool, Utils}

import scala.collection.JavaConverters._
import scala.collection.mutable

object circos extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val l = (params("l") == "TRUE")
    val o = (params("o") == "TRUE")
    val filenum = params("filenum")
    val file1=request.body.file("fttable").get
    val tableFile=new File(dutyDir,file1.filename)
    file1.ref.getPath.moveToFile(tableFile)

    var input=file1.filename
    val filelist=mutable.Buffer(file1.filename)
    val fttable=FileUtils.readLines(tableFile).asScala
    val ftbgcolor=
      if(fttable.head.split("\t").length==3) {
        fttable.map{line=>
          line.split("\t").last
        }.tail.mkString(":")
      }else "#8DD3C7:#FFFFB3:#BEBADA:#FB8072:#80B1D3:#FDB462:#B3DE69:#FCCDE5"

    val file2=request.body.file("cdtable")

    val (cd,cdname)=if(l==false) ("","") else {
      input+="/"+file2.get.filename
      file2.get.ref.getPath.moveToFile(new File(dutyDir,file2.get.filename))
      (" -cd " + dutyDir + "/" + file2.get.filename,file2.get.filename)
    }

    val otfile=if(o==false) "" else{
      " -ot \""+(1 to filenum.toInt).map{x=>
        val file=request.body.file("table"+x)
        input+="/"+file.get.filename
        file.get.ref.getPath.moveToFile(new File(dutyDir,file.get.filename))
        filelist+=file.get.filename
        dutyDir+"/"+file.get.filename
      }.mkString(";") + "\""
    }

    val otcolor=filelist.tail.map { x =>
      if (FileUtils.readLines(new File(dutyDir + "/" + x)).asScala(0).split("\t").length == 6) {
        FileUtils.readLines(new File(dutyDir + "/" + x)).asScala(1).split("\t").last.split("-").mkString(":")
      } else "#8DD3C7:#FFFFB3:#BEBADA"
    }.mkString("-")

    val param="上传基因组或基因之间共线性关系：" + l.toString + "/上传基因或基因组窗口：" + o.toString + "/是否绘制基因组窗口详细信息：" + params("sl")

    val elements= Json.obj("rtot"->filelist.mkString(","), "cd"->cdname, "fttagcolor"->"black",
      "ftheight"->"0.1", "ftbgcolor"->ftbgcolor, "otcolor"->otcolor, "fttagfont"->"1", "sl"->params("sl"),
      "kssize"->"1", "ksfont"->"0.7", "kscolor"->"black", "stl"->"FALSE", "ch"->"0.5", "cbsgap"->"2",
      "cbsdegree"->"90", "ottype"->"l", "blt"->"0", "highcolor"->"#984EA3", "highname"->"",
      "hightrack"->"","high"->"false").toString()

    try {
      val command = "Rscript "+Utils.path+"R/circos/plot_circle-4.R -ft "+ tableFile.getAbsolutePath + otfile + cd +
        " -o " +dutyDir+"/out -if pdf -sl " + params("sl") + " -blt 0"

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/circle.pdf",dutyDir+"/out/circle.png")
        Utils.pdf2Png(dutyDir+"/out/circle.pdf",dutyDir+"/out/circle.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "Circos圈图", input, param, elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val files=elements("rtot").split(",")
    val mainfilename=files(0)
    val chr=FileUtils.readLines(new File(dutyDir+"/"+mainfilename)).asScala.map{line=>line.split("\t").head}.tail
    val otnum=if(elements("otcolor")=="") Array()
    else elements("otcolor").split("-").map{x=>
      if(x=="/") 0
      else x.split(":").length
    }
    val highname=elements("highname")
    val hightrack=elements("hightrack")
    val high=elements("high")
    val colornums = elements("ftbgcolor").split(":").length +: otnum
    val colorlists = (elements("ftbgcolor") + "-" + elements("otcolor")).split("-")
    Json.obj("elements"->elements,"chr"->chr,"files"->files,"colornums"->colornums,"colorlists"->colorlists,"highname"->highname,"hightrack"->hightrack,"high"->high)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String], oldelements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val showha = (elements("showha") == "TRUE")
    val rtot=oldelements("rtot")
    val rt=rtot.split(",").head
    val ot=rtot.split(",").tail
    val cdname=oldelements("cd")
    val colors=elements("colors").split("-")
    val ftbgcolor=colors.head
    val otcolor=colors.tail.mkString("-")

    val newelements= Json.obj("rtot"->rtot, "cd"->cdname, "fttagcolor"->elements("fttagcolor"),
      "ftheight"->elements("ftheight"), "ftbgcolor"->ftbgcolor, "otcolor"->otcolor,
      "fttagfont"->elements("fttagfont"), "sl"->elements("sl"), "kssize"->elements("kssize"), "ksfont"->elements("ksfont"),
      "kscolor"->elements("kscolor"), "stl"->elements("stl"), "ch"->elements("ch"), "cbsgap"->elements("cbsgap"),
      "cbsdegree"->elements("cbsdegree"), "ottype"->elements("ottype"), "blt"->elements("blt"),
      "highcolor"->elements("highcolor"), "highname"->elements("highname"), "hightrack"->elements("hightrack"),
      "high"->showha.toString).toString()

    val otc=colors.tail.map(_.split(":").mkString(","))
    val otfile=if(ot.isEmpty) "" else{
      " -ot \""+(0 to ot.length-1).map{x=>
        val otpath=dutyDir+"/"+ot(x)
        otpath+"@NULL@"+otc(x)+"@"+elements("ottype")
      }.mkString(";") + "\""
    }
    val ftfile = " -ft \"" + dutyDir + "/" + rt + "@" + elements("fttagcolor") + "@" + elements("ftheight") +
      "@" + ftbgcolor.split(":").mkString(",") + "@NULL@" + elements("fttagfont") + "\""
    val cdfile = if(cdname=="") "" else " -cd " + dutyDir + "/" + cdname

    val highname=elements("highname").split(":")
    val hightrack=elements("hightrack").split(":")
    val ha = if(showha) " -ha \"" + (0 to (highname.length-1)).map{x=>
      highname(x)+"@"+hightrack(x)+"@"+elements("highcolor")}.mkString(";")+"\"" else ""

    val command = "Rscript "+Utils.path+"R/circos/plot_circle-4.R"+ ftfile + otfile + cdfile +
      " -o " +dutyDir+"/out -if pdf -sl " + elements("sl") + " -ks \"" + elements("kssize") + ":" + elements("ksfont") +
      ":" + elements("kscolor") + "\" -stl " + elements("stl") + " -ch " + elements("ch") + " -cbs \"" + elements("cbsgap") +
      ":" + elements("cbsdegree") + "\" -blt " + elements("blt") + ha

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")
    println(command)
    
    var valid = ""
    var pics = ""
    var msg = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/circle.pdf",dutyDir+"/out/circle.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/circle.pdf",dutyDir+"/out/circle.tiff") //替换图片
      pics=dutyDir+"/out/circle.png"
      valid = "true"
    } else {
      valid = "false"
      msg = execCommand.getErrStr
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements, "message"->msg)
  }


}
