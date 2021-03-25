package tools

import java.io.File

import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, MyFile, MyMapTool, MyStringTool, Utils}

import scala.collection.JavaConverters._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

object faprotax extends MyFile with MyStringTool with MyMapTool{

  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "picrust Success!"

    val tableFile=new File(dutyDir,"table.txt")
    val otuFile=new File(dutyDir,"otu.biom")
    val file1 = request.body.file("table").get
    file1.ref.getPath.moveToFile(tableFile)

    try {
      val command =if(file1.filename.contains("biom")) {
        file1.ref.getPath.moveToFile(otuFile)
        //        file1.ref.copyTo(otuFile)
        "python3 " + Utils.path + "R/FAPROTAX/FAPROTAX_1.2.4/collapse_table.py -i " + otuFile.getAbsolutePath + " -o " + dutyDir + "/out/functional_table.biom -g " + Utils.path + "R/FAPROTAX/FAPROTAX_1.2.4/FAPROTAX.txt --collapse_by_metadata 'taxonomy' -s " + dutyDir + "/out/out && \n" +
          "biom convert -i " + dutyDir + "/out/functional_table.biom -o " + dutyDir + "/out/functional_table.txt --to-tsv --table-type \"Function table\" && \n" +
          "perl " + Utils.path + "R/FAPROTAX/format_txttoXls.pl " + dutyDir + "/out/functional_table.txt " + dutyDir + "/out/functional_table.xls && \n" +
          "rm " + dutyDir + "/out/functional_table.txt && \n" +
          "cd " + dutyDir + "/out/out && \n" +
          "find *biom > function.list && \n" +
          "perl " + Utils.path + "R/FAPROTAX/format_mult-Biomtotxt.pl function.list ./ && \n" +
          "find *txt > function_txt.list && \n" +
          "perl " + Utils.path + "R/FAPROTAX/combine.function.pl function_txt.list ../functional_otu.txt"
      }else {
        "biom convert -i " + tableFile.getAbsolutePath + " -o " + otuFile.getAbsolutePath + " --table-type=\"OTU table\" --to-json --process-obs-metadata taxonomy && \n"+
          "python3 " + Utils.path + "R/FAPROTAX/FAPROTAX_1.2.4/collapse_table.py -i " + otuFile.getAbsolutePath + " -o " + dutyDir + "/out/functional_table.biom -g " + Utils.path + "R/FAPROTAX/FAPROTAX_1.2.4/FAPROTAX.txt --collapse_by_metadata 'taxonomy' -s " + dutyDir + "/out/out && \n" +
          "biom convert -i " + dutyDir + "/out/functional_table.biom -o " + dutyDir + "/out/functional_table.txt --to-tsv --table-type \"Function table\" && \n" +
          "perl " + Utils.path + "R/FAPROTAX/format_txttoXls.pl " + dutyDir + "/out/functional_table.txt " + dutyDir + "/out/functional_table.xls && \n" +
          "rm " + dutyDir + "/out/functional_table.txt && \n" +
          "cd " + dutyDir + "/out/out && \n" +
          "find *biom > function.list && \n" +
          "perl " + Utils.path + "R/FAPROTAX/format_mult-Biomtotxt.pl function.list ./ && \n" +
          "find *txt > function_txt.list && \n" +
          "perl " + Utils.path + "R/FAPROTAX/combine.function.pl function_txt.list ../functional_otu.txt"
      }

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

      if (execCommand.isSuccess) {
        (dutyDir+"/out/outTables.zip").creatZip(dutyDir+"/out/out")
      } else {
        state = 2
        msg = execCommand.getErrStr
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "FAPROTAX功能预测", file1.filename, "/", "")
  }

}
