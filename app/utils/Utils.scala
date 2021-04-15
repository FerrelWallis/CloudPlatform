package utils

import java.io.File

import com.aliyuncs.DefaultAcsClient
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest
import com.aliyuncs.http.MethodType
import com.aliyuncs.profile.DefaultProfile
import dao.softDao
import javax.imageio.ImageIO
import models.Tables.SoftRow
import org.apache.commons.io.FileUtils
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import play.api.libs.json.Json

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Random

object Utils{

  val windowsPath="F:/CloudPlatform/"
  val linuxPath="/mnt/sdb/ww/CloudPlatform/"
  //bak
//  val linuxPath="/mnt/sdb/ww/bak/CloudPlatform/"

//  val RPath : String= {
//    if (new File(windowsPath).exists()) "\"C:/Program Files/R/R-3.6.3/bin/Rscript.exe\" " else linuxPath+""
//  }

  val path : String= {
    if (new File(windowsPath).exists()) windowsPath else linuxPath
  }

  def jsonToMap(json:String): Map[String, String] = {
    scala.util.parsing.json.JSON.parseFull(json).get.asInstanceOf[Map[String, String]]
  }

  def mapToJson(map:Map[String,String]): String = {
    Json.toJson(map).toString()
  }


  //短信验证
  def sendMessage(phone: String,akid:String,aksecret:String) = {
    System.setProperty("sun.net.client.defaultConnectTimeout", "10000")
    System.setProperty("sun.net.client.defaultReadTimeout", "10000")
    val product = "Dysmsapi"
    val domain = "dysmsapi.aliyuncs.com"
    val profile = DefaultProfile.getProfile("cn-hangzhou", akid, aksecret)
    DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", product, domain)
    val acsClient = new DefaultAcsClient(profile)
    val request = new SendSmsRequest
    request.setMethod(MethodType.POST)
    request.setPhoneNumbers(phone)
    request.setSignName("微课云")
    request.setTemplateCode("SMS_195705252")
    val code = productValidCode
    val json = Json.obj("code" -> code)
    val jsonString = Json.stringify(json)
    request.setTemplateParam(jsonString)
    val sendSmsResponse = acsClient.getAcsResponse(request)
    val responseCode = sendSmsResponse.getCode
    if (sendSmsResponse.getCode != null && sendSmsResponse.getCode.equals("OK")) {
      (true, code, responseCode)
    } else {
      println(sendSmsResponse.getCode)
      (false, code, responseCode)
    }
  }

  def productValidCode = {
    "000000".map { i =>
      (Random.nextInt(10) + '0').toChar
    }
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
    if(file.indexOf("png")>=0) ImageIO.write(renderer.renderImage(0, 3), "png", outFile)
    else ImageIO.write(renderer.renderImage(0, 3), "tiff", outFile)
    document.close()
  }

  //Linux下
  def pdfToPng(pdfpath:String,pngpath:String)={
    //$path/R_value_top_10_pairs.pdf $path/top.jpg
    val command = s"convert -density 300 "+ pdfpath + " " + pngpath
    val execCommand = new ExecCommand
    //exec需要指定结果输出路径的时候，不指定默认本地任务路径
    val del = "rm -rf " + pngpath + ".gz"
    val gz = s"gzip " + pngpath
    if(pngpath.indexOf("tiff") >= 0) {
      println(command)
      println(gz)
      execCommand.exec(Array(del, command, gz))
    }
    else execCommand.exec(command)
  }

  //Linux下
  def psToPdf(pspath:String,pdfpath:String)={
    //$path/R_value_top_10_pairs.pdf $path/top.jpg
    val command = s"ps2pdf "+ pspath + " " + pdfpath
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
