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

object manhattan extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val samnum = params("samnum").toInt
    val sam = params("sam")
    val file1=request.body.file("table1").get
    val tableFile=new File(dutyDir,"table.txt")
    tableFile.setExecutable(true,false)
    tableFile.setReadable(true,false)
    tableFile.setWritable(true,false)
    val input= file1.filename

    val (cs,ahpos,ahcolo,ahsize,xtext,ytext,width,height)=
      if(samnum>1)
        ("#E41A1C:#9B445D:#526E9F:#3C8A9B:#469F6C:#54A453:#747B78:#94539E:#BD6066:#E97422:#FF990A:#FFCF20:#FAF632:#D4AE2D:#AF6729:#BF6357:#E17597:#E884B9:#C08EA9:#999999",
          "20","#66ff33","1","","","13","10")
      else ("#1E90FF:#E41A1C","1.5","#ffcc00","0.8",sam,"LOD","12","11")

    val elements= Json.obj("sam"->sam,"samnum"->samnum.toString,"cs"->cs,"ahpos"->ahpos,
      "ahcolo"->ahcolo,"ahsize"->ahsize,"dt"->"point","p"->"FALSE","yrmax"->"","yrmin"->"",
      "lts"->"14","xts"->"13","yts"->"12","xls"->"16","xtext"->xtext,"yls"->"16","ytext"->ytext,
      "ms"->"16","mstext"->"","width"->width,"height"->height,"dpi"->"300","ps"->"2","lbs"->"4",
      "lbscolo"->"#CA9197","lis"->"3","showah"->"FALSE","pshigh"->"0.5","pslow"->"0.5",
      "xtpangle"->"60","xtphjust"->"1","hpccolo"->"#7F4B89").toString()

    file1.ref.getPath.moveToFile(tableFile)

    try {
      val command =
        if(samnum>1) "Rscript "+Utils.path+"R/manhadun/mahadun_plot.R -i "+ tableFile.getAbsolutePath + " -o " +dutyDir+"/out" + " -if pdf -in manhattan -ls 13:10"
        else "Rscript "+Utils.path+"R/manhadun/mahadun_virtual_plot.R -i "+ tableFile.getAbsolutePath + " -o " +dutyDir+"/out" + " -if pdf -in manhattan"

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")
      println(command)

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/manhattan.pdf",dutyDir+"/out/manhattan.png")
        Utils.pdf2Png(dutyDir+"/out/manhattan.pdf",dutyDir+"/out/manhattan.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg,"曼哈顿图（Manhattan）", input, "/", elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val samnum=elements("samnum").toInt
    val group=if(samnum>1) elements("sam").split(",").toArray else Array("down","up")
    val color=elements("cs").split(":")
    Json.obj("group"->group,"sam"->elements("sam"),"samnum"->samnum,"elements"->elements,"color"->color)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String], oldelements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val tableFile=new File(dutyDir,"table.txt")

    val newelements= Json.obj("sam"->oldelements("sam"),"samnum"->oldelements("samnum"),"cs"->elements("color"),
      "ahpos"->elements("ahpos"),"ahcolo"->elements("ahcolo"),"ahsize"->elements("ahsize"),"dt"->elements("dt"),
      "p"->elements("p"),"yrmax"->elements("yrmax"),"yrmin"->elements("yrmin"),"lts"->elements("lts"),"xts"->elements("xts"),
      "yts"->elements("yts"),"xls"->elements("xls"),"xtext"->elements("xtext"),"yls"->elements("yls"),"ytext"->elements("ytext"),
      "ms"->elements("ms"),"mstext"->elements("mstext"),"width"->elements("width"),"height"->elements("height"),
      "dpi"->elements("dpi"),"ps"->elements("ps"),"lbs"->elements("lbs"),"lbscolo"->elements("lbscolo"),
      "lis"->elements("lis"),"showah"->elements("showah"),"pshigh"->elements("pshigh"),"pslow"->elements("pslow"),
      "xtpangle"->elements("xtpangle"),"xtphjust"->elements("xtphjust"),"hpccolo"->elements("hpccolo")).toString()

    val yr=if(elements("yrmin")=="" || elements("yrmax")=="") "all" else elements("yrmin") + ":" + elements("yrmax")
    val xtext=if(elements("xtext")=="") " " else elements("xtext")
    val ytext=if(elements("ytext")=="") " " else elements("ytext")
    val mstext=if(elements("mstext")=="") " " else elements("mstext")
    val ps=if(elements("showah")=="FALSE") elements("ps") + ":" + elements("ps") else elements("pslow") + ":" + elements("pshigh")

    val command = if(oldelements("samnum").toInt > 1)
      "Rscript " + Utils.path + "R/manhadun/mahadun_plot.R -i " + tableFile.getAbsolutePath +
        " -o " + dutyDir + "/out -cs \"" + elements("color") + "\" -ah \"" + elements("ahpos") + ":" + elements("ahcolo") +
        ":" + elements("ahsize") + ":" + elements("showah") + "\" -dt " + elements("dt") + " -ps " + ps + " -lis " +
        elements("lis") + " -xtp \"" + elements("xtpangle") + ":" + elements("xtphjust") + "\" -p " + elements("p") + " -yr " + yr +
        " -lts sans:plain:" + elements("lts") + " -xts sans:bold.italic:" + elements("xts") +
        " -yts sans:bold.italic:" + elements("yts") + " -xls \"sans:bold.italic:" + elements("xls") + ":" + xtext +
        ":black\"" + " -yls \"sans:bold.italic:" + elements("yls") + ":" + ytext + ":black\"" + " -hpc \"" +
        elements("hpccolo") + "\" -ms \"sans:bold.italic:" + elements("ms") + ":" + mstext + ":black\"" + " -ls " +
        elements("width") + ":" + elements("height") + " -dpi " + elements("dpi") + " -if pdf -in manhattan"
    else
      "Rscript " + Utils.path + "R/manhadun/mahadun_virtual_plot.R -i " + tableFile.getAbsolutePath +
        " -o " + dutyDir + "/out -cs \"" + elements("color") + "\"" + " -ps " + elements("ps") + " -ah \"" +
        elements("ahpos") + ":" + elements("ahcolo") + ":" + elements("ahsize") + "\" -yr " + yr + " -lbs \"sans:plain:" +
        elements("lbs") + ":" + elements("lbscolo") + "\" -lis " + elements("lis") + " -lts sans:plain:" + elements("lts") +
        " -yts sans:bold.italic:" + elements("yts") + " -xls \"sans:bold.italic:" + elements("xls") + ":" +
        xtext + ":black\"" + " -yls \"sans:bold.italic:" + elements("yls") + ":" + ytext + ":black\"" +
        " -ms \"sans:bold.italic:" + elements("ms") + ":" + mstext + ":black\"" + " -ls " + elements("width") +
        ":" + elements("height") + " -dpi " + elements("dpi") + " -if pdf -in manhattan"

    println(command)

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    var valid = ""
    var pics = ""
    var msg = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/manhattan.pdf",dutyDir+"/out/manhattan.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/manhattan.pdf",dutyDir+"/out/manhattan.tiff") //替换图片
      pics=dutyDir+"/out/manhattan.png"
      valid = "true"
    } else {
      valid = "false"
      msg = execCommand.getErrStr
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements, "message"->msg)
  }

}
