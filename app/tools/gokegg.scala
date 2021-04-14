package tools

import java.io.File

import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, MyFile, MyMapTool, MyStringTool, Utils}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object gokegg extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String], abbre:String)(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val tableFile=new File(dutyDir,"table.txt")
    val koFile=new File(dutyDir,"kogo.txt")
    val tablenum = params("tablenum")
    val refer = params("isrefer")
    
    val input=
      if(tablenum=="2"){
        val file1=request.body.file("table1").get
        file1.ref.getPath.moveToFile(tableFile)
        if(refer=="TRUE"){
          file1.filename
        }else{
          val file2=request.body.file("table2").get
          file2.ref.getPath.moveToFile(koFile)
          file1.filename+"/"+file2.filename
        }
      } else {
        FileUtils.writeStringToFile(tableFile, params("txdata1"))
        if(refer=="TRUE"){
          "无"
        }else{
          val file3=request.body.file("table2").get
          file3.ref.getPath.moveToFile(koFile)
          file3.filename
        }
      }

    val (param,model)=
      if(refer=="TRUE") {
        if(abbre=="KEGG") ("使用已有参考:"+refer+"/选择物种："+params("species"),"ko_data.jar -m "+params("species"))
        else ("使用已有参考:"+refer+"/选择物种："+params("species"),"go_data.jar -m "+params("species"))
      }
      else {
        if(abbre=="KEGG") ("使用已有参考:"+refer,"ko_data_file.jar -pathway "+koFile.getAbsolutePath)
        else ("使用已有参考:"+refer,"Go_data_file.jar -go "+koFile.getAbsolutePath)
      }

    val elements= Json.obj("n"->"15","br"->"0.9","g"->"FALSE","sm"->"50","width"->"20",
      "height"->"14", "xts"->"13","yts"->"14","lts"->"15","dpi"->"300").toString()

    val (command1,command2,sname)=
      if(abbre=="KEGG")
        ("java -jar " + Utils.path + "R/gokegg/data/" + model + " -i " + tableFile.getAbsolutePath +
          " -o " + dutyDir + "/out/ko" , "Rscript " + Utils.path + "R/gokegg/plot/Ko_dodge_plot.R -i " + dutyDir +
          "/out/ko.Ko.bar.dat" + " -o " + dutyDir + "/out" + " -in ko_dodge -if pdf -sm 50 -n 15",
          "KEGG富集分析")
      else
        ("java -jar " + Utils.path + "R/gokegg/data/" + model + " -i " + tableFile.getAbsolutePath +
          " -o " + dutyDir + "/out/go" , "Rscript " + Utils.path + "R/gokegg/plot/Go_stack_plot.R -i " + dutyDir +
          "/out/go.Go.bar.dat" + " -o " + dutyDir + "/out" + " -in go_stack -if pdf -sm 50 -n 15",
          "GO富集分析")

    try {
      val command3 = "dos2unix " + tableFile.getAbsolutePath
      val command4 = "dos2unix " + koFile.getAbsolutePath
      val commandpack1= if(koFile.exists()) Array(command3,command4) else Array(command3)
      val commandpack2=Array(command1,command2)

      println(command1)
      println(command2)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),commandpack1.mkString(" && \n")+" && \n"+commandpack2.mkString(" && \n"))
      val execCommand = new ExecCommand
      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      } else {
        if(abbre=="KEGG"){
          Utils.pdf2Png(dutyDir+"/out/ko.Ko.enrich.pdf",dutyDir+"/out/ko.Ko.enrich.png")
          Utils.pdf2Png(dutyDir+"/out/ko.Ko.enrich.pdf",dutyDir+"/out/ko.Ko.enrich.tiff")
          Utils.pdf2Png(dutyDir+"/out/ko_dodge.pdf",dutyDir+"/out/ko_dodge.png")
          Utils.pdf2Png(dutyDir+"/out/ko_dodge.pdf",dutyDir+"/out/ko_dodge.tiff")
        }else {
          Utils.pdf2Png(dutyDir+"/out/go.Go.enrich.pdf",dutyDir+"/out/go.Go.enrich.png")
          Utils.pdf2Png(dutyDir+"/out/go.Go.enrich.pdf",dutyDir+"/out/go.Go.enrich.tiff")
          Utils.pdf2Png(dutyDir+"/out/go_stack.pdf",dutyDir+"/out/go_stack.png")
          Utils.pdf2Png(dutyDir+"/out/go_stack.pdf",dutyDir+"/out/go_stack.tiff")
        }
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, sname, input, param, elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String], abbre:String)(implicit request: Request[AnyContent]) = {
    val (groupname,groupcolor) = FileUtils.readLines(new File(dutyDir+"/out/color.xls")).asScala.map{line=>
      (line.split("\t")(0).replaceAll("\"",""),
        line.split("\t")(1).replaceAll("\"",""))
    }.unzip
    val pics= if(abbre=="KEGG") (dutyDir+"/out/ko_dodge.png",dutyDir+"/out/ko.Ko.enrich.png") else (dutyDir+"/out/go_stack.png",dutyDir+"/out/go.Go.enrich.png")
    Json.obj("pics"->pics,"elements"->elements,"groupname"->groupname.tail,"groupcolor"->groupcolor.tail)
  }

  def ReDraw(dutyDir: String, newElements: Map[String, String], abbre:String)(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val (r,i,g,in)=
      if(abbre=="KEGG")
        ("R/gokegg/plot/Ko_dodge_plot.R",dutyDir + "/out/ko.Ko.bar.dat"," -g " + newElements("g"),"ko_dodge")
      else ("R/gokegg/plot/Go_stack_plot.R",dutyDir + "/out/go.Go.bar.dat","","go_stack")

    val elejson= Json.obj("n"->newElements("n"),"br"->newElements("br"),"g"->newElements("g"),"sm"->newElements("sm"),"width"->newElements("width"),
      "height"->newElements("height"),"xts"->newElements("xts"),"yts"->newElements("yts"),"lts"->newElements("lts"),"dpi"->newElements("dpi")).toString()

    val cs=newElements("color")+":#E41A1C:#FFC0CB:#1E90FF:#00BFFF:#FF8C00:#FFDEAD:#4DAF4A:#90EE90:#9692C3:#CDB4FF:#40E0D0:#00FFFF"

    val command = "Rscript " + Utils.path + r + " -i "+ i + " -o " + dutyDir + "/out -n " + newElements("n") + " -sm " +
      newElements("sm") + " -br " + newElements("br") + " -cs " + cs + " -is " + newElements("width") + ":" + newElements("height") + " -dpi " +
      newElements("dpi") + " -xts sans:bold.italic:" + newElements("xts") + " -yts sans:bold.italic:" + newElements("yts") + " -lts sans:bold.italic:" +
      newElements("lts") + " -in " + in + g + " -if pdf"

    println(command)
    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(command,dutyDir+"/temp")

    var valid = ""
    var pics = ""
    val (groupname,groupcolor) = if(new File(dutyDir + "/out/color.xls").exists()) {
      FileUtils.readLines(new File(dutyDir + "/out/color.xls")).asScala.map{line=>
        (line.split("\t")(0).replaceAll("\"",""),line.split("\t")(1).replaceAll("\"",""))
      }.unzip
    } else (mutable.Buffer("",""), mutable.Buffer("",""))

    if (execCommand.isSuccess) {
      valid = "true"
      Utils.pdf2Png(dutyDir+"/out/"+in+".pdf",dutyDir+"/out/"+in+".png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/"+in+".pdf",dutyDir+"/out/"+in+".tiff") //替换图片
      pics = dutyDir+"/out/"+in+".png"
    } else {
      valid = "false"
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->elejson, "groupname"->groupname.tail, "groupcolor"->groupcolor.tail)
  }

}
