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

object net extends MyFile with MyStringTool with MyMapTool{
  def Run(dutyDir: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    var state = 1
    var msg = "Run Success!"

    val file1=request.body.file("table1").get
    val file2=request.body.file("table2").get
    val tableFile=new File(dutyDir,"table.txt")
    val eviFile=new File(dutyDir,"evi.txt")
    val input= file1.filename+"/"+file2.filename
    val param= "相关分析方法:"+params("anatype")

    val elements=Json.obj("gshape"->"ellipse","color1"->"#555555","gopa"->"1","gsize"->"5",
      "gfont"->"20","color2"->"#ffffff","eshape"->"diamond","color3"->"#5da5fb","eopa"->"1","esize"->"10",
      "efont"->"20","color4"->"#ffffff","color5"->"#737373","opacity"->"0.6","dot"->"3","pthres"->"0.1",
      "cthres"->"1").toString()

    file1.ref.getPath.moveToFile(tableFile)
    file2.ref.getPath.moveToFile(eviFile)

    try {
      val command1 = "Rscript "+Utils.path+"R/net/network_data.R -i2 "+ tableFile.getAbsolutePath + " -i1 " +
        eviFile.getAbsolutePath + " -o " +dutyDir+"/out/result.xls" + " -m1 " + params("anatype") + " -m2 " +
        params("anatype")
      val command2 = "Rscript "+Utils.path+"R/net/node_attr_calculate.R -t "+ dutyDir + "/out/result.xls" +
        " -pt 0.1 -ct 1 -o " + dutyDir + "/out"

      println(command1 + "\n" + command2)
      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command1 + " && \n" + command2)
      val execCommand = new ExecCommand
      execCommand.exect(Array(command1, command2), new File(dutyDir + "/temp"))

      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg, "权重网络图", input, param, elements)
  }

  def GetParams(dutyDir: String, elements: Map[String, String])(implicit request: Request[AnyContent]) = {
    val genus=FileUtils.readLines(new File(dutyDir+"/table.txt")).asScala
    val g=genus.map{line=>
      line.trim.split("\t").head
    }

    val evi=FileUtils.readLines(new File(dutyDir+"/evi.txt")).asScala
    val e=evi.map{line=>
      line.trim.split("\t").head
    }

    val list=e.drop(1)++g.drop(1)
    var count=0;
    val nodes=list.map{x=>
      count=count+1
      val id=list.indexOf(x).toString
      val xy=Json.obj("x"->Math.random()*500,"y"->Math.random()*500)
      val (group,score)=
        if(count<=e.drop(1).length) ("evi",elements("esize").toDouble) //环境node
        else ("gene",elements("gsize").toDouble) //基因node
      val data=Json.obj("id"->id,"name"->x,"score"->score,"group"->group)
      Json.obj("data"->data,"position"->xy,"group"->"nodes")
    }

    val result=FileUtils.readLines(new File(dutyDir+"/out/result.xls")).asScala
    var eid=0;

    var resultFilter = Array("")
    result.drop(1).foreach{x=>
      val ei = x.split("\t").filter(_.trim!="")
      if(ei(3) != "NA" && ei(4) != "NA") {
        val w=ei(4).toDouble
        val c=ei(3).toDouble
        if(w<elements("pthres").toDouble && Math.abs(c)<elements("cthres").toDouble) {
          resultFilter=resultFilter:+x
        }
      }
    }

    val edges=resultFilter.drop(1).map { x =>
      eid = eid + 1
      val id = "e" + eid
      val e = x.split("\t").filter(_.trim != "")
      val source = list.indexOf(e(1))
      val target = list.indexOf(e(2))
      val weight = e(4).toDouble
      val cc = e(3).toDouble
      val lab = "c=" + cc.formatted("%." + elements("dot") + "f") + "；p=" + weight.formatted("%." + elements("dot") + "f")
      val data = Json.obj("source" -> source, "target" -> target, "weight" -> weight, "label" -> lab)
      Json.obj("data" -> data, "group" -> "edges", "id" -> id)
    }

    val rows=nodes++edges

    val node=Json.obj("selector"->"node", "style"->Json.obj("width"->"mapData(score, 0, 10, 10, 100)", "height"->"mapData(score, 0, 10, 10, 100)", "content"-> "data(name)", "font-size"-> "12px", "text-valign"-> "center", "text-halign"-> "center", "text-outline-width"-> "2px"))

    val font1=elements("gfont")+"px"
    val nodegene=Json.obj("selector"-> "node[group='gene']","style"->Json.obj( "shape"-> elements("gshape"), "background-color"-> elements("color1"), "text-outline-color"-> elements("color1"), "opacity"-> elements("gopa"), "font-size"->font1,"color"->elements("color2")))

    val font2=elements("efont")+"px"
    val nodeevi=Json.obj("selector"-> "node[group='evi']","style"->Json.obj("shape"-> elements("eshape"), "background-color"-> elements("color3"), "text-outline-color"-> elements("color3"), "opacity"-> elements("eopa"), "font-size"->font2,"color"->elements("color4")))

    val nodesele=Json.obj("selector"-> "node:selected","style"->Json.obj("border-width"-> "6px", "border-color"-> "#AAD8FF", "border-opacity"-> "0.5", "background-color"-> "#993399", "text-outline-color"-> "#993399"))

    //    val edgehigh=Json.obj("selector"-> "edge.highlighted","style"->Json.obj("line-color"-> "#2a6cd6", "target-arrow-color"-> "#2a6cd6", "opacity"-> 0.7))

    val edgehigh=Json.obj("selector"-> "edge.highlighted","style"->Json.obj("line-color"-> "#2a6cd6", "target-arrow-color"-> "#2a6cd6", "opacity"-> 0.7, "label"-> "data(label)", "edge-text-rotation"-> "autorotate"))


    //    val edgesele=Json.obj("selector"-> "edge[label]:selected","style"->Json.obj("line-color"-> "#2a6cd6", "target-arrow-color"-> "#2a6cd6", "opacity"-> 0.7, "label"-> "data(label)", "edge-text-rotation"-> "autorotate"))

    val edge=Json.obj("selector"-> "edge","style"->Json.obj("curve-style"-> "haystack", "haystack-radius"-> "0.5", "opacity"-> elements("opacity"), "line-color"-> elements("color5"), "width"-> "mapData(weight, 0, 1, 1, 8)", "overlay-padding"-> "3px"))

    val selector=Array(node,nodegene,nodeevi,nodesele,edge,edgehigh)

    Json.obj("rows"->rows,"elements"->elements,"selector"->selector)
  }

  def ReDraw(dutyDir: String, elements: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]) = {
    val newelements=Json.obj("gshape"->elements("gshape"),"color1"->elements("color1"),"gopa"->elements("gopa"),
      "gsize"->elements("gsize"),"gfont"->elements("gfont"),"color2"->elements("color2"),"eshape"->elements("eshape"),
      "color3"->elements("color3"),"eopa"->elements("eopa"),"esize"->elements("esize"),"efont"->elements("efont"),
      "color4"->elements("color4"),"color5"->elements("color5"),"opacity"->elements("opacity"),"dot"->elements("dot"),
      "pthres"->elements("pthres"),"cthres"->elements("cthres")).toString()

    val command = "Rscript "+Utils.path+"R/net/node_attr_calculate.R -t "+ dutyDir + "/out/result.xls" +
      " -pt " + elements("pthres") + " -ct " + elements("cthres") + " -o " + dutyDir + "/out"

    println(command)
    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(command, dutyDir+"/temp")
    Json.obj("valid"->"true", "elements"->newelements)
  }


}

