package utils

import java.io.File

import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils

import scala.util.Try

/**
 * Created by Administrator on 2019/9/12
 */
trait MyStringTool {

  implicit class MyString(v: String) {

    def reformFile = {
      v.split("\n").map{line =>
        line.trim.split("\t").mkString("\t")
      }.mkString("\n")
    }

    def jsonToMap : Map[String, String] = {
      scala.util.parsing.json.JSON.parseFull(v).get.asInstanceOf[Map[String, String]]
    }

    def isWindows = {
      System.getProperty("os.name") match {
        case x if x.contains("Windows") => true
        case _ => false
      }
    }

    def isInt = {
      Try(v.toInt).toOption match {
        case Some(value) => true
        case None => false
      }
    }

    def unixPath = {
      v.slashL2R.replaceAll("D:", "/mnt/d").
        replaceAll("E:", "/mnt/e").replaceAll("C:", "/mnt/c").
        replaceAll("G:", "/mnt/g")
    }

    def slashL2R = {
      v.replace("\\", "/")
    }

    def startWithsIgnoreCase(prefix: String) = {
      v.toLowerCase.startsWith(prefix.toLowerCase)
    }

    def toFile(file: File, encoding: String = "UTF-8", append: Boolean = false) = {
      FileUtils.writeStringToFile(file, v, encoding, append)
      file
    }

    def trimQuote = {
      v.replaceAll("^\"", "").replaceAll("\"$", "")
    }

    def mySplit(sep: String = "\t") = {
      v.split(sep).toList
    }

    def isBlank = StringUtils.isBlank(v)

    def wsl = {
      if (isWindows) s"wsl ${v}" else v
    }

    def isValidRVar = {
      (!v.matches("^\\.\\d+.*$")) && !(v.matches("^[\\d_]+.*$")) && (v.matches("^[\\w\\.]+$"))
    }


    def fileNamePrefix: String = {
      val index = v.lastIndexOf(".")
      v.substring(0, index)
    }

    def splitByTab = {
      v.mySplit("\t")
    }

    def isDoubleP(p: Double => Boolean): Boolean = {
      Try(v.toDouble).toOption match {
        case Some(value) => p(value)
        case None => false
      }
    }

    def isDouble: Boolean = {
      Try(v.toDouble).toOption match {
        case Some(value) => true
        case None => false
      }
    }

    def like(piece: String) = {
      if (piece.isEmpty) true else v.contains(piece)
    }

  }

}
