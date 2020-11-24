package utils
import java.io.File

import Implicits._
import utils.Utils.{pdfPng, pdfToPng, windowsPath}
import java.io.File

import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils

import scala.util.Try

trait DealOutcome {

  implicit class Outcome(path:String) {

    def pdf2PngTiff(pdfPath: String,file:String): Unit = {
      if (new File(windowsPath).exists()) pdfPng(pdfPath,file)
      else pdfToPng(pdfPath,file)
    }

    def updateLog(start:String,finish:String) = {
      FileUtils.writeStringToFile(new File(path,"log.txt"),"Start Time:"+start+"\n\nFinish Time:"+finish+"\n\n运行成功！")
    }

    def updateFinish = {
    }

    def creatZip= {
      val target=path+"/outcome.zip"
      new File(target).createNewFile()
      CompressUtil.zip(path+"/out",target)
    }

  }

}
