package utils

import java.io.File

import org.apache.commons.io.FileUtils

import scala.collection.mutable
import scala.collection.JavaConverters._

trait MyFile {

  implicit class myFile(file: File)(implicit encoding: String = "UTF-8") {

    def readLines: mutable.Buffer[String] = {
      FileUtils.readLines(file, encoding).asScala
    }

    def readFileToString: String = {
      FileUtils.readFileToString(file, encoding)
    }

    def writeLines(buffer: mutable.Buffer[String]): Unit = {
      FileUtils.writeLines(file, buffer.asJava, encoding,false)
    }


    def writeStringToFile(text: String): Unit = {
      FileUtils.writeStringToFile(file, text, encoding)
    }

    def mkdir = {
      file.mkdir()
    }

    def mkdirs = {
      file.mkdirs()
    }

    def unixPath: String = {
      file.getAbsolutePath.replaceAll("\\\\", "/")
    }

    def moveToFile(dest: File): Unit = {
      val content = file.readLines.filter(!_.trim.isEmpty).map{line =>
        line.trim.split("\t").mkString("\t")
      }.mkString("\n")
      FileUtils.writeStringToFile(dest, content)
//      FileUtils.moveFile(file, dest)
    }

    def creatZip(path:String): Unit ={
      file.createNewFile()
      CompressUtil.zip(path,file.getAbsolutePath)
    }

    def creatUserDir(uid:String,taskname:String): String ={
      val userDutyDir: String =Utils.path+"users/"
      new File(userDutyDir+uid+"/"+taskname).mkdir()
      new File(userDutyDir+uid+"/"+taskname+"/out").mkdir()
      new File(userDutyDir+uid+"/"+taskname+"/temp").mkdir()
      userDutyDir+uid+"/"+taskname
    }

    def delete: Unit = {
      if (file.isDirectory) {
        FileUtils.deleteDirectory(file)
      } else {
        file.delete()
      }
    }

    def isDir: Boolean = {
      try{
      if(file.isDirectory){
        true
      }else{
        false
      }
      }catch {
        case e:Exception=>false
      }

    }

  }

  implicit class myPath(path: String) extends myFile(new File(path)) {

    def toFile: File = {
      new File(path)
    }
  }

}
