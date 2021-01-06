package controllers

import java.io.{File, FileInputStream}
import java.nio.file.{Files, Path}

import dao.dutyDao
import javax.inject.Inject
import org.apache.commons.io.FileUtils
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Headers, Result}
import utils.{CompressUtil, ExecCommand, MyStringTool, Utils}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.collection.JavaConverters._
import utils.Implicits._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class RFunctionController @Inject()(cc: ControllerComponents, dutydao: dutyDao, rservice: RService, dutyController: DutyController)(implicit exec: ExecutionContext) extends AbstractController(cc) with MyStringTool {

  val userDutyDir: String =Utils.path+"users/"

  case class TaxFunData(taskname:String)

  val TaxFunForm: Form[TaxFunData] =Form(
    mapping (
      "taskname"->text
    )(TaxFunData.apply)(TaxFunData.unapply)
  )


  //GO && KEGG
  case class GoData(taskname:String,species:String,txdata1:String)

  val GoForm: Form[GoData] =Form(
    mapping (
      "taskname"->text,
      "species"->text,
      "txdata1"->text
    )(GoData.apply)(GoData.unapply)
  )

  def doGo(types:String,refer:String,table:String)=Action(parse.multipartFormData){implicit request=>
    val data=GoForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=rservice.creatUserDir(id,data.taskname)
    val tableFile=new File(dutyDir,"table.txt")
    val koFile=new File(dutyDir,"kogo.txt")

    val input=
      if(table=="2"){
        val file1=request.body.file("table1").get
        file1.ref.moveTo(tableFile)
        if(refer=="TRUE"){
          file1.filename
        }else{
          val file2=request.body.file("table2").get
          file2.ref.moveTo(koFile)
          file1.filename+"/"+file2.filename
        }
      } else {
        FileUtils.writeStringToFile(tableFile, data.txdata1)
        if(refer=="TRUE"){
          "无"
        }else{
          val file3=request.body.file("table2").get
          file3.ref.moveTo(koFile)
          file3.filename
        }
      }

    val (param,model)=
      if(refer=="TRUE") {
        if(types=="ko") ("使用已有参考:"+refer+"/选择物种："+data.species,"ko_data.jar -m "+data.species)
        else ("使用已有参考:"+refer+"/选择物种："+data.species,"go_data.jar -m "+data.species)
      }
      else {
        if(types=="ko") ("使用已有参考:"+refer,"ko_data_file.jar -pathway "+koFile.getAbsolutePath)
        else ("使用已有参考:"+refer,"Go_data_file.jar -go "+koFile.getAbsolutePath)
      }

    val elements= Json.obj("n"->"15","br"->"0.9","g"->"FALSE","sm"->"50","width"->"20",
      "height"->"14", "xts"->"13","yts"->"14","lts"->"15","dpi"->"300").toString()

    Future{
      val (command1,command2,start)=
        if(types=="ko")
          ("java -jar " + Utils.path + "R/gokegg/data/" + model + " -i " + tableFile.getAbsolutePath +
            " -o " + dutyDir + "/out/ko" , "Rscript " + Utils.path + "R/gokegg/plot/Ko_dodge_plot.R -i " + dutyDir +
            "/out/ko.Ko.bar.dat" + " -o " + dutyDir + "/out" + " -in ko_dodge -if pdf -sm 50 -n 15",
            dutyController.insertDuty(data.taskname,id,"KEGG","KEGG富集分析",input,param,elements))
        else
          ("java -jar " + Utils.path + "R/gokegg/data/" + model + " -i " + tableFile.getAbsolutePath +
            " -o " + dutyDir + "/out/go" , "Rscript " + Utils.path + "R/gokegg/plot/Go_stack_plot.R -i " + dutyDir +
            "/out/go.Go.bar.dat" + " -o " + dutyDir + "/out" + " -in go_stack -if pdf -sm 50 -n 15",
            dutyController.insertDuty(data.taskname,id,"GO","GO富集分析",input,param,elements))
      val command3 = "dos2unix " + tableFile.getAbsolutePath
      val command4 = "dos2unix " + koFile.getAbsolutePath
      val commandpack1= if(koFile.exists()) Array(command3,command4) else Array(command3)
      val commandpack2=Array(command1,command2)
      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),commandpack1.mkString(" && \n")+" && \n"+commandpack2.mkString(" && \n"))

      //      val execCommand1 = new ExecCommand
      //      execCommand1.exec(commandpack1)

      val execCommand = new ExecCommand
      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

      if(execCommand.isSuccess){
        //        FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),commandpack1.mkString(" && \n")+" && \n"+commandpack2.mkString(" && \n"))
        println(command1)
        println(command2)

        if(types=="ko"){
          Utils.pdf2Png(dutyDir+"/out/ko.Ko.enrich.pdf",dutyDir+"/out/ko.Ko.enrich.png")
          Utils.pdf2Png(dutyDir+"/out/ko.Ko.enrich.pdf",dutyDir+"/out/ko.Ko.enrich.tiff")
          Utils.pdf2Png(dutyDir+"/out/ko_dodge.pdf",dutyDir+"/out/ko_dodge.png")
          Utils.pdf2Png(dutyDir+"/out/ko_dodge.pdf",dutyDir+"/out/ko_dodge.tiff")
        }else {
          Utils.pdf2Png(dutyDir+"/out/go.Go.enrich.pdf",dutyDir+"/out/go.Go.enrich.png")
          Utils.pdf2Png(dutyDir+"/out/go.Go.enrich.pdf",dutyDir+"/out/go.Go.enrich.tiff")
          Utils.pdf2Png(dutyDir+"/out/go_stack.pdf",dutyDir+"/out/go_stack.png")
          Utils.pdf2Png(dutyDir+"/out/go_stack.pdf",dutyDir+"/out/go_stack.tiff")
        }
        rservice.creatZip(dutyDir)
        val finish=dutyController.updateFini(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\nFinish Time:"+finish+"\n\n运行成功！")
      }else{
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readGoData(types:String,taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val elements=rservice.jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)
    val (groupname,groupcolor) = FileUtils.readLines(new File(path+"/out/color.xls")).asScala.map{line=>
      (line.split("\t")(0).replaceAll("\"",""),
        line.split("\t")(1).replaceAll("\"",""))
    }.unzip

    val pics= if(types=="ko") (path+"/out/ko_dodge.png",path+"/out/ko.Ko.enrich.png") else (path+"/out/go_stack.png",path+"/out/go.Go.enrich.png")
    Ok(Json.obj("pics"->pics,"elements"->elements,"groupname"->groupname.tail,"groupcolor"->groupcolor.tail))
  }

  case class ReGoData(g:String,n:String,sm:String,br:String,color:String,width:String,height:String,dpi:String,xts:String,yts:String,lts:String)

  val ReGoForm: Form[ReGoData] =Form(
    mapping (
      "g"->text,
      "n"->text,
      "sm"->text,
      "br"->text,
      "color"->text,
      "width"->text,
      "height"->text,
      "dpi"->text,
      "xts"->text,
      "yts"->text,
      "lts"->text
    )(ReGoData.apply)(ReGoData.unapply)
  )

  def redrawGO(taskname:String,types:String)=Action(parse.multipartFormData) { implicit request =>
    val data=ReGoForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname
    val (r,i,g,in)=
      if(types=="ko")
        ("R/gokegg/plot/Ko_dodge_plot.R",dutyDir + "/out/ko.Ko.bar.dat"," -g " + data.g,"ko_dodge")
      else ("R/gokegg/plot/Go_stack_plot.R",dutyDir + "/out/go.Go.bar.dat","","go_stack")

    val elements= Json.obj("n"->data.n,"br"->data.br,"g"->data.g,"sm"->data.sm,"width"->data.width,
      "height"->data.height,"xts"->data.xts,"yts"->data.yts,"lts"->data.lts,"dpi"->data.dpi).toString()
    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    val cs=data.color+":#E41A1C:#FFC0CB:#1E90FF:#00BFFF:#FF8C00:#FFDEAD:#4DAF4A:#90EE90:#9692C3:#CDB4FF:#40E0D0:#00FFFF"

    val command = "Rscript " + Utils.path + r + " -i "+ i + " -o " + dutyDir + "/out -n " + data.n + " -sm " +
      data.sm + " -br " + data.br + " -cs " + cs + " -is " + data.width + ":" + data.height + " -dpi " +
      data.dpi + " -xts sans:bold.italic:" + data.xts + " -yts sans:bold.italic:" + data.yts + " -lts sans:bold.italic:" +
      data.lts + " -in " + in + g + " -if pdf"

    println(command)
    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(command,dutyDir+"/temp")

    if (execCommand.isSuccess) {
      val (groupname,groupcolor) = FileUtils.readLines(new File(dutyDir + "/out/color.xls")).asScala.map{line=>
        (line.split("\t")(0).replaceAll("\"",""),line.split("\t")(1).replaceAll("\"",""))
      }.unzip
      Utils.pdf2Png(dutyDir+"/out/"+in+".pdf",dutyDir+"/out/"+in+".png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/"+in+".pdf",dutyDir+"/out/"+in+".tiff") //替换图片
      rservice.creatZip(dutyDir) //替换压缩文件包
      val pics=dutyDir+"/out/"+in+".png"
      Ok(Json.obj("valid"->"true","pics"->pics,"groupname"->groupname.tail,"groupcolor"->groupcolor.tail))
    } else {
      Ok(Json.obj("valid"->"false", "message"->execCommand.getErrStr))
    }
  }



  //Lefse
  def doLefse=Action(parse.multipartFormData) { implicit request =>
    val data = TaxFunForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=rservice.creatUserDir(id,data.taskname)
    var input=""

    val tableFile=new File(dutyDir,"table.txt")
    val otuFile=new File(dutyDir,"otu_taxa_table.biom")
    val file1 = request.body.file("table1").get
    input=input+file1.filename+"/"
    file1.ref.copyTo(tableFile)
    tableFile.setExecutable(true,false)
    tableFile.setReadable(true,false)
    tableFile.setWritable(true,false)

    val groupFile=new File(dutyDir,"map.txt")
    val file2 = request.body.file("group").get
    input+=file2.filename
    file2.ref.moveTo(groupFile)
    groupFile.setExecutable(true,false)
    groupFile.setReadable(true,false)
    groupFile.setWritable(true,false)

    val start=dutyController.insertDuty(data.taskname,id,"LEF","lefse分析",input,"/","")

    Future{
      val command0 = if(file1.filename.contains("biom")) {
        file1.ref.copyTo(otuFile)
        ""
      } else
        "biom convert -i " + tableFile.getAbsolutePath + " -o " + otuFile.getAbsolutePath + " --table-type=\"OTU table\" --to-json --process-obs-metadata taxonomy && \n"

      val command1 = "sed -i s'/\\r//g' "+groupFile.getAbsolutePath+" && \n"

      val command2 = "chmod -R 777 " + otuFile.getAbsolutePath + "&& \n"+
        "java -jar "+Utils.path+"R/lefse/lefse_yiti-1.0-SNAPSHOT.jar -i "+otuFile.getAbsolutePath+" -o "+dutyDir+"/out -m "+dutyDir+"/map.txt\n"

      val commandpack=command0+command1+command2

      println(commandpack)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),commandpack)
      val execCommand = new ExecCommand
      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

      println(commandpack)

      //      val execCommand = new ExecCommand
      //      execCommand.exec(commandpack)

      if (execCommand.isSuccess) {
        Utils.pdf2Png(dutyDir+"/out/lefse_LDA.cladogram.pdf",dutyDir+"/out/lefse_LDA.cladogram.png")
        Utils.pdf2Png(dutyDir+"/out/lefse_LDA.pdf",dutyDir+"/out/lefse_LDA.png")
        val finish=dutyController.updateFini(id,data.taskname)
        Utils.pdf2Png(dutyDir+"/out/lefse_LDA.cladogram.pdf",dutyDir+"/out/lefse_LDA.cladogram.tiff")
        Utils.pdf2Png(dutyDir+"/out/lefse_LDA.pdf",dutyDir+"/out/lefse_LDA.tiff")
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\nFinish Time:"+finish+"\n\n运行成功！")
        rservice.creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readLefse(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname
    println(new File(path+"/out/lefse_LDA.png").length())
    val lefse_LDA= if(new File(path+"/out/lefse_LDA.png").length()==0) "false" else "true"
    //获取图片
    val pics= ((path+"/out/lefse_LDA.png",path+"/out/lefse_LDA.cladogram.png"))
    Ok(Json.obj("pics"->pics,"lefse_LDA"->lefse_LDA))
  }



  //Lefse2.0
  case class Lefse2Data(taskname:String, a:String, w:String, l:String, y:String, e:String, level:String)

  val Lefse2Form: Form[Lefse2Data] =Form(
    mapping (
      "taskname"->text,
      "a"->text,
      "w"->text,
      "l"->text,
      "y"->text,
      "e"->text,
      "level"->text
    )(Lefse2Data.apply)(Lefse2Data.unapply)
  )

  def doLefse2=Action(parse.multipartFormData) { implicit request =>
    val data = Lefse2Form.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=rservice.creatUserDir(id,data.taskname)
    var input=""

    val tableFile=new File(dutyDir,"table.txt")
    val otuFile=new File(dutyDir,"otu_taxa_table.biom")
    val file1 = request.body.file("table1").get
    input=input+file1.filename+"/"
    file1.ref.copyTo(tableFile)
    tableFile.setExecutable(true,false)
    tableFile.setReadable(true,false)
    tableFile.setWritable(true,false)

    val groupFile=new File(dutyDir,"map.txt")
    val file2 = request.body.file("group").get
    input+=file2.filename
    file2.ref.moveTo(groupFile)
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

    val y = if(data.y == "1") "一对一（限制较多）" else "一对多（限制较少）"
    val e = if(data.e == "0") "no" else "yes"
    val param = "类间因子Kruskal-Wallis秩检验的Alpha值：" + data.a + "/子类间配对Wilcoxon秩检验的Alpha值：" +
      data.w + "/差异特征LDA对数值的阈值：" + data.l + "/多组分类分析的分析策略：" + y +
      "/是否仅在子类名相同的情况下，进行子类间的成对比较？" + e

    val start=dutyController.insertDuty(data.taskname,id,"LF2","lefse分析2.0",input,param,elements)

    Future{
      val command0 = if(file1.filename.contains("biom")) {
        file1.ref.copyTo(otuFile)
        ""
      } else "biom convert -i " + tableFile.getAbsolutePath + " -o " + otuFile.getAbsolutePath + " --table-type=\"OTU table\" --to-json --process-obs-metadata taxonomy && \n"

      val runlefse = Utils.path + "R/lefse2.0/lefse_to_export/run_lefse.py " +
        dutyDir + "/out/lefse_format.txt " + dutyDir + "/out -a " + data.a + " -w " +
        data.w + " -l " + data.l + " -y " + data.y + " -e " + data.e + " && \n "

      val sum_level = (1 to data.level.toInt).mkString(",")

      val commandpack1 = "export PATH=\"/usr/bin/:$PATH\" && \n" + command0 +
        "dos2unix " + groupFile.getAbsolutePath + " && \n " +
        "chmod -R 777 " + otuFile.getAbsolutePath + " && \n " +
        "summarize_taxa.py -i " + otuFile.getAbsolutePath + " -o " + dutyDir + "/out/tax_summary_a -L " + sum_level + " -a" + " && \n " +
        Utils.path + "R/lefse2.0/lefse_to_export/plot-lefse.pl -i " + dutyDir + "/out/tax_summary_a/ -o " + dutyDir + "/out -m " + groupFile.getAbsolutePath + " -g Group -l " + data.level + " && \n " +
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

      if (execCommand.isSuccess) {
        val finish=dutyController.updateFini(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\nFinish Time:"+finish+"\n\n运行成功！")
        rservice.creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readLefse2(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname
    val elements=rservice.jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)
    val res=rservice.jsonToMap(elements("res"))
    val cla=rservice.jsonToMap(elements("cla"))
    val fea=rservice.jsonToMap(elements("fea"))
    val lefse_LDA= if(new File(path+"/out/lefse_LDA.png").length()==0) "false" else "true"
    //获取图片
    val pics= ((path+"/out/lefse_LDA.pdf",path+"/out/lefse_LDA.cladogram.pdf",path+"/out/lefse_LDA.features.pdf"))

    val fname = FileUtils.readLines(new File(path + "/out/lefse_LDA_diff.xls")).asScala.map{_.replaceAll("\"","").split("\t")(0)}.sorted

    Ok(Json.obj("pdfUrl"->pics,"lefse_LDA"->lefse_LDA,"res"->res,"cla"->cla,"fea"->fea,"fname"->fname))
  }

  case class ReLefResData(resmfl:String, resffs:String, rescfs:String, resdpi:String,
                          restitle:String, restfs:String, reswidth:String, resheight:String,
                          resorientation:String, resnscl:String)

  val ReLefResForm: Form[ReLefResData] =Form(
    mapping (
      "resmfl"->text,
      "resffs"->text,
      "rescfs"->text,
      "resdpi"->text,
      "restitle"->text,
      "restfs"->text,
      "reswidth"->text,
      "resheight"->text,
      "resorientation"->text,
      "resnscl"->text
    )(ReLefResData.apply)(ReLefResData.unapply)
  )

  def redrawLefRes(taskname:String)=Action(parse.multipartFormData) { implicit request =>
    val data=ReLefResForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname

    val oldele = rservice.jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)

    val elements=Json.obj("res"->Json.obj("resmfl"->data.resmfl,
      "resffs"->data.resffs, "rescfs"->data.rescfs, "resdpi"->data.resdpi,
      "restitle"->data.restitle, "restfs"->data.restfs, "reswidth"->data.reswidth,
      "resheight"->data.resheight, "resorientation"->data.resorientation,
      "resnscl"->data.resnscl).toString(),
      "cla"->oldele("cla"),"fea"->oldele("fea")).toString()

    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    val title =
      if(!data.restitle.equals("")) " --title \"" + data.restitle + "\""
      else ""
    val lsrs = if(data.resorientation == "v") " --left_space 0.02 --right_space 0.98" else ""

    val command = "export PATH=\"/usr/bin/:$PATH\" && \n" +
      Utils.path + "R/lefse2.0/lefse_to_export/plot_res.py " + dutyDir +
      "/out/lefse_LDA.xls " + dutyDir + "/out/lefse_LDA.pdf --format pdf --feature_font_size " +
      data.resffs + " --dpi " + data.resdpi + title + " --title_font_size " + data.restfs +
      " --width " + data.reswidth + " --height " + data.resheight + " --orientation " +
      data.resorientation + lsrs + " --subclades " + data.resnscl + " --class_legend_font_size " +
      data.rescfs + " --max_feature_len " + data.resmfl

    println(command)

    //先放入sh，在运行
    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/runres.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/runres.sh",dutyDir+"/temp")

    if (execCommand.isSuccess) {
      rservice.creatZip(dutyDir) //替换压缩文件包
      Ok(Json.obj("valid"->"true"))
    } else {
      Ok(Json.obj("valid"->"false","message"->execCommand.getErrStr))
    }
  }

  case class ReLefClaData(clasc:String, claevl:String, claml:String, claclv:String, clacs:String, clarsl:String,
                          clalstartl:String, clalstopl:String, claastartl:String, claastopl:String,
                          clamaxps:String, claminps:String, claalpha:String, clapew:String,
                          clascw:String, clapcw:String, clarsp:String, clatitle:String,
                          clatfs:String, clalfs:String, claclfs:String, cladpi:String)

  val ReLefClaForm: Form[ReLefClaData] =Form(
    mapping (
      "clasc"->text,
      "claevl"->text,
      "claml"->text,
      "claclv"->text,
      "clacs"->text,
      "clarsl"->text,
      "clalstartl"->text,
      "clalstopl"->text,
      "claastartl"->text,
      "claastopl"->text,
      "clamaxps"->text,
      "claminps"->text,
      "claalpha"->text,
      "clapew"->text,
      "clascw"->text,
      "clapcw"->text,
      "clarsp"->text,
      "clatitle"->text,
      "clatfs"->text,
      "clalfs"->text,
      "claclfs"->text,
      "cladpi"->text
    )(ReLefClaData.apply)(ReLefClaData.unapply)
  )

  def redrawLefCla(taskname:String)=Action(parse.multipartFormData) { implicit request =>
    val data=ReLefClaForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname

    val oldele = rservice.jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)

    val elements=Json.obj("res"->oldele("res"),
      "cla"->Json.obj("clasc"->data.clasc, "claevl"->data.claevl, "claml"->data.claml,
        "claclv"->data.claclv, "clacs"->data.clacs, "clarsl"->data.clarsl, "clalstartl"->data.clalstartl,
        "clalstopl"->data.clalstopl, "claastartl"->data.claastartl, "claastopl"->data.claastopl,
        "clamaxps"->data.clamaxps, "claminps"->data.claminps, "claalpha"->data.claalpha,
        "clapew"->data.clapew, "clascw"->data.clascw, "clapcw"->data.clapcw, "clarsp"->data.clarsp,
        "clatitle"->data.clatitle, "clatfs"->data.clatfs, "clalfs"->data.clalfs,
        "claclfs"->data.claclfs, "cladpi"->data.cladpi).toString(),
      "fea"->oldele("fea")).toString()

    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    val clasc = if(data.clasc == "") "" else " --sub_clade " + data.clasc
    val title = if(data.clatitle == "") " --title \" \"" else " --title \"" + data.clatitle + "\""

    val command = "export PATH=\"/usr/bin/:$PATH\" && \n" +
      Utils.path + "R/lefse2.0/lefse_to_export/plot_cladogram.py " + dutyDir +
      "/out/lefse_LDA.xls " + dutyDir + "/out/lefse_LDA.cladogram.pdf --format pdf" + clasc +
      " --expand_void_lev " + data.claevl + " --max_lev " + data.claml + " --clade_sep " +
      data.clacs + " --radial_start_lev " + data.clarsl + " --labeled_start_lev " + data.clalstartl +
      " --labeled_stop_lev " + data.clalstopl + " --abrv_start_lev " + data.claastartl +
      " --abrv_stop_lev " + data.claastopl + " --max_point_size " + data.clamaxps +
      " --min_point_size " + data.claminps + " --alpha " + data.claalpha + " --point_edge_width " +
      data.clapew + " --siblings_connector_width " + data.clascw + " --parents_connector_width " +
      data.clapcw + " --right_space_prop " + data.clarsp + title + " --title_font_size " +
      data.clatfs + " --label_font_size " + data.clalfs + " --class_legend_font_size " +
      data.claclfs + " --dpi " + data.cladpi + " --class_legend_vis " + data.claclv

    println(command)

    //先放入sh，在运行
    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/runcla.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/runcla.sh",dutyDir+"/temp")

    if (execCommand.isSuccess) {
      rservice.creatZip(dutyDir) //替换压缩文件包
      Ok(Json.obj("valid"->"true"))
    } else {
      Ok(Json.obj("valid"->"false","message"->execCommand.getErrStr))
    }
  }


  case class ReLefFeaData(feaf:String, feafname:String, feawidth:String, feaheight:String,
                          featop:String, feabot:String, featfs:String, feacfs:String,
                          feasmean:String, feasmedian:String, feafs:String, feadpi:String,
                          feaca:String)

  val ReLefFeaForm: Form[ReLefFeaData] =Form(
    mapping (
      "feaf"->text,
      "feafname"->text,
      "feawidth"->text,
      "feaheight"->text,
      "featop"->text,
      "feabot"->text,
      "featfs"->text,
      "feacfs"->text,
      "feasmean"->text,
      "feasmedian"->text,
      "feafs"->text,
      "feadpi"->text,
      "feaca"->text
    )(ReLefFeaData.apply)(ReLefFeaData.unapply)
  )

  def redrawLefFea(taskname:String)=Action(parse.multipartFormData) { implicit request =>
    val data=ReLefFeaForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname

    val oldele = rservice.jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)

    val elements=Json.obj("res"->oldele("res"), "cla"->oldele("cla"),
      "fea"->Json.obj("feaf"->data.feaf, "feafname"->data.feafname, "feawidth"->data.feawidth,
        "feaheight"->data.feaheight, "featop"->data.featop, "feabot"->data.feabot, "featfs"->data.featfs,
        "feacfs"->data.feacfs, "feasmean"->data.feasmean, "feasmedian"->data.feasmedian,
        "feafs"->data.feafs, "feadpi"->data.feadpi, "feaca"->data.feaca).toString()).toString()

    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    val fname = if(data.feaf == "one") " --feature_name " + data.feafname else ""

    val command = "export PATH=\"/usr/bin/:$PATH\" && \n" +
      Utils.path + "R/lefse2.0/lefse_to_export/plot_features.py " + dutyDir +
      "/out/lefse_format.txt " + dutyDir + "/out/lefse_LDA.xls " + dutyDir +
      "/out/lefse_LDA.features.pdf --format pdf -f " + data.feaf + fname + " --width " +
      data.feawidth + " --height " + data.feaheight + " --top " + data.featop + " --bot " +
      data.feabot + " --title_font_size " + data.featfs + " --class_font_size " + data.feacfs +
      " --subcl_mean " + data.feasmean + " --subcl_median " + data.feasmedian + " --font_size " +
      data.feafs + " --dpi " + data.feadpi + " --class_angle " + data.feaca

    println(command)

    //先放入sh，在运行
    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/runfea.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/runfea.sh",dutyDir+"/temp")

    if (execCommand.isSuccess) {
      rservice.creatZip(dutyDir) //替换压缩文件包
      Ok(Json.obj("valid"->"true"))
    } else {
      Ok(Json.obj("valid"->"false","message"->execCommand.getErrStr))
    }
  }



  //Tax4Fun
  case class TaxFunData2(taskname:String, r:String, fct:String, ref:String, srm:String)

  val TaxFunForm2: Form[TaxFunData2] =Form(
    mapping (
      "taskname"->text,
      "r"->text,
      "fct"->text,
      "ref"->text,
      "srm"->text,
    )(TaxFunData2.apply)(TaxFunData2.unapply)
  )

  def doTax4Fun=Action(parse.multipartFormData) { implicit request =>
    val data = TaxFunForm2.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=rservice.creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val tableFile=new File(dutyDir,"table.txt")
    val otuFile=new File(dutyDir,"otu_taxa_table.format.txt")

    val file1 = request.body.file("table1").get
    file1.ref.copyTo(tableFile)

    val param= "/选择SILVA数据库：" + data.r + "/是否使用预计算的KEGG正相关参考谱计算：" + data.fct + "/预计算方法：" + data.ref + "/是否基于100 bp的读数计算：" + data.srm

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"TAX","Tax4Fun功能预测",file1.filename,param,"")
    //矩阵文件读取写入任务文件下table.txt

    Future{
      val command =if(file1.filename.contains("biom")) {
        "perl " + Utils.path + "R/tax4fun/perl/tax4fun.step1.pl " + tableFile.getAbsolutePath + " " + dutyDir +" && \n" +
          "Rscript " + Utils.path + "R/tax4fun/Tax4Fun.R -rp " + Utils.path + "R/tax4fun/db -i " + otuFile.getAbsolutePath + " -o " + dutyDir + "/out -r " + data.r + " -fct " + data.fct + " -ref " + data.ref + " -srm " + data.srm + " && \n" +
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
          "Rscript " + Utils.path + "R/tax4fun/Tax4Fun.R -rp " + Utils.path + "R/tax4fun/db -i " + otuFile.getAbsolutePath + " -o " + dutyDir + "/out -r " + data.r + " -fct " + data.fct + " -ref " + data.ref + " -srm " + data.srm + " && \n" +
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
        val finish=dutyController.updateFini(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\nFinish Time:"+finish+"\n\n运行成功！")
        rservice.creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readTax4Fun(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    //获取图片
    val pics= (path+"/out/pca.png",path+"/out/kegg_L1.png",path+"/out/kegg_L2.png",path+"/out/kegg_L3.png")
    Ok(Json.obj("pics"->pics))
  }



  //FAPROTAX
  def doFAPROTAX=Action(parse.multipartFormData) { implicit request =>
    val data = TaxFunForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=rservice.creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val tableFile=new File(dutyDir,"table.txt")
    val otuFile=new File(dutyDir,"otu.biom")
    val file1 = request.body.file("table").get
    file1.ref.copyTo(tableFile)

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"FAP","FAPROTAX功能预测",file1.filename,"/","")
    //矩阵文件读取写入任务文件下table.txt

    Future{
      val command =if(file1.filename.contains("biom")) {
        file1.ref.copyTo(otuFile)
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
        val finish=dutyController.updateFini(id,data.taskname)
        val target=dutyDir+"/out/outTables.zip"
        new File(target).createNewFile()
        CompressUtil.zip(dutyDir+"/out/out",target)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\nFinish Time:"+finish+"\n\n运行成功！")
        rservice.creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }


  //Picrust
  case class PicrustData(taskname:String,in_traits:String,stratified:String)

  val PicrustForm: Form[PicrustData] =Form(
    mapping (
      "taskname"->text,
      "in_traits"->text,
      "stratified"->text
    )(PicrustData.apply)(PicrustData.unapply)
  )

  def doPICRUST=Action(parse.multipartFormData) { implicit request =>
    val data = PicrustForm.bindFromRequest.get
    val id=request.session.get("userId").get
    new File(userDutyDir+id+"/"+data.taskname).mkdir()
    new File(userDutyDir+id+"/"+data.taskname+"/temp").mkdir()
    val dutyDir=userDutyDir+id+"/"+data.taskname
    //在用户下创建任务文件夹和结果文件夹
    val tableFile=new File(dutyDir,"table.txt")
    val otuFile=new File(dutyDir,"otu.biom")
    val seqFile=new File(dutyDir,"seq.fasta")
    val file1 = request.body.file("table1").get
    val file2 = request.body.file("table2").get
    file1.ref.copyTo(tableFile)
    file2.ref.moveTo(seqFile)
    tableFile.setExecutable(true,false)
    tableFile.setReadable(true,false)
    tableFile.setWritable(true,false)
    otuFile.setExecutable(true,false)
    otuFile.setReadable(true,false)
    otuFile.setWritable(true,false)
    seqFile.setExecutable(true,false)
    seqFile.setReadable(true,false)
    seqFile.setWritable(true,false)

    val param="选择数据基因家族:"+data.in_traits+"/是否在各层级产生分层:"+data.stratified

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"PIC","PICRUST2功能预测",file1.filename+"/"+file2.filename,param,"")
    //矩阵文件读取写入任务文件下table.txt

    Future{
      val stratified=if(data.stratified=="yes") " --stratified " else ""

      val command0=if(file1.filename.contains("biom")) {
        file1.ref.copyTo(otuFile)
        ""
      } else
        "biom convert -i " + tableFile.getAbsolutePath + " -o " + otuFile.getAbsolutePath + " --table-type=\"OTU table\" --to-json && \n"

      val command1 = "picrust2_pipeline.py -i " + otuFile.getAbsolutePath +
        " -s " + seqFile.getAbsolutePath + " -o " + dutyDir + "/out" + " --processes 20 " + stratified +
        " --in_traits " + data.in_traits
      val command=command0 +
        "cd /root/miniconda3/bin && \n" +
        ". ./activate && \n"+
        "conda activate picrust2 && \n"+
        command1+ " && \n"+
        "conda deactivate"

      //先放入sh，在运行
      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

      if (execCommand.isSuccess) {
        new File(dutyDir+"/out/intermediate").delete()
        new File(Utils.path+"/users/"+id+"/"+data.taskname+"/out").listFiles().filter(_.isDirectory).foreach{x=>
          val dname=x.getName
          rservice.creatZip(dutyDir+"/out/"+dname+".zip",x.getAbsolutePath)
        }
        rservice.creatZip(dutyDir)
        val finish=dutyController.updateFini(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\nFinish Time:"+finish+"\n\n运行成功！")
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }



  //Getorf
  case class GetorfData(taskname:String,osformat:String,table:String,minsize:String,maxsize:String,
                        find:String,methionine:String,circular:String,reverse:String,flanking:String)

  val GetorfForm: Form[GetorfData] =Form(
    mapping (
      "taskname"->text,
      "osformat"->text,
      "table"->text,
      "minsize"->text,
      "maxsize"->text,
      "find"->text,
      "methionine"->text,
      "circular"->text,
      "reverse"->text,
      "flanking"->text
    )(GetorfData.apply)(GetorfData.unapply)
  )

  def doGetorf=Action(parse.multipartFormData) { implicit request =>
    val data = GetorfForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=rservice.creatUserDir(id,data.taskname)

    val seqFile=new File(dutyDir,"seq.txt")
    val file = request.body.file("table1").get
    val input=file.filename
    file.ref.moveTo(seqFile)
    seqFile.setExecutable(true,false)
    seqFile.setReadable(true,false)
    seqFile.setWritable(true,false)

    val table=data.table.split(":")
    val find=data.find.split(":")

    val param="输出序列格式：" + data.osformat + "/选择使用参考：" + table(1) +
      "/ORF显示的最小核苷酸大小：" + data.minsize + "/ORF显示的最大核苷酸大小：" + data.maxsize +
      "/输出类型：" + find(1) + "/是否将初始START密码子更改为蛋氨酸：" + data.methionine +
      "/序列是否为循环的：" + data.circular + "/是否以相反的顺序找到ORF：" + data.reverse +
      "/报告侧翼核苷酸的数量：" + data.flanking

    val start=dutyController.insertDuty(data.taskname,id,"GTF","Emboss Getorf",input,param,"")

    Future{
      val command = Utils.path+"R/EMBOSS/EMBOSS-6.6.0/emboss/getorf -sequence "+ seqFile.getAbsolutePath +
        " -outseq " + dutyDir + "/out/sequence." + data.osformat + " -osformat2 " +  data.osformat +
        " -table " + table(0) + " -minsize " + data.minsize + " -maxsize " + data.maxsize + " -find " +
        find(0) + " -methionine " + data.methionine + " -circular " + data.circular + " -reverse " +
        data.reverse + " -flanking " + data.flanking

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exec(command)

      if (execCommand.isSuccess) {
        val finish=dutyController.updateFini(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\nFinish Time:"+finish+"\n\n运行成功！")
        rservice.creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }




  //Eprimer
  case class EprimerData(taskname:String,primer:String,task:String,hybridprobe:String,numreturn:String,
                         includedregion:String,targetregion:String,excludedregion:String,
                         forwardinput:String,reverseinput:String,gcclamp:String,optsize:String,
                         minsize:String,maxsize:String,opttm:String,mintm:String,maxtm:String,
                         maxdifftm:String,ogcpercent:String,mingc:String,maxgc:String,saltconc:String)

  val EprimerForm: Form[EprimerData] =Form(
    mapping (
      "taskname"->text,
      "primer"->text,
      "task"->text,
      "hybridprobe"->text,
      "numreturn"->text,
      "includedregion"->text,
      "targetregion"->text,
      "excludedregion"->text,
      "forwardinput"->text,
      "reverseinput"->text,
      "gcclamp"->text,
      "optsize"->text,
      "minsize"->text,
      "maxsize"->text,
      "opttm"->text,
      "mintm"->text,
      "maxtm"->text,
      "maxdifftm"->text,
      "ogcpercent"->text,
      "mingc"->text,
      "maxgc"->text,
      "saltconc"->text
    )(EprimerData.apply)(EprimerData.unapply)
  )

  case class EprimerData2(dnaconc:String,maxpolyx:String,psizeopt:String,prange:String,ptmopt:String,
                          ptmmin:String,ptmmax:String)

  val EprimerForm2: Form[EprimerData2] =Form(
    mapping (
      "dnaconc"->text,
      "maxpolyx"->text,
      "psizeopt"->text,
      "prange"->text,
      "ptmopt"->text,
      "ptmmin"->text,
      "ptmmax"->text
    )(EprimerData2.apply)(EprimerData2.unapply)
  )

  def doEprimer=Action(parse.multipartFormData) { implicit request =>
    val data = EprimerForm.bindFromRequest.get
    val data2 = EprimerForm2.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=rservice.creatUserDir(id,data.taskname)

    val seqFile=new File(dutyDir,"seq.txt")
    val mishybFile=new File(dutyDir,"mishyb.txt")
    val mispriFile=new File(dutyDir,"mispri.txt")

    val file = request.body.file("table1").get
    file.ref.moveTo(seqFile)
    val (input,mishyblibraryfile,mispriminglibraryfile)=
      if(request.body.file("table2").isEmpty && request.body.file("table3").isEmpty)
        (file.filename,"","")
      else if(request.body.file("table2").isEmpty) {
        request.body.file("table3").get.ref.moveTo(mispriFile)
        (file.filename+"/"+request.body.file("table3").get.filename,""," -mispriminglibraryfile "+mispriFile.getAbsolutePath)
      } else if(request.body.file("table3").isEmpty) {
        request.body.file("table2").get.ref.moveTo(mishybFile)
        (file.filename+"/"+request.body.file("table2").get.filename," -mishyblibraryfile "+mishybFile.getAbsolutePath,"")
      } else {
        request.body.file("table3").get.ref.moveTo(mispriFile)
        request.body.file("table2").get.ref.moveTo(mishybFile)
        (file.filename+"/"+request.body.file("table2").get.filename+"/"+request.body.file("table3").get.filename," -mishyblibraryfile "+mishybFile.getAbsolutePath," -mispriminglibraryfile "+mispriFile.getAbsolutePath)
      }

    seqFile.setExecutable(true,false)
    seqFile.setReadable(true,false)
    seqFile.setWritable(true,false)
    mishybFile.setExecutable(true,false)
    mishybFile.setReadable(true,false)
    mishybFile.setWritable(true,false)
    mispriFile.setExecutable(true,false)
    mispriFile.setReadable(true,false)
    mispriFile.setWritable(true,false)

    val task=data.task.split(":")
    val includedregion=if(data.includedregion=="") "" else "/Included region(s)：" + data.includedregion
    val targetregion=if(data.targetregion=="") "" else "/Target region(s)：" + data.targetregion
    val excludedregion=if(data.excludedregion=="") "" else "/Excluded region(s)：" + data.excludedregion
    val forwardinput=if(data.forwardinput=="") "" else "/Forward input primer sequence to check：" + data.forwardinput
    val reverseinput=if(data.reverseinput=="") "" else "/Reverse input primer sequence to check：" + data.reverseinput

    val param="Pick PCR primer(s)：" + data.primer + "/Select task：" + task(1) +
      "/Pick hybridization probe：" + data.hybridprobe + "/Number of results to return：" +
      data.numreturn + includedregion + targetregion + excludedregion + forwardinput +
      reverseinput + "/GC clamp：" + data.gcclamp + "/Primer optimum size：" + data.optsize +
      "/Primer minimum size：" + data.minsize + "/Primer maximum size：" + data.maxsize +
      "/Primer optimum Tm：" + data.opttm + "/Primer minimum Tm：" + data.mintm +
      "/Primer maximum Tm：" + data.maxtm + "/Maximum difference in Tm of primers：" + data.maxdifftm +
      "/Primer optimum GC percent：" + data.ogcpercent + "/Primer minimum GC percent：" + data.mingc +
      "/Primer maximum GC percent：" + data.maxgc + "/Salt concentration (mM)：" + data.saltconc +
      "/DNA concentration (nM)：" + data2.dnaconc + "/Maximum polynucleotide repeat：" + data2.maxpolyx +
      "/Product optimum size：" + data2.psizeopt + "/Product size range：" + data2.prange +
      "/Product optimum Tm：" + data2.ptmopt + "/Product minimum Tm：" + data2.ptmmin

    val start=dutyController.insertDuty(data.taskname,id,"PMR","Emboss Eprimer3",input,param,"")

    Future{
      val includedregion_command=
        if(data.includedregion=="") "" else " -includedregion \"" + data.includedregion + "\""
      val targetregion_command=
        if(data.targetregion=="") "" else " -targetregion \"" + data.targetregion + "\""
      val excludedregion_command=
        if(data.excludedregion=="") "" else " -excludedregion \"" + data.excludedregion + "\""
      val forwardinput_command=
        if(data.forwardinput=="") "" else " -forwardinput \"" + data.forwardinput + "\""
      val reverseinput_command=
        if(data.reverseinput=="") "" else " -reverseinput \"" + data.reverseinput + "\""

      val command = Utils.path+"R/EMBOSS/EMBOSS-6.6.0/emboss/eprimer32 -sequence " + seqFile.getAbsolutePath +
        " -outfile " + dutyDir + "/out/sequence.eprimer32" + mishyblibraryfile + mispriminglibraryfile +
        " -primer \"" + data.primer + "\" -task \"" + task(0) + "\" -hybridprobe \"" + data.hybridprobe +
        "\" -numreturn \"" + data.numreturn + "\"" + includedregion_command + targetregion_command +
        excludedregion_command + forwardinput_command + reverseinput_command +
        " -gcclamp \"" + data.gcclamp + "\" -optsize \"" + data.optsize + "\" -minsize \"" +
        data.minsize + "\" -maxsize \"" + data.maxsize + "\" -opttm \"" + data.opttm + "\" -mintm \"" + data.mintm +
        "\" -maxtm \"" + data.maxtm + "\" -maxdifftm \"" + data.maxdifftm + "\" -ogcpercent \"" + data.ogcpercent +
        "\" -mingc \"" + data.mingc + "\" -maxgc \"" + data.maxgc + "\" -saltconc \"" + data.saltconc + "\" -dnaconc \"" +
        data2.dnaconc + "\" -maxpolyx \"" + data2.maxpolyx + "\" -psizeopt \"" + data2.psizeopt + "\" -prange \"" +
        data2.prange + "\" -ptmopt \"" + data2.ptmopt + "\" -ptmmin \"" + data2.ptmmin + "\" -ptmmax \"" +
        data2.ptmmax + "\""

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

      if (execCommand.isSuccess) {
        val finish=dutyController.updateFini(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\nFinish Time:"+finish+"\n\n运行成功！")
        rservice.creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }


}
