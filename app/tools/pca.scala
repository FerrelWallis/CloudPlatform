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

object pca extends MyFile with MyStringTool{

  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "PCA Success!"
    val isgroup = params("isgroup")
    val tablenum = params("tablenum")
    val groupnum = params("groupnum")
    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")

    val input=
      if(tablenum == "2") {
        val file1=request.body.file("table1").get
        file1.ref.getPath.moveToFile(tableFile)
        if(isgroup == "TRUE" && groupnum == "2") file1.filename+"/"+ request.body.file("table2").get.filename
        else file1.filename
      } else{
        FileUtils.writeStringToFile(tableFile, params("txdata1").trim.reformFile)
        if(isgroup == "TRUE" && groupnum == "2") request.body.file("table2").get.filename
        else "无"
      }

    println(isgroup == "TRUE")
    val (param,co)=
      if(isgroup == "TRUE")
        ("是否归一化：" + params("scale") + "/是否显示样本名：" + params("showname") +
          "/是否分组绘图：" + params("isgroup"), "#CD0000:#3A89CC:#769C30:#D99536:#7B0078:#BFBC3B:" +
          "#E2609F:#00688B:#C10077:#CAAA76:#EEEE00:#458B00:#8B4513:#008B8B:#6E8B3D:#8B7D6B:#7FFF00:" +
          "#CDBA96:#ADFF2F")
      else ("是否归一化：" +  params("scale") + "/是否显示样本名：" + params("showname") +
        "/是否分组绘图：" + params("isgroup"), "#48FF75")

    val (xdata,ydata) = ("PC1","PC2")

    val groupdata=
      if(isgroup == "TRUE" ){
        if(groupnum == "2"){
          val file = request.body.file("table2").get
          val groupdatas = FileUtils.readFileToString(file.ref.file).trim
          FileUtils.writeStringToFile(groupFile, ("#SampleID\tGroup\n"+groupdatas).reformFile)
        }else{
          println(("#SampleID\tGroup\n" + params("txdata2").trim))
          println(("#SampleID\tGroup\n" + params("txdata2").trim).reformFile)
          FileUtils.writeStringToFile(groupFile, ("#SampleID\tGroup\n" + params("txdata2").trim).reformFile)
        }
        " -g "+groupFile.getAbsolutePath
      }else ""

    val name=if(params("showname").equals("TRUE") && groupFile.exists()){
      val f=FileUtils.readLines(groupFile).asScala
      val n=f.map{_.split('\t').last}.distinct.drop(1).mkString(",")
      " -b " + n
    }else if(params("showname").equals("TRUE") && !groupFile.exists()){
      " -sl TRUE"
    } else ""

    val elements= Json.obj("xdata"->xdata,"ydata"->ydata,"width"->"15","length"->"12",
      "showname"->params("showname"), "showerro"->params("showerro"),"color"->co,"resolution"->"300",
      "xts"->"15","yts"->"15","xls"->"17","yls"->"17", "lts"->"14","lms"->"15","lmtext"->"",
      "ms"->"17","mstext"->"","c"->"FALSE","big"->"no", "xdamin"->"","xdamax"->"","ydamin"->"",
      "ydamax"->"").toString()

    try {
      val execCommand = new ExecCommand()
      val command1 = "Rscript " + Utils.path + "R/pca/pca_data.R" + " -i " + tableFile.getAbsolutePath +
        " -o " + dutyDir + "/out -sca " + params("scale")
      val command2 = "Rscript " + Utils.path + "R/pca/pca_plot.R -i " + dutyDir+"/out/pca.x.xls" +
        " -si " + dutyDir + "/out/pca.sdev.xls" + groupdata + " -o " +dutyDir+"/out" + name +
        " -if pdf -ss " + params("showerro") + " -sl " + params("showname")
      execCommand.exect(Array(command1, command2), new File(dutyDir + "/temp"))
      println(command1)
      println(command2)

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.png")
        Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "主成分分析（PCA）", input, param, elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val color=elements("color").split(":")
    val head=FileUtils.readFileToString(new File(dutyDir+"/table.txt")).trim.split("\n")
    val gnum=head(0).trim.split("\t").drop(1).length
    val group=
      if(new File(dutyDir+"/group.txt").exists()) {
        val f=FileUtils.readLines(new File(dutyDir+"/group.txt")).asScala
        val g=f.map{_.split('\t').last}.distinct.drop(1).sorted
        //检查group的数量与矩阵head是否一样，小于则+nogroup，相等则不变
        if(f.map{_.split('\t').head}.drop(1).length<gnum) g.append("nogroup")
        g.toArray
      }else Array("nogroup")
    val filepath=dutyDir+"/out/pca.x.xls"
    val data=FileUtils.readLines(new File(filepath))
    val col=data.get(0).split("\"").filter(_.trim!="").map(_.trim)
    Json.obj("group"->group,"cols"->col,"elements"->elements,"color"->color)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val groupFile=new File(dutyDir,"group.txt")
    val groupdata=if(groupFile.exists()) " -g " + groupFile.getAbsolutePath else ""
    val newelements= Json.obj("xdata"->elements("xdata"),"ydata"->elements("ydata"),"width"->"15",
      "length"->"12","showname"->elements("showname"),"showerro"->elements("showerro"),
      "color"->elements("color"),"resolution"->elements("resolution"), "xts"->elements("xts"),
      "yts"->elements("yts"),"xls"->elements("xls"),"yls"->elements("yls"),"lts"->elements("lts"),
      "lms"->elements("lms"),"lmtext"->elements("lmtext"),"ms"->elements("ms"),"mstext"->elements("mstext"),
      "c"->elements("c"),"big"->elements("big"),"xdamin"->elements("xdamin"),"xdamax"->elements("xdamax"),
      "ydamin"->elements("ydamin"),"ydamax"->elements("ydamax")).toString()

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

    val script="R/pca/pca_plot.R" + " -i " + dutyDir + "/out/pca.x.xls" + " -si " + dutyDir+"/out/pca.sdev.xls"

    val command = "Rscript " + Utils.path + script + groupdata + " -o " +dutyDir+"/out" + " -pxy "+
      elements("xdata") + ":" + elements("ydata") + " -is "+ elements("width") +
      ":" + elements("length") + c + " -dpi " + elements("resolution") + " -xts " +
      "sans:plain:"+elements("xts") + " -yts " + "sans:plain:"+elements("yts") + " -xls " +
      "sans:plain:"+elements("xls") + " -yls " + "sans:plain:"+elements("yls") + " -lts " +
      "sans:plain:" + elements("lts") + " -if pdf -ss " + elements("showerro") + name + lms + ms +
      " -c " + elements("c") + big

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    //    val execCommand = new ExecCommand
    //    execCommand.exect(command,dutyDir+"/temp")

    println(command)
    println(execCommand.getOutStr)
    println(execCommand.getErrStr)
    var valid = ""
    var pics = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.tiff") //替换图片
      pics=dutyDir+"/out/pca.png"
      valid = "true"
    } else {
      valid = "false"
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements)
  }


}
