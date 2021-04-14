package tools

import java.io.File

import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, MyFile, MyMapTool, MyStringTool, Utils}

object edger extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String)(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")
    val file1 = request.body.file("table1").get
    file1.ref.getPath.moveToFile(tableFile)
    val file2 = request.body.file("table2").get
    file2.ref.getPath.moveToFile(groupFile)
    val (group,filepara) = (" -g " + groupFile.getAbsolutePath,file1.filename+"/"+file2.filename)

    val elements= Json.obj("pcl"->"10e-6","fcl"->"1","xrmin"->"","xrmax"->"",
      "yrmin"->"","yrmax"->"","sp"->"TRUE","color4"->"black","cs"->"blue:red:grey",
      "xts"->"16","yts"->"16","xls"->"18","yls"->"18","ts"->"20","lts"->"16", "ltes"->"12",
      "xtext"->"log2(FC)","ytext"->"-log10(pvalue)","tstext"->"","ltstext"->"","width"->"15",
      "height"->"15","dpi"->"300").toString()

    try {
      val command1 = "Rscript " + Utils.path + "R/edger/edgeR.R -i " + tableFile.getAbsolutePath +
        group + " -o " + dutyDir + "/out"
      val command2 = "Rscript "+Utils.path+"R/volcano/volcano.R -i "+ dutyDir + "/out/edger.gene_diff.txt" +
        " -o " +dutyDir+"/out" + " -pcl 10e-6 -fcl 1 -if pdf -in volcano"

      println(command1 + " && \n" + command2)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"), command1 + " && \n" + command2)
      val execCommand = new ExecCommand
      execCommand.exect(Array(command1,command2),new File(dutyDir+"/temp"))

      if (execCommand.isSuccess) {
        Utils.pdf2Png(dutyDir+"/out/volcano.pdf",dutyDir+"/out/volcano.png")
        Utils.pdf2Png(dutyDir+"/out/volcano.pdf",dutyDir+"/out/volcano.tiff")
      } else {
        state = 2
        msg = execCommand.getErrStr
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "edgeR差异分析", filepara, "/", elements)
  }



}