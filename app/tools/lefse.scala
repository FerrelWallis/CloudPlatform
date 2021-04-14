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

object lefse extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    var input=""
    val tableFile=new File(dutyDir,"table.txt")
    val otuFile=new File(dutyDir,"otu_taxa_table.biom")
    val file1 = request.body.file("table1").get
    input=input+file1.filename+"/"
    file1.ref.moveTo(tableFile)
    tableFile.setExecutable(true,false)
    tableFile.setReadable(true,false)
    tableFile.setWritable(true,false)
    val groupFile=new File(dutyDir,"map.txt")
    val file2 = request.body.file("group").get
    input+=file2.filename
    file2.ref.moveTo(groupFile)
    groupFile.setExecutable(true,false)
    groupFile.setReadable(true,false)
    groupFile.setWritable(true,false)
    
    try {
      val command0 = if(file1.filename.contains("biom")) {
        file1.ref.moveTo(otuFile)
        ""
      } else
        "biom convert -i " + tableFile.getAbsolutePath + " -o " + otuFile.getAbsolutePath + " --table-type=\"OTU table\" --to-json --process-obs-metadata taxonomy && \n"

      val command1 = "sed -i s'/\\r//g' "+groupFile.getAbsolutePath+" && \n"
      val command2 = "chmod -R 777 " + otuFile.getAbsolutePath + "&& \n"+
        "java -jar "+Utils.path+"R/lefse/lefse_yiti-1.0-SNAPSHOT.jar -i "+otuFile.getAbsolutePath+" -o "+dutyDir+"/out -m "+dutyDir+"/map.txt\n"

      val commandpack=command0+command1+command2
      println(commandpack)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),commandpack)
      val execCommand = new ExecCommand
      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

      if (execCommand.isSuccess) {
        Utils.pdf2Png(dutyDir+"/out/lefse_LDA.cladogram.pdf",dutyDir+"/out/lefse_LDA.cladogram.png")
        Utils.pdf2Png(dutyDir+"/out/lefse_LDA.pdf",dutyDir+"/out/lefse_LDA.png")
        Utils.pdf2Png(dutyDir+"/out/lefse_LDA.cladogram.pdf",dutyDir+"/out/lefse_LDA.cladogram.tiff")
        Utils.pdf2Png(dutyDir+"/out/lefse_LDA.pdf",dutyDir+"/out/lefse_LDA.tiff")
      } else {
        state = 2
        msg = execCommand.getErrStr
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "lefse分析", input, "/", "")
  }

  def GetParams(dutyDir: String)(implicit request: Request[AnyContent]) = {
    val lefse_LDA= if(new File(dutyDir+"/out/lefse_LDA.png").length()==0) "false" else "true"
    val pics= ((dutyDir+"/out/lefse_LDA.png",dutyDir+"/out/lefse_LDA.cladogram.png"))
    Json.obj("pics"->pics, "lefse_LDA"->lefse_LDA)
  }

}
