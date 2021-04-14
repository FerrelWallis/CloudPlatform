package tools

import java.io.File

import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, MyFile, MyMapTool, MyStringTool, Utils}

import scala.collection.JavaConverters._
import scala.collection.mutable

object scatterplot extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val tableFile=new File(dutyDir,"table.txt")
    val file1=request.body.file("table1").get
    file1.ref.getPath.moveToFile(tableFile)

    val elements= Json.obj("xydx"->params("xydx"), "xydy"->params("xydy"), "xl"->params("xlog"),
      "yl"->params("ylog"), "m"->params("m"), "hl"->"", "vl"->"", "dl"->"", "dc"->"TRUE", "dax"->"TRUE",
      "cohj"->"1.5", "covj"->"10", "cor_color"->"black", "cos"->"4.5", "big"->"no", "xdamin"->"", "xdamax"->"",
      "ydamin"->"","ydamax"->"", "dot_color"->"red", "width" -> "10", "height"->"10", "dpi"->"300", "xts"->"15",
      "yts"->"15", "xls"->"17", "yls"->"17", "ms"->"17", "xlsangle"->"0", "ylsangle"->"90", "xtext"->"", "ytext"->"",
      "mstext"->"").toString()

    val param = "作为X轴数据的列：" + params("xydx") + "/作为Y轴数据的列：" + params("xydy") + "/对X轴数据预处理：" +
      params("xlog") + "/对Y轴数据预处理:" + params("ylog") + "/相关分析方法：" + params("m")

    try {
      val command = "Rscript "+Utils.path+"R/scatterpoint/scatterplot.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out -if pdf -xyd " + params("xydx") + ":" + params("xydy") + " -xl " + params("xlog") +
        " -yl " + params("ylog")

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      //exec需要指定结果输出路径的时候，不指定默认本地任务路径
      execCommand.exect(command,dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/scatter.pdf",dutyDir+"/out/scatter.png")
        Utils.pdf2Png(dutyDir+"/out/scatter.pdf",dutyDir+"/out/scatter.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "高级散点图", file1.filename, param, elements)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val tableFile = new File(dutyDir,"table.txt")
    val newelements = Json.obj("xydx"->elements("xydx"), "xydy"->elements("xydy"), "xl"->elements("xl"), 
      "yl"->elements("yl"), "m"->elements("m"), "hl"->elements("hl"), "vl"->elements("vl"), "dl"->elements("dl"), 
      "dc"->elements("dc"), "dax"->elements("dax"), "cohj"->elements("cohj"), "covj"->elements("covj"), 
      "cor_color"->elements("cor_color"), "cos"->elements("cos"), "big"->elements("big"), 
      "xdamin"->elements("xdamin"), "xdamax"->elements("xdamax"),"ydamin"->elements("ydamin"),
      "ydamax"->elements("ydamax"), "dot_color"->elements("dot_color"), "width"->elements("width"), 
      "height"->elements("height"), "dpi"->elements("dpi"), "xts"->elements("xts"), "yts"->elements("yts"),
      "xls"->elements("xls"), "yls"->elements("yls"), "ms"->elements("ms"), "xlsangle"->elements("xlsangle"),
      "ylsangle"->elements("ylsangle"), "xtext"->elements("xtext"), "ytext"->elements("ytext"),
      "mstext"->elements("mstext")).toString()

    val big=if(elements("big")=="no") ""
    else if(elements("big")=="x")
      " -da x:"+elements("xdamin")+","+elements("xdamax")
    else " -da y:"+elements("ydamin")+","+elements("ydamax")

    val xtext=if(elements("xtext")=="") "" else elements("xtext")
    val ytext=if(elements("ytext")=="") "" else elements("ytext")
    val mstext=if(elements("mstext")=="") " " else elements("mstext")

    val command =  "Rscript "+Utils.path+"R/scatterpoint/scatterplot.R -i "+ tableFile.getAbsolutePath +
      " -o " + dutyDir + "/out -if pdf -xyd " + elements("xydx") + ":" + elements("xydy") + " -xl " + elements("xl") +
      " -yl " + elements("yl") + " -m " + elements("m") + " -hl " + elements("hl") + " -vl " + elements("vl") +
      " -dl \"" + elements("dl") + "\" -dc " + elements("dc") + " -dax " + elements("dax") + " -coj " + elements("cohj") +
      ":" + elements("covj") + " -coc \"" + elements("cor_color") + "\" -cos " + elements("cos") + big + " -doc \"" +
      elements("dot_color") + "\" -is " + elements("width") + ":" + elements("height") + " -dpi " + elements("dpi") +
      " -xts \"sans:plain:" + elements("xts") + "\" -yts \"sans:plain:" + elements("yts") + "\" -xls \"sans:plain:" +
      elements("xls") + ":" + xtext + ":" + elements("xlsangle") + "\" -yls " + "\"sans:plain:" + elements("yls") +
      ":" + ytext + ":" + elements("ylsangle") + "\" -ms \"sans:plain:" + elements("ms") + ":" + mstext + "\""

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    println(command)
    
    var valid = ""
    var pics = ""
    var msg = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/scatter.pdf",dutyDir+"/out/scatter.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/scatter.pdf",dutyDir+"/out/scatter.tiff") //替换图片
      pics=dutyDir+"/out/scatter.png"
      valid = "true"
    } else {
      valid = "false"
      msg = execCommand.getErrStr
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements, "message"->msg)
  }

}
