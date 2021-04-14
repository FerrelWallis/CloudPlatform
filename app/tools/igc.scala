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

object igc extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String], abbre:String)(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val tableFile1=new File(dutyDir,"table1.txt")
    val tableFile2=new File(dutyDir,"table2.txt")
    val file1 = request.body.file("table1").get
    val file2 = request.body.file("table2")
    val (input,tablepath) =
      if(abbre == "IGC") {
        file1.ref.getPath.moveToFile(tableFile1)
        (file1.filename," -i1 "+tableFile1.getAbsolutePath)
      } else {
        file1.ref.getPath.moveToFile(tableFile1)
        file2.get.ref.getPath.moveToFile(tableFile2)
        (file1.filename+"/" + file2.get.filename," -i1 "+tableFile1.getAbsolutePath+" -i2 "+tableFile2.getAbsolutePath)
      }
    val sname = if(abbre != "IGC") "组间相关性分析" else "组内相关性分析"
    val param= "分析类型：" + params("anatype")

    val elements=Json.obj("heat"->Json.obj("smt"->"full","cluster_rows"->"FALSE","crm"->"complete",
      "rp"->"1","cluster_cols"->"FALSE","ccm"->"complete","cp"->"1","sc"->"none","lg"->"none",
      "color"->"#E41A1C:#FFFF00:#1E90FF","cc"->"30","nc"->"#DDDDDD","hasborder"->"white",
      "cbc"->"#ffffff","hasnum"->"FALSE","hasrname"->"TRUE","hascname"->"TRUE",
      "rtree"->"50","ctree"->"50","xfs"->"10","yfs"->"10","xfa"->"90","fn"->"8","lfi"->"TRUE").toString(),
      "net"->Json.obj("gshape"->"ellipse","netcolor1"->"#555555","gopa"->"1","gsize"->"5",
      "gfont"->"20","netcolor2"->"#ffffff","eshape"->"diamond","netcolor3"->"#5da5fb","eopa"->"1",
      "esize"->"10","efont"->"20","netcolor4"->"#ffffff","netcolor5"->"#737373","opacity"->"0.6",
      "dot"->"3","pthres"->"0.1","cthres"->"0.5","rows"->"0").toString()).toString()

    try {
      val command1 = "Rscript "+Utils.path+"R/igc/cor_pvalue_calculate.R" + tablepath +
        " -o " + dutyDir + "/out" + " -m " + params("anatype")
      val command2 = "Rscript "+Utils.path+"R/net/node_attr_calculate.R -t "+ dutyDir + "/out/pandv.xls" +
        " -pt 0.1 -ct 0.5 -o " + dutyDir + "/out"
      val command3 = "Rscript "+Utils.path+"R/heatmap/heatMap.R -i "+ dutyDir+"/out/cor.xls" +
        " -o " +dutyDir+"/out -c #E41A1C:#FFFF00:#1E90FF -lfi " + dutyDir+"/out/p_star.xls" + " -if pdf"
      val commandpack1=Array(command1,command2,command3)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command1+" && \n"+command2+" && \n"+command3)
      val execCommand = new ExecCommand
      execCommand.exec(commandpack1)

      if (execCommand.isSuccess) {
        Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.png")
        Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.tiff")
      } else {
        state = 2
        msg = execCommand.getErrStr
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, sname, input, param, elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val elements1=elements("heat").jsonToMap
    val elements2=elements("net").jsonToMap
    val head=FileUtils.readFileToString(new File(dutyDir+"/out/cor.xls")).trim.split("\n")
    val rnum=head.length-1
    val cnum=head(1).trim.split("\t").length-1
    val tableFile2=new File(dutyDir,"table2.txt")
    var result=FileUtils.readLines(new File(dutyDir+"/out/pandv.xls")).asScala.drop(1)
    val len = if(result.length <= 2000) 0
    else if(result.length / 2000 > (result.length / 2000).toInt)
      (result.length / 2000).toInt
    else (result.length / 2000).toInt - 1

    val startrow = elements2("rows").toInt
    if(startrow != 0) result = result.drop(startrow * 2000)
    if(result.length >= 2000) result = result.take(2000)

    var eid=0;
    var soutar:List[List[String]]=List(List(""))
    var resultFilter = Array("")
    result.foreach{x=>
      val ei = x.split("\t").filter(_.trim!="")
      val source=ei(1)
      val target=ei(2)
      if(ei(3) != "NA" && ei(4) != "NA") {
        val w=ei(4).toDouble
        val c=ei(3).toDouble
        if((!soutar.contains(List(source,target)) || !soutar.contains(List(target,source))) && source!=target && w<elements2("pthres").toDouble && Math.abs(c)<elements2("cthres").toDouble) {
          soutar=soutar:+List(source,target):+List(target,source)
          resultFilter=resultFilter:+x
        }
      }
    }

    val (e,g) = resultFilter.drop(1).map{row =>
      (row.split("\t")(1),row.split("\t")(2))
    }.unzip
    val list=e++g

    var count=0;
    val nodes=list.map{x=>
      count=count+1
      val id=list.indexOf(x).toString
      val xy=Json.obj("x"->Math.random()*500,"y"->Math.random()*500)
      val (group,score)=
        if(tableFile2.exists()){
          if(count<=e.drop(1).length) ("evi",elements2("esize").toDouble) //环境node
          else ("gene",elements2("gsize").toDouble) //基因node
        }else ("gene",elements2("gsize").toDouble)
      val data=Json.obj("id"->id,"name"->x,"score"->score,"group"->group)
      Json.obj("data"->data,"position"->xy,"group"->"nodes")
    }

    val edges=resultFilter.drop(1).map{x=>
      val ei = x.split("\t").filter(_.trim!="")
      val source=list.indexOf(ei(1))
      val target=list.indexOf(ei(2))
      eid=eid+1
      val id="e"+eid
      val weight=ei(4).toDouble
      val cc=ei(3).toDouble
      val lab="c="+cc.formatted("%."+elements2("dot")+"f")+"；p="+weight.formatted("%."+elements2("dot")+"f")
      val data=Json.obj("source"->source,"target"->target,"weight"->weight,"label"->lab)
      Json.obj("data"->data,"group"->"edges","id"->id)
    }

    val rows=nodes++edges

    val node=Json.obj("selector"->"node", "style"->Json.obj("width"->"mapData(score, 0, 10, 10, 100)", "height"->"mapData(score, 0, 10, 10, 100)", "content"-> "data(name)", "font-size"-> "12px", "text-valign"-> "center", "text-halign"-> "center", "text-outline-width"-> "2px"))

    val font1=elements2("gfont")+"px"
    val nodegene=Json.obj("selector"-> "node[group='gene']","style"->Json.obj( "shape"-> elements2("gshape"), "background-color"-> elements2("netcolor1"), "text-outline-color"-> elements2("netcolor1"), "opacity"-> elements2("gopa"), "font-size"->font1,"color"->elements2("netcolor2")))

    val font2=elements2("efont")+"px"
    val nodeevi=Json.obj("selector"-> "node[group='evi']","style"->Json.obj("shape"-> elements2("eshape"), "background-color"-> elements2("netcolor3"), "text-outline-color"-> elements2("netcolor3"), "opacity"-> elements2("eopa"), "font-size"->font2,"color"->elements2("netcolor4")))

    val nodesele=Json.obj("selector"-> "node:selected","style"->Json.obj("border-width"-> "6px", "border-color"-> "#AAD8FF", "border-opacity"-> "0.5", "background-color"-> "#993399", "text-outline-color"-> "#993399"))

    val edgehigh=Json.obj("selector"-> "edge.highlighted","style"->Json.obj("line-color"-> "#2a6cd6", "target-arrow-color"-> "#2a6cd6", "opacity"-> 0.7, "label"-> "data(label)", "edge-text-rotation"-> "autorotate"))

    val edge=Json.obj("selector"-> "edge","style"->Json.obj("curve-style"-> "haystack", "haystack-radius"-> "0.5", "opacity"-> elements2("opacity"), "line-color"-> elements2("netcolor5"), "width"-> "mapData(weight, 0, 1, 1, 8)", "overlay-padding"-> "3px"))

    val selector=Array(node,nodegene,nodeevi,nodesele,edge,edgehigh)

    Json.obj("len"->len,"rnum"->rnum,"cnum"->cnum,"elements1"->elements1,"elements2"->elements2,"allcol"->(head(1).trim.split("\t").length-1),"allrow"->(head.length-1),"rows"->rows,"selector"->selector,"table2"->tableFile2.exists())
  }

  def ReDrawHeat(dutyDir: String, newElements: Map[String, String], elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val tableFile=new File(dutyDir+"/out","cor.xls")
    val tagFile=new File(dutyDir+"/out","p_star.xls")
    val lfi=
      if(newElements("lfi")=="TRUE") " -lfi " + tagFile.getAbsolutePath
      else ""

    val rowclu=
      if(newElements("cluster_rows")=="TRUE") " -crw " + newElements("cluster_rows") + " -crm " + newElements("crm") + " -rp " + newElements("rp")
      else " -crw " + newElements("cluster_rows")
    val colclu=
      if(newElements("cluster_cols")=="TRUE") " -ccl " + newElements("cluster_cols") + " -ccm " + newElements("ccm") + " -cp " + newElements("cp")
      else " -ccl " + newElements("cluster_cols")

    val color= if(newElements("color")=="0") newElements("designcolor") else newElements("color")
    val cbc= if(newElements("hasborder")=="white") " -cbc " + newElements("cbc") else " -cbc " + newElements("hasborder")

    val elenet=elements("net")
    val elejson=Json.obj("heat"->Json.obj("smt"->newElements("smt"),"cluster_rows"->newElements("cluster_rows"),
      "crm"->newElements("crm"),"rp"->newElements("rp"),"cluster_cols"->newElements("cluster_cols"),"ccm"->newElements("ccm"),
      "cp"->newElements("cp"),"sc"->newElements("sc"),"lg"->newElements("lg"),"color"->color,"cc"->newElements("cc"),
      "nc"->newElements("nc"),"hasborder"->newElements("hasborder"),"cbc"->newElements("cbc"),"hasnum"->newElements("hasnum"),
      "hasrname"->newElements("hasrname"), "hascname"->newElements("hascname"),"rtree"->newElements("rtree"),
      "ctree"->newElements("ctree"),"xfs"->newElements("xfs"), "yfs"->newElements("yfs"),"xfa"->newElements("xfa"),
      "fn"->newElements("fn"),"lfi"->newElements("lfi")).toString(),"net"->elenet).toString()

    val head=FileUtils.readFileToString(tableFile).trim.split("\n")
    val rnum = (head.length-1).toString
    val cnum = (head(1).trim.split("\t").length-1).toString

    val command = "Rscript "+Utils.path+"R/heatmap/heatMap.R -i "+ tableFile.getAbsolutePath +
      " -o " +dutyDir+"/out"+ " -smt " + newElements("smt") + lfi + rowclu + colclu + " -sc " + newElements("sc") +
      " -lg " + newElements("lg") + " -c " + color + " -cc " + newElements("cc") + " -nc " + newElements("nc") + cbc +
      " -sn " + newElements("hasrname") + ":" + newElements("hascname") + ":" + newElements("hasnum") + " -th " +
      newElements("rtree") + ":" + newElements("ctree") + " -fs " + newElements("yfs") + ":" + newElements("xfs") +
      " -if pdf -cln TRUE -xfa " + newElements("xfa") + " -fn " + newElements("fn")
    println(command)

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(command,dutyDir+"/temp")

    var valid = ""
    var pics = ""
    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.tiff") //替换图片
      pics=dutyDir+"/out/heatmap.png"
      valid = "true"
    } else {
      valid = "false"
    }
    Json.obj("valid"->valid, "pics"->pics, "elements"->elejson,"cnum"->cnum,"rnum"->rnum)
  }

  def ReDrawNet(dutyDir: String, newElements: Map[String, String], elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val eleheat=elements("heat")
    val elejson=Json.obj("heat"->eleheat,"net"->Json.obj("gshape"->newElements("gshape"),"netcolor1"->newElements("netcolor1"),
      "gopa"->newElements("gopa"),"gsize"->newElements("gsize"),"gfont"->newElements("gfont"),"netcolor2"->newElements("netcolor2"),
      "eshape"->newElements("eshape"),"netcolor3"->newElements("netcolor3"),"eopa"->newElements("eopa"),"esize"->newElements("esize"),
      "efont"->newElements("efont"),"netcolor4"->newElements("netcolor4"),"netcolor5"->newElements("netcolor5"),
      "opacity"->newElements("opacity"),"dot"->newElements("dot"),"pthres"->newElements("pthres"),"cthres"->newElements("cthres"),
      "rows"->newElements("rows")).toString()).toString()

    val command = "Rscript "+Utils.path+"R/net/node_attr_calculate.R -t "+ dutyDir + "/out/pandv.xls" +
      " -pt " + newElements("pthres") + " -ct " + newElements("cthres") + " -o " + dutyDir + "/out"

    println(command)
    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(command, dutyDir+"/temp")

    var valid = ""
    if (execCommand.isSuccess) {
      valid = "true"
    } else {
      valid = "false"
    }
    Json.obj("valid"->valid, "elements"->elejson)
  }


}
