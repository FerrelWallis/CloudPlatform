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

object picrust extends MyFile with MyStringTool with MyMapTool{
  
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    new File(dutyDir + "/out").delete()
    val tableFile=new File(dutyDir,"table.txt")
    val otuFile=new File(dutyDir,"otu.biom")
    val seqFile=new File(dutyDir,"seq.fasta")
    val file1 = request.body.file("table1").get
    val file2 = request.body.file("table2").get
    file1.ref.getPath.moveToFile(tableFile)
    file2.ref.getPath.moveToFile(new File(dutyDir + "/temp.txt"))

    val seqcontent = FileUtils.readLines(new File(dutyDir + "/temp.txt")).asScala.map{line=>
      if(line.indexOf(">") >= 0) line.split("\\s").head else line
    }.mkString("\n")
    FileUtils.writeStringToFile(seqFile,seqcontent)

    tableFile.setExecutable(true,false)
    tableFile.setReadable(true,false)
    tableFile.setWritable(true,false)
    otuFile.setExecutable(true,false)
    otuFile.setReadable(true,false)
    otuFile.setWritable(true,false)
    seqFile.setExecutable(true,false)
    seqFile.setReadable(true,false)
    seqFile.setWritable(true,false)

    val param="选择数据基因家族:"+params("in_traits")+"/是否在各层级产生分层:"+params("stratified")+
      "/max_nsti:"+params("max_nsti")+"/min_reads:"+params("min_reads")+"/min_samples:"+params("min_samples")+
      "/min_align:"+params("min_align")+"/HSP method:"+params("m")+"/skip_norm:"+params("skip_norm")

    try {
      val stratified = if(params("stratified")=="yes") " --stratified " else ""
      val skipnorm = if(params("skip_norm") == "yes") " --skip_norm " else ""

      val command0=if(file1.filename.contains("biom")) {
        file1.ref.getPath.moveToFile(otuFile)
        ""
      } else
        "biom convert -i " + tableFile.getAbsolutePath + " -o " + otuFile.getAbsolutePath + " --table-type=\"OTU table\" --to-json && \n"

      val command1 = "picrust2_pipeline.py -i " + otuFile.getAbsolutePath +
        " -s " + seqFile.getAbsolutePath + " -o " + dutyDir + "/out" + " --processes 20 " + stratified +
        " --in_traits " + params("in_traits") + skipnorm + " --max_nsti " + params("max_nsti") + " --min_reads " +
        params("min_reads") + " --min_samples " + params("min_samples") + " --min_align " + params("min_align") +
        " -m " + params("m")

      val command=command0 +
        "cd /root/miniconda3/bin && \n" +
        ". ./activate && \n"+
        "conda activate picrust2 && \n"+
        command1+ " && \n"+
        "conda deactivate"

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

      if (execCommand.isSuccess) {
        new File(dutyDir+"/out/intermediate").delete()
        new File(dutyDir+"/out").listFiles().filter(_.isDirectory).foreach{x=>
          val dname=x.getName
          (dutyDir+"/out/"+dname+".zip").creatZip(x.getAbsolutePath)
        }
      } else {
        state = 2
        msg = execCommand.getErrStr
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "PICRUST2功能预测", file1.filename+"/"+file2.filename, param, "")
  }

}
