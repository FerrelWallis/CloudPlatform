package utils

import java.io.File

import dao.softDao
import javax.imageio.ImageIO
import models.Tables.SoftRow
import org.apache.commons.io.FileUtils
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import play.api.libs.json.Json

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Utils{

  val windowsPath="F:/CloudPlatform/"
  val linuxPath="/mnt/sdb/ww/CloudPlatform/"

  val RPath : String= {
    if (new File(windowsPath).exists()) "\"C:/Program Files/R/R-3.6.3/bin/Rscript.exe\" " else linuxPath+""
  }

  val path : String= {
    if (new File(windowsPath).exists()) windowsPath else linuxPath
  }


  def pdf2Png(pdfPath: String,file:String): Unit = {
    if (new File(windowsPath).exists()) pdfPng(pdfPath,file)
    else pdfToPng(pdfPath,file)
  }

  //PDF转png（windows）
  def pdfPng(pdfPath: String,file:String): Unit = {
    val pdfFiles = new File(pdfPath)
    val outFile = new File(file)
    val document = PDDocument.load(pdfFiles)
    val renderer = new PDFRenderer(document)
    ImageIO.write(renderer.renderImage(0, 3), "png", outFile)
    document.close()
  }

  //Linux下
  def pdfToPng(pdfpath:String,pngpath:String)={
    //$path/R_value_top_10_pairs.pdf $path/top.jpg
    val command = s"convert -density 300 "+ pdfpath + " " + pngpath
    val execCommand = new ExecCommand
    //exec需要指定结果输出路径的时候，不指定默认本地任务路径
    execCommand.exec(command)
  }



  val suffix: String = {
    if (new File(windowsPath).exists()) ".exe" else " "

  }

  def deleteDirectory(direcotry: String) = {
    try {
      FileUtils.deleteDirectory(new File(direcotry))
    } catch {
      case _: Throwable =>
    }
  }

  def deleteDirectory(direcotry: File) = {
    try {
      FileUtils.deleteDirectory(direcotry)
    } catch {
      case _: Throwable =>
    }
  }

  val scriptHtml =
    """
      |<script>
      |  $(function () {
      |        $("footer:first").remove()
      |        $("#content").css("margin","0")
      |       $(".linkheader>a").each(function () {
      |        var text=$(this).text()
      |        $(this).replaceWith("<span style='color: #222222;'>"+text+"</span>")
      |       })
      |
      |      $("tr").each(function () {
      |         var a=$(this).find("td>a:last")
      |      var text=a.text()
      |      a.replaceWith("<span style='color: #222222;'>"+text+"</span>")
      |     })
      |
      |       $("p.titleinfo>a").each(function () {
      |        var text=$(this).text()
      |        $(this).replaceWith("<span style='color: #606060;'>"+text+"</span>")
      |       })
      |
      |       $(".param:eq(1)").parent().hide()
      |       $(".linkheader").hide()
      |
      |    })
      |</script>
  """.stripMargin



}
