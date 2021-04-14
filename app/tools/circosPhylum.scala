package tools

import java.io.File

import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, MyFile, MyMapTool, MyStringTool, Utils}

import scala.collection.JavaConverters._
import scala.collection.mutable

object circosPhylum extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    var input=""
    val tableFile=new File(dutyDir,"table.txt")
    val otuFile=new File(dutyDir,"otu_taxa_table.biom")
    val file1 = request.body.file("table1").get
    input=input+file1.filename+"/"
    file1.ref.getPath.moveToFile(tableFile)
    tableFile.setExecutable(true,false)
    tableFile.setReadable(true,false)
    tableFile.setWritable(true,false)
    val groupFile=new File(dutyDir,"map.txt")
    val file2 = request.body.file("group").get
    input+=file2.filename
    file2.ref.getPath.moveToFile(groupFile)
    groupFile.setExecutable(true,false)
    groupFile.setReadable(true,false)
    groupFile.setWritable(true,false)

    val elements = Json.obj("gs"->params("gs"), "otmin"->params("otmin"), "otmax"->params("otmax"), "df"->"TRUE",
      "ds"->"TRUE", "dl"->"TRUE", "flpstyle"->"auto", "vjust"->"10", "vjustunit"->"mm",
      "th1"->"0.05", "th2"->"0.05", "th3"->"0.03", "width"->"12", "height"->"10",
      "spos"->"90", "flpmaxlen"->"60", "flpfont"->"0.4", "slpfont"->"0.2",
      "tlpfont"->"0.4").toString()

    val otparam = "作图的OTU丰度取值范围：" + params("otmin") + ":" + (if(params("otmax") == "") "NULL" else params("otmax"))

    val param = "选择作图的OTU层级：" + params("L") + "/" + otparam + "/间距设置：" + params("gs")

    try {
      val command0 = if(file1.filename.contains("biom")) {
        file1.ref.getPath.moveToFile(otuFile)
        ""
      } else "biom convert -i " + tableFile.getAbsolutePath + " -o " + otuFile.getAbsolutePath + " --table-type=\"OTU table\" --to-json --process-obs-metadata taxonomy && \n"

      val ot = " -ot " + params("otmin") + ":" + (if(params("otmax") == "") "NULL" else params("otmax"))

      val commandpack1 = command0 + "dos2unix " + groupFile.getAbsolutePath + " && \n " +
        "chmod -R 777 " + otuFile.getAbsolutePath + " && \n " +
        "summarize_taxa.py -i " + otuFile.getAbsolutePath + " -o " + dutyDir + "/out -L " + params("L") + " -a" + " && \n " +
        Utils.path + "R/circos_phylum/sum_tax.pl -i " + dutyDir + "/out/otu_taxa_table_L" + params("L") + ".txt -o " + dutyDir + "/out/phylum.xls && \n " +
        "Rscript " + Utils.path + "R/circos_phylum/circos_phylum.R -i " + dutyDir + "/out/phylum.xls" + " -g " +
        groupFile.getAbsolutePath + " -o " + dutyDir + "/out" + ot + " -gs " + params("gs")

      println(commandpack1)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),commandpack1)
      val execCommand = new ExecCommand
      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/phylum_circos.pdf",dutyDir+"/out/phylum_circos.png")
        Utils.pdf2Png(dutyDir+"/out/phylum_circos.pdf",dutyDir+"/out/phylum_circos.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "Circos物种关系图", input, param, elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val groupnum = FileUtils.readLines(new File(dutyDir + "/map.txt")).asScala.map{_.replaceAll("\"","").split("\t")(1)}.drop(1).distinct.length
    val (names,colors) = FileUtils.readLines(new File(dutyDir + "/out/colors.xls")).asScala.map{x=>
      val temp = x.replaceAll("\"","").split("\t")
      (temp.head,temp.last)
    }.unzip
    val otuid = names.take(names.length - groupnum)
    val otucolors = colors.take(names.length - groupnum)
    val group = names.takeRight(groupnum)
    val groupcolors = colors.takeRight(groupnum)
    val pics = dutyDir + "/out/phylum_circos.pdf"
    Json.obj("pdfUrl"->pics, "elements"->elements, "otuid"->otuid,"otucolor"->otucolors,"group"->group,"groupcolor"->groupcolors)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val tableFile=new File(dutyDir+"/out","phylum.xls")
    val groupFile=new File(dutyDir,"map.txt")

    val newelements = Json.obj("gs"->elements("gs"), "otmin"->elements("otmin"), "otmax"->elements("otmax"),
      "df"->elements("df"), "ds"->elements("ds"), "dl"->elements("dl"), "flpstyle"->elements("flpstyle"),
      "vjust"->elements("vjust"), "vjustunit"->elements("vjustunit"), "th1"->elements("th1"), "th2"->elements("th2"),
      "th3"->elements("th3"), "width"->elements("width"), "height"->elements("height"), "spos"->elements("spos"),
      "flpmaxlen"->elements("flpmaxlen"), "flpfont"->elements("flpfont"), "slpfont"->elements("slpfont"),
      "tlpfont"->elements("tlpfont")).toString()

    val ot = " -ot \"" + elements("otmin") + ":" + (if(elements("otmax") == "") "NULL" else elements("otmax")) + "\""

    val command = "Rscript " + Utils.path + "R/circos_phylum/circos_phylum.R -i " + tableFile.getAbsolutePath +
      " -g " + groupFile.getAbsolutePath + " -o " + dutyDir + "/out -df " + elements("df") + " -ds " + elements("ds") +
      " -dl " + elements("dl") + " -flp \"" + elements("vjust") + " " + elements("vjustunit") + "@" +
      elements("flpmaxlen") + "@" + elements("flpfont") + "@" + elements("flpstyle") +"\" -slp " + elements("slpfont") +
      " -tlp " + elements("tlpfont") + " -th \"" + elements("th1") + ":" + elements("th2") + ":" + elements("th3") +
      "\" -spos " + elements("spos") + " -gs " + elements("gs") + ot + " -oc \"" + elements("otucolor") + "\" -gc \"" +
      elements("groupcolor") + "\" -is " + elements("height") + ":" + elements("width")

    println(command)

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    val groupnum = FileUtils.readLines(new File(dutyDir + "/map.txt")).asScala.map{_.replaceAll("\"","").split("\t")(1)}.drop(1).distinct.length
    val (names,colors) = FileUtils.readLines(new File(dutyDir + "/out/colors.xls")).asScala.map{x=>
      val temp = x.replaceAll("\"","").split("\t")
      (temp.head,temp.last)
    }.unzip

    val otuid = names.take(names.length - groupnum)
    val otucolors = colors.take(names.length - groupnum)
    val group = names.takeRight(groupnum)
    val groupcolors = colors.takeRight(groupnum)
    
    var valid = ""
    var msg = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/phylum_circos.pdf",dutyDir+"/out/phylum_circos.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/phylum_circos.pdf",dutyDir+"/out/phylum_circos.tiff") //替换图片
      valid = "true"
    } else {
      valid = "false"
      msg = execCommand.getErrStr
    }
    Json.obj("valid"->valid, "elements"->newelements, "message"->msg, "otuid"->otuid,"otucolor"->otucolors,"group"->group,"groupcolor"->groupcolors)
  }

}
