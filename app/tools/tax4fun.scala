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

object tax4fun extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "ADB Success!"
    
    val tableFile=new File(dutyDir,"table.txt")
    val otuFile=new File(dutyDir,"otu_taxa_table.format.txt")
    val file1 = request.body.file("table1").get
    file1.ref.getPath.moveToFile(tableFile)
    val param= "/选择SILVA数据库：" + params("r") + "/预计算方法：" + params("ref")

    try {
      val command =if(file1.filename.contains("biom")) {
        "perl " + Utils.path + "R/tax4fun/perl/tax4fun.step1.pl " + tableFile.getAbsolutePath + " " + dutyDir +" && \n" +
          "Rscript " + Utils.path + "R/tax4fun/Tax4Fun.R -rp " + Utils.path + "R/tax4fun/db -i " + otuFile.getAbsolutePath + " -o " + dutyDir + "/out -r " + params("r") + " -ref " + params("ref") + " && \n" +
          "perl " + Utils.path + "R/tax4fun/perl/K2pathway.pl " + dutyDir + "/out/KO_table.txt" + " " + dutyDir + "/out && \n" +
          "Rscript " + Utils.path + "R/tax4fun/rs/tax4fun-boxplot.R -i " + dutyDir + "/out/kegg_L1.txt -o " + dutyDir + "/out -in kegg_L1 && \n" +
          "Rscript " + Utils.path + "R/tax4fun/rs/tax4fun-boxplot.R -i " + dutyDir + "/out/kegg_L2.txt -o " + dutyDir + "/out -in kegg_L2 && \n" +
          "Rscript " + Utils.path + "R/tax4fun/rs/tax4fun-boxplot.R -i " + dutyDir + "/out/kegg_L3.txt -o " + dutyDir + "/out -in kegg_L3 -lsa 10:1:1 -yts sans:bold.italic:4 -le FALSE && \n" +
          "Rscript " + Utils.path + "R/tax4fun/rs/tax4fun-pca_data.R -i " + dutyDir + "/out/KO_table.txt -o " + dutyDir + "/out && \n" +
          "Rscript " + Utils.path + "R/tax4fun/rs/tax4fun-pca_plot.R -i " + dutyDir + "/out/pca.x.xls -si " + dutyDir + "/out/pca.sdev.xls -o " + dutyDir + "/out -c TRUE && \n" +
          "Rscript " + Utils.path + "R/tax4fun/rs/change.R -i " + dutyDir + "/out/KO_table.txt -o " + dutyDir + "/out/KO_table.xls"
      }else {
        "biom convert -i " + tableFile.getAbsolutePath + " -o " + dutyDir + "/temp.biom --table-type=\"OTU table\" --to-json --process-obs-metadata taxonomy && \n"+
          "perl " + Utils.path + "R/tax4fun/perl/tax4fun.step1.pl " + dutyDir + "/temp.biom " + dutyDir +" && \n" +
          "Rscript " + Utils.path + "R/tax4fun/Tax4Fun.R -rp " + Utils.path + "R/tax4fun/db -i " + otuFile.getAbsolutePath + " -o " + dutyDir + "/out -r " + params("r") + " -ref " + params("ref") + " && \n" +
          "perl " + Utils.path + "R/tax4fun/perl/K2pathway.pl " + dutyDir + "/out/KO_table.txt" + " " + dutyDir + "/out && \n" +
          "Rscript " + Utils.path + "R/tax4fun/rs/tax4fun-boxplot.R -i " + dutyDir + "/out/kegg_L1.txt -o " + dutyDir + "/out -in kegg_L1 && \n" +
          "Rscript " + Utils.path + "R/tax4fun/rs/tax4fun-boxplot.R -i " + dutyDir + "/out/kegg_L2.txt -o " + dutyDir + "/out -in kegg_L2 && \n" +
          "Rscript " + Utils.path + "R/tax4fun/rs/tax4fun-boxplot.R -i " + dutyDir + "/out/kegg_L3.txt -o " + dutyDir + "/out -in kegg_L3 -lsa 10:1:1 -yts sans:bold.italic:4 -le FALSE && \n" +
          "Rscript " + Utils.path + "R/tax4fun/rs/tax4fun-pca_data.R -i " + dutyDir + "/out/KO_table.txt -o " + dutyDir + "/out && \n" +
          "Rscript " + Utils.path + "R/tax4fun/rs/tax4fun-pca_plot.R -i " + dutyDir + "/out/pca.x.xls -si " + dutyDir + "/out/pca.sdev.xls -o " + dutyDir + "/out -c TRUE && \n" +
          "Rscript " + Utils.path + "R/tax4fun/rs/change.R -i " + dutyDir + "/out/KO_table.txt -o " + dutyDir + "/out/KO_table.xls"
      }

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

      if (execCommand.isSuccess) {
        Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.png")
        Utils.pdf2Png(dutyDir+"/out/kegg_L1.pdf",dutyDir+"/out/kegg_L1.png")
        Utils.pdf2Png(dutyDir+"/out/kegg_L2.pdf",dutyDir+"/out/kegg_L2.png")
        Utils.pdf2Png(dutyDir+"/out/kegg_L3.pdf",dutyDir+"/out/kegg_L3.png")
        Utils.pdf2Png(dutyDir+"/out/pca.pdf",dutyDir+"/out/pca.tiff")
        Utils.pdf2Png(dutyDir+"/out/kegg_L1.pdf",dutyDir+"/out/kegg_L1.tiff")
        Utils.pdf2Png(dutyDir+"/out/kegg_L2.pdf",dutyDir+"/out/kegg_L2.tiff")
        Utils.pdf2Png(dutyDir+"/out/kegg_L3.pdf",dutyDir+"/out/kegg_L3.tiff")
      } else {
        state = 2
        msg = execCommand.getErrStr
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "Tax4Fun功能预测", file1.filename, param, "")
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val pics= (dutyDir+"/out/pca.png",dutyDir+"/out/kegg_L1.png",dutyDir+"/out/kegg_L2.png",dutyDir+"/out/kegg_L3.png")

    Json.obj("pics"->pics)
  }

}
