package tools

import java.io.File

import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, MyFile, MyMapTool, MyStringTool, Utils}

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object lefse2 extends MyFile with MyStringTool with MyMapTool{
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

    val elements = Json.obj("res"->Json.obj("resmfl"->"60", "resffs"->"7",
      "rescfs"->"7", "resdpi"->"300", "restitle"->"", "restfs"->"12", "reswidth"->"10",
      "resheight"->"4", "resorientation"->"h", "resnscl"->"1").toString,
      "cla"->Json.obj("clasc"->"", "claevl"->"1", "claml"->"6", "clacs"->"1.5",
        "clarsl"->"1", "clalstartl"->"2", "clalstopl"->"5", "claastartl"->"3", "claastopl"->"5",
        "clamaxps"->"6", "claminps"->"1", "claalpha"->"0.2", "clapew"->"0.25", "clascw"->"2",
        "clapcw"->"0.75", "clarsp"->"0.15", "clatitle"->"Cladogram", "clatfs"->"14", "clalfs"->"6",
        "claclfs"->"8", "cladpi"->"300").toString(),
      "fea"->Json.obj("feaf"->"diff", "feafname"->"", "feawidth"->"13", "feaheight"->"6",
        "featop"->"-1", "feabot"->"0", "featfs"->"14", "feacfs"->"10", "feasmean"->"y", "feasmedian"->"y",
        "feafs"->"10", "feadpi"->"300", "feaca"->"0").toString()).toString()

    val y = if(params("y") == "1") "一对一（限制较多）" else "一对多（限制较少）"
    val param = "类间因子Kruskal-Wallis秩检验的Alpha值：" + params("a") + "/子类间配对Wilcoxon秩检验的Alpha值：" +
      params("w") + "/差异特征LDA对数值的阈值：" + params("l") + "/多组分类分析的分析策略：" + y

    try {
      val command0 = if(file1.filename.contains("biom")) {
        file1.ref.getPath.moveToFile(otuFile)
        ""
      } else "biom convert -i " + tableFile.getAbsolutePath + " -o " + otuFile.getAbsolutePath + " --table-type=\"OTU table\" --to-json --process-obs-metadata taxonomy && \n"

      val runlefse = Utils.path + "R/lefse2.0/lefse_to_export/run_lefse.py " +
        dutyDir + "/out/lefse_format.txt " + dutyDir + "/out -a " + params("a") + " -w " +
        params("w") + " -l " + params("l") + " -y " + params("y") + " -e " + params("e") + " && \n "

      val sum_level = (1 to params("level").toInt).mkString(",")

      val commandpack1 = "export PATH=\"/usr/bin/:$PATH\" && \n" + command0 +
        "dos2unix " + groupFile.getAbsolutePath + " && \n " +
        "chmod -R 777 " + otuFile.getAbsolutePath + " && \n " +
        "summarize_taxa.py -i " + otuFile.getAbsolutePath + " -o " + dutyDir + "/out/tax_summary_a -L " + sum_level + " -a" + " && \n " +
        Utils.path + "R/lefse2.0/lefse_to_export/plot-lefse.pl -i " + dutyDir + "/out/tax_summary_a/ -o " + dutyDir + "/out -m " + groupFile.getAbsolutePath + " -g Group -l " + params("level") + " && \n " +
        Utils.path + "R/lefse2.0/lefse_to_export/format_input.py " + dutyDir + "/out/lefse_input.txt " + dutyDir + "/out/lefse_format.txt  -f r -c 1 -u 2 -o 1000000" + " && \n " +
        runlefse +
        "/mnt/sdb/ww/CloudPlatform/tools/R/bin/Rscript " + Utils.path + "R/lefse2.0/lefse_to_export/ida_filter.R -i " + dutyDir + "/out/lefse_LDA.xls" + " -o " + dutyDir + "/out/lefse_LDA_diff.xls && \n" +
        Utils.path + "R/lefse2.0/lefse_to_export/plot_res.py " + dutyDir + "/out/lefse_LDA.xls " + dutyDir + "/out/lefse_LDA.pdf --dpi 300 --format pdf --width 10" + " && \n " +
        Utils.path + "R/lefse2.0/lefse_to_export/plot_cladogram.py " + dutyDir + "/out/lefse_LDA.xls " + dutyDir + "/out/lefse_LDA.cladogram.pdf --format pdf --dpi 300 --max_lev 6 --class_legend_font_size 8 --right_space_prop 0.15" + " && \n " +
        Utils.path + "R/lefse2.0/lefse_to_export/plot_features.py " + dutyDir + "/out/lefse_format.txt " + dutyDir + "/out/lefse_LDA.xls " + dutyDir + "/out/lefse_LDA.features.pdf --format pdf --dpi 300 --width 13"

      println(commandpack1)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),commandpack1)
      val execCommand = new ExecCommand
      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "lefse分析2.0", input, param, elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val res=elements("res").jsonToMap
    val cla=elements("cla").jsonToMap
    val fea=elements("fea").jsonToMap
    val lefse_LDA= if(new File(dutyDir+"/out/lefse_LDA.png").length()==0) "false" else "true"
    val pics= ((dutyDir+"/out/lefse_LDA.pdf",dutyDir+"/out/lefse_LDA.cladogram.pdf",dutyDir+"/out/lefse_LDA.features.pdf"))
    val fname = FileUtils.readLines(new File(dutyDir + "/out/lefse_LDA_diff.xls")).asScala.map{_.replaceAll("\"","").split("\t")(0)}.sorted
    Json.obj("pics"->pics,"res"->res,"cla"->cla,"fea"->fea,"fname"->fname)
  }

  def ReDrawRes(dutyDir: String, newElements: Map[String, String], elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val elejson=Json.obj("res"->Json.obj("resmfl"->newElements("resmfl"),
      "resffs"->newElements("resffs"), "rescfs"->newElements("rescfs"), "resdpi"->newElements("resdpi"),
      "restitle"->newElements("restitle"), "restfs"->newElements("restfs"), "reswidth"->newElements("reswidth"),
      "resheight"->newElements("resheight"), "resorientation"->newElements("resorientation"),
      "resnscl"->newElements("resnscl")).toString(),
      "cla"->elements("cla"),"fea"->elements("fea")).toString()
    
    val title =
      if(!newElements("restitle").equals("")) " --title \"" + newElements("restitle") + "\""
      else ""
    val lsrs = if(newElements("resorientation") == "v") " --left_space 0.02 --right_space 0.98" else ""

    val command = "export PATH=\"/usr/bin/:$PATH\" && \n" +
      Utils.path + "R/lefse2.0/lefse_to_export/plot_res.py " + dutyDir +
      "/out/lefse_LDA.xls " + dutyDir + "/out/lefse_LDA.pdf --format pdf --feature_font_size " +
      newElements("resffs") + " --dpi " + newElements("resdpi") + title + " --title_font_size " + newElements("restfs") +
      " --width " + newElements("reswidth") + " --height " + newElements("resheight") + " --orientation " +
      newElements("resorientation") + lsrs + " --subclades " + newElements("resnscl") + " --class_legend_font_size " +
      newElements("rescfs") + " --max_feature_len " + newElements("resmfl")

    println(command)

    //先放入sh，在运行
    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/runres.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/runres.sh",dutyDir+"/temp")
    
    var valid = ""
    var pics = ""
    if (execCommand.isSuccess) {
      valid = "true"
    } else {
      valid = "false"
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->elejson)
  }
  
  def ReDrawCla(dutyDir: String, newElements: Map[String, String], elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val elejson=Json.obj("res"->elements("res"),
      "cla"->Json.obj("clasc"->newElements("clasc"), "claevl"->newElements("claevl"), "claml"->newElements("claml"),
        "claclv"->newElements("claclv"), "clacs"->newElements("clacs"), "clarsl"->newElements("clarsl"), "clalstartl"->newElements("clalstartl"),
        "clalstopl"->newElements("clalstopl"), "claastartl"->newElements("claastartl"), "claastopl"->newElements("claastopl"),
        "clamaxps"->newElements("clamaxps"), "claminps"->newElements("claminps"), "claalpha"->newElements("claalpha"),
        "clapew"->newElements("clapew"), "clascw"->newElements("clascw"), "clapcw"->newElements("clapcw"), "clarsp"->newElements("clarsp"),
        "clatitle"->newElements("clatitle"), "clatfs"->newElements("clatfs"), "clalfs"->newElements("clalfs"),
        "claclfs"->newElements("claclfs"), "cladpi"->newElements("cladpi")).toString(),
      "fea"->elements("fea")).toString()

    val clasc = if(newElements("clasc") == "") "" else " --sub_clade " + newElements("clasc")
    val title = if(newElements("clatitle") == "") " --title \" \"" else " --title \"" + newElements("clatitle") + "\""

    val command = "export PATH=\"/usr/bin/:$PATH\" && \n" +
      Utils.path + "R/lefse2.0/lefse_to_export/plot_cladogram.py " + dutyDir +
      "/out/lefse_LDA.xls " + dutyDir + "/out/lefse_LDA.cladogram.pdf --format pdf" + clasc +
      " --expand_void_lev " + newElements("claevl") + " --max_lev " + newElements("claml") + " --clade_sep " +
      newElements("clacs") + " --radial_start_lev " + newElements("clarsl") + " --labeled_start_lev " + newElements("clalstartl") +
      " --labeled_stop_lev " + newElements("clalstopl") + " --abrv_start_lev " + newElements("claastartl") +
      " --abrv_stop_lev " + newElements("claastopl") + " --max_point_size " + newElements("clamaxps") +
      " --min_point_size " + newElements("claminps") + " --alpha " + newElements("claalpha") + " --point_edge_width " +
      newElements("clapew") + " --siblings_connector_width " + newElements("clascw") + " --parents_connector_width " +
      newElements("clapcw") + " --right_space_prop " + newElements("clarsp") + title + " --title_font_size " +
      newElements("clatfs") + " --label_font_size " + newElements("clalfs") + " --class_legend_font_size " +
      newElements("claclfs") + " --dpi " + newElements("cladpi") + " --class_legend_vis " + newElements("claclv")

    println(command)

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/runcla.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/runcla.sh",dutyDir+"/temp")

    var valid = ""
    var pics = ""
    if (execCommand.isSuccess) {
      valid = "true"
    } else {
      valid = "false"
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->elejson)
  }

  def ReDrawFea(dutyDir: String, newElements: Map[String, String], elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val elejson=Json.obj("res"->elements("res"), "cla"->elements("cla"),
      "fea"->Json.obj("feaf"->newElements("feaf"), "feafname"->newElements("feafname"), "feawidth"->newElements("feawidth"),
        "feaheight"->newElements("feaheight"), "featop"->newElements("featop"), "feabot"->newElements("feabot"), "featfs"->newElements("featfs"),
        "feacfs"->newElements("feacfs"), "feasmean"->newElements("feasmean"), "feasmedian"->newElements("feasmedian"),
        "feafs"->newElements("feafs"), "feadpi"->newElements("feadpi"), "feaca"->newElements("feaca")).toString()).toString()

    val fname = if(newElements("feaf") == "one") " --feature_name " + newElements("feafname") else ""

    val command = "export PATH=\"/usr/bin/:$PATH\" && \n" +
      Utils.path + "R/lefse2.0/lefse_to_export/plot_features.py " + dutyDir +
      "/out/lefse_format.txt " + dutyDir + "/out/lefse_LDA.xls " + dutyDir +
      "/out/lefse_LDA.features.pdf --format pdf -f " + newElements("feaf") + fname + " --width " +
      newElements("feawidth") + " --height " + newElements("feaheight") + " --top " + newElements("featop") + " --bot " +
      newElements("feabot") + " --title_font_size " + newElements("featfs") + " --class_font_size " + newElements("feacfs") +
      " --subcl_mean " + newElements("feasmean") + " --subcl_median " + newElements("feasmedian") + " --font_size " +
      newElements("feafs") + " --dpi " + newElements("feadpi") + " --class_angle " + newElements("feaca")

    println(command)

    //先放入sh，在运行
    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/runfea.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/runfea.sh",dutyDir+"/temp")

    var valid = ""
    var pics = ""
    if (execCommand.isSuccess) {
      valid = "true"
    } else {
      valid = "false"
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->elejson)
  }


}
