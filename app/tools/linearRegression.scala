package tools

import java.io.File

import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, MyFile, MyMapTool, MyStringTool, Utils}

object linearRegression extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"
    
    val tableFile=new File(dutyDir,"table.txt")
    val file1=request.body.file("table1").get
    file1.ref.getPath.moveToFile(tableFile)

    val elements= Json.obj("mt"->"XY Linear Regression Plot", "ms"->"20",
      "mc_color"->"black", "xt"->"X", "yt"->"Y", "ac_color"->"black", "ats"->"20",
      "atc_color"->"black", "als"->"15", "alc_color"->"black", "pc_color"->"#6699ff",
      "pz"->"4", "sc_color"->"red", "height"->"10", "width"->"10", "dpi"->"300",
      "fc_color"->"black", "fz"->"5").toString()

    try {
      val command = "Rscript "+Utils.path+"R/linearReg/XY.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out -if pdf -in linearReg"

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/linearReg.pdf",dutyDir+"/out/linearReg.png")
        Utils.pdf2Png(dutyDir+"/out/linearReg.pdf",dutyDir+"/out/linearReg.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "线性回归图", file1.filename, "/", elements)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val tableFile=new File(dutyDir,"table.txt")
    val newelements= Json.obj("mt"->elements("mt"), "ms"->elements("ms"), "mc_color"->elements("mc_color"),
      "xt"->elements("xt"), "yt"->elements("yt"), "ac_color"->elements("ac_color"), "ats"->elements("ats"),
      "atc_color"->elements("atc_color"), "als"->elements("als"), "alc_color"->elements("alc_color"),
      "pc_color"->elements("pc_color"), "pz"->elements("pz"), "sc_color"->elements("sc_color"), "height"->elements("height"),
      "width"->elements("width"), "dpi"->elements("dpi"), "fc_color"->elements("fc_color"), "fz"->elements("fz")).toString()

    println(newelements)

    val command = "Rscript "+Utils.path+"R/linearReg/XY.R -i "+ tableFile.getAbsolutePath +
      " -o " +dutyDir+"/out -if pdf -in linearReg -mt \"" + elements("mt") + "\" -ms " + elements("ms") +
      " -mc \"" + elements("mc_color") + "\" -xt \"" + elements("xt") + "\" -yt \"" + elements("yt") + "\" -ac \"" +
      elements("ac_color") + "\" -ats " + elements("ats") + " -atc \"" + elements("atc_color") + "\" -als " +
      elements("als") + " -alc \"" + elements("alc_color") + "\" -pc \"" + elements("pc_color") + "\" -pz " +
      elements("pz") + " -sc \"" + elements("sc_color") + "\" -is " + elements("height") + " -iw " + elements("width") +
      " -dpi " + elements("dpi") + " -fc \"" + elements("fc_color") + "\" -fz " + elements("fz")

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    println(command)
    
    var valid = ""
    var pics = ""
    var msg = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/linearReg.pdf",dutyDir+"/out/linearReg.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/linearReg.pdf",dutyDir+"/out/linearReg.tiff") //替换图片
      pics=dutyDir+"/out/linearReg.png"
      valid = "true"
    } else {
      valid = "false"
      msg = execCommand.getErrStr
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements, "message"->msg)
  }

}