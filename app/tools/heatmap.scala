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

object heatmap extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val isgroupr = (params("isgrouprtext") == "TRUE")
    val isgroupc = (params("isgroupctext") == "TRUE")
    val istag = (params("istagtext") == "TRUE")

    val tableFile=new File(dutyDir,"table.txt")
    val treerFile=new File(dutyDir,"treer.txt")
    val treecFile=new File(dutyDir,"treec.txt")
    val grouprFile=new File(dutyDir,"groupr.txt")
    val groupcFile=new File(dutyDir,"groupc.txt")
    val tagFile=new File(dutyDir,"tag.txt")
    val filename2=
      if(params("cluster_rows")=="file") {
        val file=request.body.file("table2").get
        file.ref.getPath.moveToFile(treerFile)
        "/"+file.filename
      }else ""

    val filename3=
      if(params("cluster_cols")=="file") {
        val file=request.body.file("table3").get
        file.ref.getPath.moveToFile(treecFile)
        "/"+file.filename
      }else ""

    val filename4=
      if(isgroupr==true) {
        val file=request.body.file("table4").get
        file.ref.getPath.moveToFile(grouprFile)
        "/"+file.filename
      }else ""

    val filename5=
      if(isgroupc==true) {
        val file=request.body.file("table5").get
        file.ref.getPath.moveToFile(groupcFile)
        "/"+file.filename
      }else ""

    val filename6=
      if(istag==true) {
        val file=request.body.file("table6").get
        file.ref.getPath.moveToFile(tagFile)
        "/"+file.filename
      }else ""
    
    val file1=request.body.file("table1").get
    file1.ref.getPath.moveToFile(tableFile)
    val input= file1.filename+filename2+filename3+filename4+filename5+filename6
    val c=
      if(params("inr")=="" && params("inc")=="") ""
      else if(params("inr")!="" && params("inc")=="") "/作图的行："+params("inr")
      else if(params("inr")=="" && params("inc")!="") "/作图的列："+params("inc")
      else "/作图的行："+params("inr")+"/作图的列："+params("inc")
    val rm=if(params("cluster_rows")=="TRUE") "/行聚类方法："+params("crm") else ""
    val cm=if(params("cluster_cols")=="TRUE") "/列聚类方法："+params("ccm") else ""

    val param= "是否对行聚类：" + params("cluster_rows") + rm + "/是否对列聚类：" +
      params("cluster_cols") + cm + "/是否行分组：" + isgroupr + "/是否列分组：" +
      isgroupc + "/是否自定义格子标签：" + istag + c + "/是否取lg：" + params("lg") +
      "/归一化：" + params("sc") + "/颜色：" + params("color") + "/在格子上显示数字：" +
      params("hasnum") + "/是否显示行名：" + params("hasrname") + "/是否显示列名：" + params("hascname") +
      "/画出格子的边界：" + params("hasborder")

    val elements=Json.obj("smt"->"full","cluster_rows"->params("cluster_rows"),"crm"->params("crm"),
      "rp"->"1","cluster_cols"->params("cluster_cols"),"ccm"->params("ccm"),"cp"->"1","inr"->params("inr"),
      "inc"->params("inc"),"sc"->params("sc"),"lg"->params("lg"),"color"->params("color"),"cc"->"30","nc"->"#DDDDDD",
      "hasborder"->params("hasborder"),"cbc"->"#ffffff","hasnum"->params("hasnum"),"hasrname"->params("hasrname"),
      "hascname"->params("hascname"),"rtree"->"50","ctree"->"50","xfs"->"10","yfs"->"10","xfa"->"90",
      "fn"->"8").toString()
    
    try {
      val trf=if(treerFile.exists()) " -trf "+treerFile.getAbsolutePath else ""
      val tcf=if(treecFile.exists()) " -tcf "+treecFile.getAbsolutePath else ""
      val ari=if(grouprFile.exists()) " -ari "+ grouprFile.getAbsolutePath else ""
      val aci=if(groupcFile.exists()) " -aci "+ groupcFile.getAbsolutePath else ""
      val lfi=if(tagFile.exists()) " -lfi "+ tagFile.getAbsolutePath else ""

      val rowclu=
        if(params("cluster_rows")!="file") " -crw " + params("cluster_rows")+" -crm " + params("crm")
        else ""
      val colclu=
        if(params("cluster_cols")!="file") " -ccl " + params("cluster_cols")+" -ccm " + params("ccm")
        else ""

      val inr=if(params("inr")!="") " -inr "+params("inr") else ""
      val inc=if(params("inc")!="") " -inc "+params("inc") else ""

      val command = "Rscript "+Utils.path+"R/heatmap/heatMap.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out" + trf + tcf + ari + aci + lfi + rowclu + colclu + inr + inc +
        " -lg " + params("lg") + " -sc " + params("sc") + " -sn " +  params("hasrname") + ":" + params("hascname") +
        ":" + params("hasnum") + " -c " + params("color") + " -cbc " + params("hasborder") + " -if pdf -cln TRUE -fn 8"

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }else {
        Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.png")
        Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.tiff")
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "Heatmap 热图", input, param, elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val groupr=new File(dutyDir,"groupr.txt").exists()
    val groupc=new File(dutyDir,"groupc.txt").exists()
    
    val head=FileUtils.readFileToString(new File(dutyDir+"/table.txt")).trim.split("\n")
    val rnum=
      if(elements("inr")=="") head.length-1
      else {
        val r1=elements("inr").split(",").length-elements("inr").split(",").filter(_.contains("-")).length
        val r2=elements("inr").split(",").filter(_.contains("-")).map{x=>
          val temp=x.split("-")
          temp(1).toInt-temp(0).toInt+1
        }.sum
        if((r1+r2)>(head.length-1)) head.length-1 else r1+r2
      }
    val cnum=
      if(elements("inc")=="") head(1).trim.split("\t").length-1
      else {
        val c1=elements("inc").split(",").length-elements("inc").split(",").filter(_.contains("-")).length
        val c2=elements("inc").split(",").filter(_.contains("-")).map{x=>
          val temp=x.split("-")
          temp(1).toInt-temp(0).toInt+1
        }.sum
        if((c1+c2)>head(1).trim.split("\t").length-1) head(1).trim.split("\t").length-1 else c1+c2
      }

    val (groupcolor)=if(elements.contains("groupcolor")) elements("groupcolor")
    else if(new File(dutyDir+"/out/color_zhushi.xls").exists()) {
      FileUtils.readLines(new File(dutyDir+"/out/color_zhushi.xls")).asScala.tail.map{line=>
        val coldata=line.replaceAll("\"","").split("\t")
        coldata(1)+"-"+coldata(2)+":"+coldata(3)
      }.mkString(",")
    } else ""
    
    Json.obj("rnum"->rnum,"cnum"->cnum,"elements"->elements,"allcol"->(head(1).trim.split("\t").length-1),
      "allrow"->(head.length-1),"groupr"->groupr,"groupc"->groupc,"groupcolor"->groupcolor)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val tableFile=new File(dutyDir,"table.txt")
    val treerFile=new File(dutyDir,"treer.txt")
    val treecFile=new File(dutyDir,"treec.txt")
    val grouprFile=new File(dutyDir,"groupr.txt")
    val groupcFile=new File(dutyDir,"groupc.txt")
    val tagFile=new File(dutyDir,"tag.txt")

    var valid = ""
    var pics = ""
    var msg = ""
    var newelements = ""
    var rnum = "0"
    var cnum = "0"
    if(elements("cluster_rows")=="file" && !treerFile.exists()){
      valid = "false"
      msg = "没有上传行聚类信息文件，无法使用！"
    } else if(elements("cluster_cols")=="file" && !treecFile.exists()){
      valid = "false"
      msg = "没有上传列聚类信息文件，无法使用！"
    }else{
      val ari=if(grouprFile.exists()) " -ari "+ grouprFile.getAbsolutePath else ""
      val aci=if(groupcFile.exists()) " -aci "+ groupcFile.getAbsolutePath else ""
      val lfi=if(tagFile.exists()) " -lfi "+ tagFile.getAbsolutePath else ""

      val rowclu=
        if(elements("cluster_rows")=="TRUE") " -crw " + elements("cluster_rows") + " -crm " + elements("crm") + " -rp " + elements("rp")
        else if(elements("cluster_rows")=="FALSE") " -crw " + elements("cluster_rows")
        else " -trf " + treerFile.getAbsolutePath + " -rp " + elements("rp")
      val colclu=
        if(elements("cluster_cols")=="TRUE") " -ccl " + elements("cluster_cols") + " -ccm " + elements("ccm") + " -cp " + elements("cp")
        else if(elements("cluster_cols")=="FALSE") " -ccl " + elements("cluster_cols")
        else " -tcf " + treecFile.getAbsolutePath + " -cp " + elements("cp")

      val color= if(elements("color")=="0") elements("designcolor") else elements("color")
      val cbc= if(elements("hasborder")=="white") " -cbc " + elements("cbc") else " -cbc " + elements("hasborder")
      val inc= if(elements("inc")=="") "" else " -inc " + elements("inc")
      val inr= if(elements("inr")=="") "" else " -inr " + elements("inr")

      newelements=Json.obj("smt"->elements("smt"),"cluster_rows"->elements("cluster_rows"),"crm"->elements("crm"),
        "rp"->elements("rp"),"cluster_cols"->elements("cluster_cols"),"ccm"->elements("ccm"),"cp"->elements("cp"),"inr"->elements("inr"),
        "inc"->elements("inc"),"sc"->elements("sc"),"lg"->elements("lg"),"color"->color,"cc"->elements("cc"),"nc"->elements("nc"),
        "hasborder"->elements("hasborder"),"cbc"->elements("cbc"),"hasnum"->elements("hasnum"), "hasrname"->elements("hasrname"),
        "hascname"->elements("hascname"),"rtree"->elements("rtree"),"ctree"->elements("ctree"),"xfs"->elements("xfs"),"yfs"->elements("yfs"),
        "xfa"->elements("xfa"),"fn"->elements("fn"),"groupcolor"->elements("gdesigncolor")).toString()

      val head=FileUtils.readFileToString(tableFile).trim.split("\n")
      rnum =
        if(elements("inr")=="") (head.length-1).toString
        else {
          val r1=elements("inr").split(",").length-elements("inr").split(",").filter(_.contains("-")).length
          val r2=elements("inr").split(",").filter(_.contains("-")).map{x=>
            val temp=x.split("-")
            temp(1).toInt-temp(0).toInt+1
          }.sum
          if((r1+r2)>(head.length-1)) (head.length-1).toString else (r1+r2).toString
        }
      cnum =
        if(elements("inc")=="") (head(1).trim.split("\t").length-1).toString
        else {
          val c1=elements("inc").split(",").length-elements("inc").split(",").filter(_.contains("-")).length
          val c2=elements("inc").split(",").filter(_.contains("-")).map{x=>
            val temp=x.split("-")
            temp(1).toInt-temp(0).toInt+1
          }.sum
          if((c1+c2)>head(1).trim.split("\t").length-1) (head(1).trim.split("\t").length-1).toString else (c1+c2).toString
        }

      val groupdata=elements("gdesigncolor").split(",")
      val acrs=if(grouprFile.exists()) {
        val rowgroupname=FileUtils.readLines(grouprFile).asScala(0).split("\t").tail
        val rr=rowgroupname.map{x=>groupdata.filter(_.indexOf(x)>=0)}.flatten.map{x=>
          (x.split("-")(0),x.split(":")(1))
        }.groupBy(_._1)
        " -acrs " + rowgroupname.map{x=>x+"@"+rr(x).map(_._2).mkString(":")}.mkString(",")
      } else ""

      val accs=if(groupcFile.exists()){
        val colgroupname=FileUtils.readLines(groupcFile).asScala(0).split("\t").tail
        val cc=colgroupname.map{x=>groupdata.filter(_.indexOf(x)>=0)}.flatten.map{x=>
          (x.split("-")(0),x.split(":")(1))
        }.groupBy(_._1)
        " -accs " + colgroupname.map{x=>x+"@"+cc(x).map(_._2).mkString(":")}.mkString(",")
      } else ""

      val command = "Rscript "+Utils.path+"R/heatmap/heatMap.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out"+ " -smt " + elements("smt") + ari + aci + lfi + rowclu + colclu + inr + inc +
        " -sc " + elements("sc") + " -lg " + elements("lg") + " -c " + color + " -cc " + elements("cc") + " -nc " + elements("nc") +
        cbc + " -sn " + elements("hascname") + ":" + elements("hasrname") + ":" + elements("hasnum") + " -th " +
        elements("rtree") + ":" + elements("ctree") + " -fs " + elements("yfs") + ":" + elements("xfs") +
        " -if pdf -cln TRUE -xfa " + elements("xfa") + " -fn " + elements("fn") + acrs + accs

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      if (execCommand.isSuccess) {
        Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.png") //替换图片
        Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.tiff") //替换图片
        pics=dutyDir+"/out/heatmap.png"
        valid = "true"
      } else {
        valid = "false"
        msg = execCommand.getErrStr
      }
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->newelements, "cnum"->cnum, "rnum"->rnum, "message"->msg)
  }


}
