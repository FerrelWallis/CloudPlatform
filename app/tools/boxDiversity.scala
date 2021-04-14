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

object boxDiversity extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")
    val file1 = request.body.file("table1").get
    val file2 = request.body.file("table2").get
    val input = file1.filename+"/" + file2.filename
    file1.ref.getPath.moveToFile(tableFile)
    file2.ref.getPath.moveToFile(groupFile)

    val origin = Json.obj("spot"->"TRUE","ymin"->"","ymax"->"","lp"->"right:top",
      "boxwidth"->"0.7","alp"->"0.8","add"->"1","color"->"#E41A1C:#1E90FF:#FF8C00:#4DAF4A:#984EA3:#40E0D0:#F4AAC4:#DC414B:#957624:#43B43C",
      "width"->"12","length"->"10","dpi"->"300","xts"->"15","xls"->"12","xtext"->"","yts"->"15",
      "yls"->"12","ytext"->"","lts"->"14","lls"->"15","lltext"->"","ms"->"12","mstext"->"",
      "flip"->"FALSE","picname"->"").toString

    val elements=Json.obj("S.obs"-> origin, "S.chao1"->origin, "se.chao1"->origin, "S.ACE"->origin,
      "se.ACE"->origin, "Shannon"->origin, "Simpson"->origin, "Pielou"->origin, "goods_coverage"->origin).toString

    try {
      val plotfile = dutyDir + "/out/alpha_diversity.xls"
      val command1 = "Rscript "+Utils.path+"R/box_diversity/box_diveristy.R -i " + tableFile.getAbsolutePath +
        " -g " + groupFile.getAbsolutePath + " -o " + dutyDir + "/out"
      val picname = Array("S.obs","S.chao1","se.chao1","S.ACE","se.ACE","Shannon","Simpson","Pielou","goods_coverage")
      var cmdArray = Array(command1)
      picname.map{ name =>
        val cmd = "Rscript "+Utils.path+"R/box/boxplot.R -i "+ plotfile + " -o " + dutyDir + "/out" +
          " -sp TRUE" + " -ls 12:10 -d TRUE -dc " + name + " -in " + name + " -f FALSE"
        cmdArray = cmdArray:+cmd
      }
      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),cmdArray.mkString(" && \n"))
      val execCommand = new ExecCommand
      execCommand.exec(cmdArray)
      println(cmdArray.mkString(" && \n"))

      if (execCommand.isSuccess) {
        val cols = Array("S.obs", "S.chao1", "se.chao1", "S.ACE", "se.ACE", "Shannon", "Simpson", "Pielou", "goods_coverage")
        cols.map{ colsname =>
          Utils.pdf2Png(dutyDir+"/out/"+colsname+".pdf",dutyDir+"/out/"+colsname+".png")
          Utils.pdf2Png(dutyDir+"/out/"+colsname+".pdf",dutyDir+"/out/"+colsname+".tiff")
        }
      } else {
        state = 2
        msg = execCommand.getErrStr
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "多样性指数盒型图", input, "/", elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val S_obs	= elements("S.obs").jsonToMap
    val S_chao1	= elements("S.chao1").jsonToMap
    val se_chao1	= elements("se.chao1").jsonToMap
    val S_ACE	= elements("S.ACE").jsonToMap
    val se_ACE	= elements("se.ACE").jsonToMap
    val Shannon	= elements("Shannon").jsonToMap
    val Simpson	= elements("Simpson").jsonToMap
    val Pielou	= elements("Pielou").jsonToMap
    val goods_coverage	= elements("goods_coverage").jsonToMap

    val group = FileUtils.readLines(new File(dutyDir + "/out/alpha_diversity.xls")).asScala.map{line =>
      line.split("\t")(1)
    }.drop(1).distinct

    val picpath = dutyDir + "/out/"
    Json.obj("picpath"->picpath, "group"->group, "S_obs"->S_obs,
      "S_chao1"->S_chao1, "se_chao1"->se_chao1,"S_ACE"->S_ACE, "se_ACE"->se_ACE, "Shannon"->Shannon,
      "Simpson"->Simpson, "Pielou"->Pielou, "goods_coverage"->goods_coverage)
  }

  def ReDraw(dutyDir: String, newElements: Map[String, String], elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val picname = newElements("picname")
    var oldelements = elements
    oldelements = oldelements.-(picname)
    val ele = Json.obj("lp"->newElements("lp"),"spot"->newElements("spot"),"ymin"->newElements("ymin"),"ymax"->newElements("ymax"),
      "boxwidth"->newElements("boxwidth"),"alp"->newElements("alp"),"add"->newElements("add"),"color"->newElements("color"), "width"->newElements("width"),
      "length"->newElements("length"),"dpi"->newElements("dpi"),"xts"->newElements("xts"),"xls"->newElements("xls"),"xtext"->newElements("xtext"),
      "yts"->newElements("yts"),"yls"->newElements("yls"),"ytext"->newElements("ytext"),"lts"->newElements("lts"),"lls"->newElements("lls"),
      "lltext"->newElements("lltext"),"ms"->newElements("ms"),"mstext"->newElements("mstext"),"flip"->newElements("flip")).toString()
    oldelements = oldelements.+((picname, ele))
    val elejson = oldelements.mapToJson.toString

    val ymm=if(newElements("ymin")==""||newElements("ymax")=="") "" else " -ymm " + newElements("ymin") + ":" + newElements("ymax")

    val yls=if(!newElements("ytext").equals("")) " -yls sans:bold.italic:" + newElements("yls") + ":\"" + newElements("ytext") + "\"" else ""
    val xls=if(!newElements("xtext").equals("")) " -xls sans:bold.italic:" + newElements("xls") + ":\"" + newElements("xtext") + "\"" else ""
    val lls=if(!newElements("lltext").equals("")) " -lls sans:bold.italic:" + newElements("lls") + ":\"" + newElements("lltext") + "\"" else ""
    val ms=if(!newElements("mstext").equals("")) " -ms sans:bold.italic:" + newElements("ms") + ":\"" + newElements("mstext") + "\"" else ""

    val command = "Rscript "+Utils.path+"R/box/boxplot.R -i "+ dutyDir+"/out/alpha_diversity.xls" + " -o " +dutyDir+"/out" +
      " -sp "+ newElements("spot") + ymm + " -ls " + newElements("width") + ":" + newElements("length") + " -dpi " + newElements("dpi") + " -bw " +
      newElements("boxwidth") + " -alp " + newElements("alp") + " -add " + newElements("add") + " -xts " + "sans:bold.italic:"+ newElements("xts") +
      " -yts " + "sans:bold.italic:" + newElements("yts") + " -lts " + "sans:bold.italic:" + newElements("lts") + " -cs \"" + newElements("color") +
      "\" -lp " + newElements("lp") + xls + yls + lls + ms + " -f " + newElements("flip") + " -d TRUE -dc " + newElements("picname") +
      " -in " + newElements("picname")

    println(command)
    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    var valid = ""
    var pics = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/"+newElements("picname")+".pdf",dutyDir+"/out/"+newElements("picname")+".png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/"+newElements("picname")+".pdf",dutyDir+"/out/"+newElements("picname")+".tiff") //替换图片
      pics=dutyDir+"/out/"+newElements("picname")+".png"
      valid = "true"
    } else {
      valid = "false"
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->elejson)
  }


}
