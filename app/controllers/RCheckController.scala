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

class RCheckController @Inject()(cc: ControllerComponents, dutydao: dutyDao, rservice: RService, dutyController: DutyController)(implicit exec: ExecutionContext) extends AbstractController(cc) with MyStringTool {

  val userDutyDir: String =Utils.path+"users/"

  case class TaxFunData(taskname:String)

  val TaxFunForm: Form[TaxFunData] =Form(
    mapping (
      "taskname"->text
    )(TaxFunData.apply)(TaxFunData.unapply)
  )



  //innergroup correlation
  case class InnerGroupData(taskname:String,anatype:String)

  val InnerGroupForm: Form[InnerGroupData] =Form(
    mapping (
      "taskname"->text,
      "anatype"->text
    )(InnerGroupData.apply)(InnerGroupData.unapply)
  )

  def doIGC(tablenum:Int)=Action(parse.multipartFormData) { implicit request =>
    val data = InnerGroupForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=rservice.creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val tableFile1=new File(dutyDir,"table1.txt")
    val tableFile2=new File(dutyDir,"table2.txt")

    val file1 = request.body.file("table1").get
    val file2 = request.body.file("table2")
    val (input,tablepath) =
      if(file2.isEmpty) {
        rservice.fileTrimMove(file1.ref, tableFile1)
//        file1.ref.moveTo(tableFile1)
        (file1.filename," -i1 "+tableFile1.getAbsolutePath)
      } else {
        rservice.fileTrimMove(file1.ref, tableFile1)
        rservice.fileTrimMove(file2.get.ref, tableFile2)
//        file1.ref.moveTo(tableFile1)
//        file2.get.ref.moveTo(tableFile2)
        (file1.filename+"/" + file2.get.filename," -i1 "+tableFile1.getAbsolutePath+" -i2 "+tableFile2.getAbsolutePath)
      }

    val param= "分析类型：" + data.anatype

    val elements=Json.obj("smt"->"full","cluster_rows"->"FALSE","crm"->"complete",
      "rp"->"1","cluster_cols"->"FALSE","ccm"->"complete","cp"->"1","sc"->"none","lg"->"none",
      "color"->"#E41A1C:#FFFF00:#1E90FF","cc"->"30","nc"->"#DDDDDD","hasborder"->"white",
      "cbc"->"#ffffff","hasnum"->"FALSE","hasrname"->"TRUE","hascname"->"TRUE",
      "rtree"->"50","ctree"->"50","xfs"->"10","yfs"->"10","xfa"->"90","fn"->"8","lfi"->"TRUE").toString() + "/" +
      Json.obj("gshape"->"ellipse","netcolor1"->"#555555","gopa"->"1","gsize"->"5",
        "gfont"->"20","netcolor2"->"#ffffff","eshape"->"diamond","netcolor3"->"#5da5fb","eopa"->"1",
        "esize"->"10","efont"->"20","netcolor4"->"#ffffff","netcolor5"->"#737373","opacity"->"0.6",
        "dot"->"3","pthres"->"0.1","cthres"->"0.5","rows"->"0").toString()

    //数据库加入duty（运行中）
    val start = if(tablenum == 2)
      dutyController.insertDuty(data.taskname,id,"ITC","组间相关性分析",input,param,elements)
    else
      dutyController.insertDuty(data.taskname,id,"IGC","组内相关性分析",input,param,elements)
    //矩阵文件读取写入任务文件下table.txt

    Future{
      val command1 = "Rscript "+Utils.path+"R/igc/cor_pvalue_calculate.R" + tablepath +
        " -o " + dutyDir + "/out" + " -m " + data.anatype
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
        val finish=dutyController.updateFini(id,data.taskname)
        Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.tiff")
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\nFinish Time:"+finish+"\n\n运行成功！")
        rservice.creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readIGC(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val splitele=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements.split("/")
    val elements1=rservice.jsonToMap(splitele(0))
    val elements2=rservice.jsonToMap(splitele(1))
    val head=FileUtils.readFileToString(new File(path+"/out/cor.xls")).trim.split("\n")
    val rnum=head.length-1
    val cnum=head(1).trim.split("\t").length-1

    val pics=rservice.getReDrawPics(path)
    val tableFile2=new File(path,"table2.txt")

    var result=FileUtils.readLines(new File(path+"/out/pandv.xls")).asScala.drop(1)
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
    println(resultFilter.toBuffer.length)

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

    //    val edgehigh=Json.obj("selector"-> "edge.highlighted","style"->Json.obj("line-color"-> "#2a6cd6", "target-arrow-color"-> "#2a6cd6", "opacity"-> 0.7))

    val edgehigh=Json.obj("selector"-> "edge.highlighted","style"->Json.obj("line-color"-> "#2a6cd6", "target-arrow-color"-> "#2a6cd6", "opacity"-> 0.7, "label"-> "data(label)", "edge-text-rotation"-> "autorotate"))

    //    val edgesele=Json.obj("selector"-> "edge[label]:selected","style"->Json.obj("line-color"-> "#2a6cd6", "target-arrow-color"-> "#2a6cd6", "opacity"-> 0.7, "label"-> "data(label)", "edge-text-rotation"-> "autorotate"))

    val edge=Json.obj("selector"-> "edge","style"->Json.obj("curve-style"-> "haystack", "haystack-radius"-> "0.5", "opacity"-> elements2("opacity"), "line-color"-> elements2("netcolor5"), "width"-> "mapData(weight, 0, 1, 1, 8)", "overlay-padding"-> "3px"))

    val selector=Array(node,nodegene,nodeevi,nodesele,edge,edgehigh)

    Ok(Json.obj("len"->len,"pics"->pics,"rnum"->rnum,"cnum"->cnum,"elements1"->elements1,"elements2"->elements2,"allcol"->(head(1).trim.split("\t").length-1),"allrow"->(head.length-1),"rows"->rows,"selector"->selector,"table2"->tableFile2.exists()))
  }


  case class ReIGCHeatData(smt:String,lfi:String,cluster_rows:String,crm:String,rp:String, cluster_cols:String,
                           ccm:String, cp:String,designcolor:String,lg:String,color:String,cc:String,nc:String,
                           hasborder:String,cbc:String,hasrname:String, hascname:String,hasnum:String)

  val ReIGCHeatForm: Form[ReIGCHeatData] =Form(
    mapping (
      "smt"->text,
      "lfi"->text,
      "cluster_rows"->text,
      "crm"->text,
      "rp"->text,
      "cluster_cols"->text,
      "ccm"->text,
      "cp"->text,
      "designcolor"->text,
      "lg"->text,
      "color"->text,
      "cc"->text,
      "nc"->text,
      "hasborder"->text,
      "cbc"->text,
      "hasrname"->text,
      "hascname"->text,
      "hasnum"->text
    )(ReIGCHeatData.apply)(ReIGCHeatData.unapply)
  )

  def redrawIGCHeat(taskname:String,rtree:String,ctree:String,xfs:String,yfs:String,sc:String,xfa:String,fn:String)=Action(parse.multipartFormData) { implicit request =>
    val data=ReIGCHeatForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname

    val tableFile=new File(dutyDir+"/out","cor.xls")
    val tagFile=new File(dutyDir+"/out","p_star.xls")

    val lfi=
      if(data.lfi=="TRUE") " -lfi " + tagFile.getAbsolutePath
      else ""

    val rowclu=
      if(data.cluster_rows=="TRUE") " -crw " + data.cluster_rows + " -crm " + data.crm + " -rp " + data.rp
      else " -crw " + data.cluster_rows
    val colclu=
      if(data.cluster_cols=="TRUE") " -ccl " + data.cluster_cols + " -ccm " + data.ccm + " -cp " + data.cp
      else " -ccl " + data.cluster_cols

    val color= if(data.color=="0") data.designcolor else data.color
    val cbc= if(data.hasborder=="white") " -cbc " + data.cbc else " -cbc " + data.hasborder

    val elenet=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements.split("/")(1)

    val elements=Json.obj("smt"->data.smt,"cluster_rows"->data.cluster_rows,"crm"->data.crm,
      "rp"->data.rp,"cluster_cols"->data.cluster_cols,"ccm"->data.ccm,"cp"->data.cp,"sc"->sc,"lg"->data.lg,
      "color"->color,"cc"->data.cc,"nc"->data.nc,"hasborder"->data.hasborder,"cbc"->data.cbc,
      "hasnum"->data.hasnum, "hasrname"->data.hasrname, "hascname"->data.hascname,"rtree"->rtree,
      "ctree"->ctree,"xfs"->xfs, "yfs"->yfs,"xfa"->xfa,"fn"->fn,"lfi"->data.lfi).toString()+"/"+elenet


    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    val head=FileUtils.readFileToString(tableFile).trim.split("\n")
    val rnum = (head.length-1).toString
    val cnum = (head(1).trim.split("\t").length-1).toString

    val command = "Rscript "+Utils.path+"R/heatmap/heatMap.R -i "+ tableFile.getAbsolutePath +
      " -o " +dutyDir+"/out"+ " -smt " + data.smt + lfi + rowclu + colclu + " -sc " + sc +
      " -lg " + data.lg + " -c " + color + " -cc " + data.cc + " -nc " + data.nc + cbc +
      " -sn " + data.hasrname + ":" + data.hascname + ":" + data.hasnum + " -th " +
      rtree + ":" + ctree + " -fs " + yfs + ":" + xfs + " -if pdf -cln TRUE -xfa " + xfa + " -fn " + fn

    println(command)

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(command,dutyDir+"/temp")

    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/heatmap.pdf",dutyDir+"/out/heatmap.tiff") //替换图片
      rservice.creatZip(dutyDir) //替换压缩文件包
      val pics=dutyDir+"/out/heatmap.png"
      Ok(Json.obj("valid"->"true","pics"->pics,"cnum"->cnum,"rnum"->rnum))
    } else {
      Ok(Json.obj("valid"->"false","message"->execCommand.getErrStr))
    }

  }

  case class ReIGCNetData(dot:String,pthres:String,cthres:String,gshape:String, netcolor1:String,gopa:String,gsize:String,gfont:String,
                          netcolor2:String,eshape:String,netcolor3:String,eopa:String,esize:String,
                          efont:String,netcolor4:String,netcolor5:String, opacity:String, rows:String)

  val ReIGCNetForm: Form[ReIGCNetData] =Form(
    mapping (
      "dot"->text,
      "pthres"->text,
      "cthres"->text,
      "gshape"->text,
      "netcolor1"->text,
      "gopa"->text,
      "gsize"->text,
      "gfont"->text,
      "netcolor2"->text,
      "eshape"->text,
      "netcolor3"->text,
      "eopa"->text,
      "esize"->text,
      "efont"->text,
      "netcolor4"->text,
      "netcolor5"->text,
      "opacity"->text,
      "rows"->text
    )(ReIGCNetData.apply)(ReIGCNetData.unapply)
  )

  def redrawIGCNet(taskname:String)=Action(parse.multipartFormData) { implicit request =>
    val data=ReIGCNetForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val eleheat=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements.split("/")(0)
    val elements=eleheat+"/"+Json.obj("gshape"->data.gshape,"netcolor1"->data.netcolor1,
      "gopa"->data.gopa,"gsize"->data.gsize,"gfont"->data.gfont,"netcolor2"->data.netcolor2,
      "eshape"->data.eshape,"netcolor3"->data.netcolor3,"eopa"->data.eopa,"esize"->data.esize,
      "efont"->data.efont,"netcolor4"->data.netcolor4,"netcolor5"->data.netcolor5,
      "opacity"->data.opacity,"dot"->data.dot,"pthres"->data.pthres,"cthres"->data.cthres,
      "rows"->data.rows).toString()
    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    val command = "Rscript "+Utils.path+"R/net/node_attr_calculate.R -t "+ path + "/out/pandv.xls" +
      " -pt " + data.pthres + " -ct " + data.cthres + " -o " + path + "/out"

    println(command)
    FileUtils.writeStringToFile(new File(s"$path/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(command, path+"/temp")

    Ok(Json.obj("valid"->"true"))
  }



  //T Test
  case class tTestData(taskname:String, p:String, ve:String, c:String, ptn:String, qtn:String)

  val tTestForm: Form[tTestData] =Form(
    mapping (
      "taskname"->text,
      "p"->text,
      "ve"->text,
      "c"->text,
      "ptn"->text,
      "qtn"->text
    )(tTestData.apply)(tTestData.unapply)
  )

  def doTTest=Action(parse.multipartFormData) { implicit request =>
    val data = tTestForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=rservice.creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")

    val file1 = request.body.file("table1").get
    val file2 = request.body.file("table2").get
    rservice.fileTrimMove(file1.ref, tableFile)
    rservice.fileTrimMove(file2.ref, groupFile)
//    file1.ref.moveTo(tableFile)
//    file2.ref.moveTo(groupFile)
    val input = file1.filename + "/" + file2.filename

    val ve = if(data.ve == "FALSE") "异方差双样本检验" else "等方差双样本检验"

    val param= "是否要做配对T检验：" + data.p + "/进行何种方差检验：" + ve +
      "/多重校验方法：" + data.c + "/P值检验阈值：" + data.ptn + "/Q值检验阈值：" + data.qtn

    //数据库加入duty（运行中）
    val start = dutyController.insertDuty(data.taskname,id,"TC","T检验",input,param,"")

    Future{

      val ptn = if(data.ptn == "") "" else " -ptn " + data.ptn
      val qtn = if(data.qtn == "") "" else " -qtn " + data.qtn

      val command = "Rscript "+Utils.path+"R/t_test/t-test.R -i " + tableFile.getAbsolutePath +
        " -g " + groupFile.getAbsolutePath + " -o " + dutyDir + "/out" + " -p " + data.p +
        " -ve " + data.ve + " -c " + data.c + ptn + qtn

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      println(command)

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



  //Wilcoxon Test
  case class WilcoxonData(taskname:String, p:String, tf:String, c:String, ptn:String, qtn:String)

  val WilcoxonForm: Form[WilcoxonData] =Form(
    mapping (
      "taskname"->text,
      "p"->text,
      "tf"->text,
      "c"->text,
      "ptn"->text,
      "qtn"->text
    )(WilcoxonData.apply)(WilcoxonData.unapply)
  )

  def doWilcoxon=Action(parse.multipartFormData) { implicit request =>
    val data = WilcoxonForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=rservice.creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")

    val file1 = request.body.file("table1").get
    val file2 = request.body.file("table2").get
    rservice.fileTrimMove(file1.ref, tableFile)
    rservice.fileTrimMove(file2.ref, groupFile)
//    file1.ref.moveTo(tableFile)
//    file2.ref.moveTo(groupFile)
    val input = file1.filename + "/" + file2.filename

    val param= "是否要做配对T检验：" + data.p + "/是否在p值的正态近似中应用连续性校正：" + data.tf +
      "/多重校验方法：" + data.c + "/P值检验阈值：" + data.ptn + "/Q值检验阈值：" + data.qtn

    //数据库加入duty（运行中）
    val start = dutyController.insertDuty(data.taskname,id,"WT","Wilcoxon秩和检验",input,param,"")

    Future{
      val ptn = if(data.ptn == "") "" else " -ptn " + data.ptn
      val qtn = if(data.qtn == "") "" else " -qtn " + data.qtn

      val command = "Rscript "+Utils.path+"R/wilcoxon/wilcoxon.R -i " + tableFile.getAbsolutePath +
        " -g " + groupFile.getAbsolutePath + " -o " + dutyDir + "/out" + " -p " + data.p +
        " -tf " + data.tf + " -c " + data.c + ptn + qtn

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      println(command)

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



  //Kruskal-Wallis
  case class KruskalData(taskname:String, c:String, ptn:String, qtn:String)

  val KruskalForm: Form[KruskalData] =Form(
    mapping (
      "taskname"->text,
      "c"->text,
      "ptn"->text,
      "qtn"->text
    )(KruskalData.apply)(KruskalData.unapply)
  )

  def doKruskal=Action(parse.multipartFormData) { implicit request =>
    val data = KruskalForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=rservice.creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")

    val file1 = request.body.file("table1").get
    val file2 = request.body.file("table2").get
    rservice.fileTrimMove(file1.ref, tableFile)
    rservice.fileTrimMove(file2.ref, groupFile)
//    file1.ref.moveTo(tableFile)
//    file2.ref.moveTo(groupFile)
    val input = file1.filename + "/" + file2.filename

    val param= "/多重校验方法：" + data.c + "/P值检验阈值：" + data.ptn + "/Q值检验阈值：" + data.qtn

    //数据库加入duty（运行中）
    val start = dutyController.insertDuty(data.taskname,id,"KWT","Kruskal-Wallis秩和检验",input,param,"")

    Future{
      val ptn = if(data.ptn == "") "" else " -ptn " + data.ptn
      val qtn = if(data.qtn == "") "" else " -qtn " + data.qtn

      val command = "Rscript "+Utils.path+"R/kruskalwallis/kruskal-wallis.R -i " + tableFile.getAbsolutePath +
        " -g " + groupFile.getAbsolutePath + " -o " + dutyDir + "/out" + " -c " + data.c + ptn + qtn

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      println(command)

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



  //ANOVA
  case class AnovaData(taskname:String, c:String, ptn:String, qtn:String)

  val AnovaForm: Form[AnovaData] =Form(
    mapping (
      "taskname"->text,
      "c"->text,
      "ptn"->text,
      "qtn"->text
    )(AnovaData.apply)(AnovaData.unapply)
  )

  def doAnova=Action(parse.multipartFormData) { implicit request =>
    val data = AnovaForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=rservice.creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")

    val file1 = request.body.file("table1").get
    val file2 = request.body.file("table2").get
    rservice.fileTrimMove(file1.ref, tableFile)
    rservice.fileTrimMove(file2.ref, groupFile)
//    file1.ref.moveTo(tableFile)
//    file2.ref.moveTo(groupFile)
    val input = file1.filename + "/" + file2.filename

    val param= "/多重校验方法：" + data.c + "/P值检验阈值：" + data.ptn + "/Q值检验阈值：" + data.qtn

    //数据库加入duty（运行中）
    val start = dutyController.insertDuty(data.taskname,id,"AOV","方差分析（ANOVA）",input,param,"")

    Future{
      val ptn = if(data.ptn == "") "" else " -ptn " + data.ptn
      val qtn = if(data.qtn == "") "" else " -qtn " + data.qtn

      val command = "Rscript "+Utils.path+"R/anova/anova.R -i " + tableFile.getAbsolutePath +
        " -g " + groupFile.getAbsolutePath + " -o " + dutyDir + "/out" + " -c " + data.c + ptn + qtn

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      execCommand.exect(command,dutyDir+"/temp")

      println(command)

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


  //plsda
  def doPlsda=Action(parse.multipartFormData) { implicit request =>
    val data = TaxFunForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=rservice.creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val tableFile=new File(dutyDir,"table.txt")
    val groupFile=new File(dutyDir,"group.txt")
    val file1 = request.body.file("table1").get
    rservice.fileTrimMove(file1.ref, tableFile)
//    file1.ref.moveTo(tableFile)
    val file2 = request.body.file("table2")
    rservice.fileTrimMove(file2.get.ref, groupFile)
//    file2.get.ref.moveTo(groupFile)
    val (group,filepara) = (" -g " + groupFile.getAbsolutePath,file1.filename+"/"+file2.get.filename)
    val co = "#CD0000:#3A89CC:#769C30:#D99536:#7B0078:#BFBC3B:#E2609F:#00688B:#C10077:#CAAA76:#EEEE00:#458B00:#8B4513:#008B8B:#6E8B3D:#8B7D6B:#7FFF00:#CDBA96:#ADFF2F"

    val elements= Json.obj("xdata"->"p1","ydata"->"p2","width"->"15","length"->"12","showname"->"FALSE",
      "showerro"->"TRUE","color"->co,"resolution"->"300","xts"->"15","yts"->"15","xls"->"17","yls"->"17",
      "lts"->"14","lms"->"15","lmtext"->"","ms"->"17","mstext"->"","c"->"FALSE","big"->"no",
      "xdamin"->"","xdamax"->"","ydamin"->"","ydamax"->"").toString()


    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"PLS","（O）PLS-DA",filepara,"/",elements)
    //矩阵文件读取写入任务文件下table.txt

    Future{
      val command1 = "Rscript " + Utils.path + "R/pls-da/plsda_data.R -i " + tableFile.getAbsolutePath +
        group + " -o " + dutyDir + "/out"

      val command2 = "Rscript " + Utils.path + "R/pcoa/pcoa-plot.R -i " + dutyDir +
        "/out/PLSDA_score.xls" + group + " -si " + dutyDir + "/out/PLSDA_sdev.xls" +
        " -o " +dutyDir+"/out" + " -if pdf -in pls-da -pxy p1:p2"

      println(command1 + " && \n" + command2)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"), command1 + " && \n" + command2)
      val execCommand = new ExecCommand
      execCommand.exect(Array(command1,command2),new File(dutyDir+"/temp"))

      if (execCommand.isSuccess) {
        Utils.pdf2Png(dutyDir+"/out/pls-da.pdf",dutyDir+"/out/pls-da.png")
        val finish=dutyController.updateFini(id,data.taskname)
        Utils.pdf2Png(dutyDir+"/out/pls-da.pdf",dutyDir+"/out/pls-da.tiff")
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\nFinish Time:"+finish+"\n\n运行成功！")
        rservice.creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n运行失败！")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }

  def readPlsdaData(taskname:String): Action[AnyContent] =Action{ implicit request=>
    val id=request.session.get("userId").get
    val path=Utils.path+"/users/"+id+"/"+taskname

    val elements=rservice.jsonToMap(Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf).head.elements)
    val color=elements("color").split(":")

    val head=FileUtils.readFileToString(new File(path+"/table.txt")).trim.split("\n")
    val gnum=head(0).trim.split("\t").drop(1).length
    //获取分组
    val group=
      if(new File(path+"/group.txt").exists()) {
        val f=FileUtils.readLines(new File(path+"/group.txt")).asScala
        val g=f.map{_.split('\t').last}.distinct.drop(1).sorted
        //检查group的数量与矩阵head是否一样，小于则+nogroup，相等则不变
        if(f.map{_.split('\t').head}.drop(1).length<gnum) g.append("nogroup")
        g.toArray
      }else Array("nogroup")

    //获取x,y轴数据
    val filepath=path+"/out/PLSDA_score.xls"
    val data=FileUtils.readLines(new File(filepath))
    val col=data.get(0).split("\t").filter(_.trim!="").map(_.trim)
    //获取图片
    val pics=rservice.getReDrawPics(path)
    Ok(Json.obj("group"->group,"cols"->col,"pics"->pics,"elements"->elements,"color"->color))
  }

  case class RePlsdaData(xdata:String,ydata:String,showname:String,showerro:String,color:String,
                       width:String, length:String,resolution:String,xts:String,yts:String,
                       xls:String,yls:String,lts:String,lms:String,lmtext:String,ms:String,
                       mstext:String,c:String)

  val RePlsdaForm: Form[RePlsdaData] =Form(
    mapping (
      "xdata"->text,
      "ydata"->text,
      "showname"->text,
      "showerro"->text,
      "color"->text,
      "width"->text,
      "length"->text,
      "resolution"->text,
      "xts"->text,
      "yts"->text,
      "xls"->text,
      "yls"->text,
      "lts"->text,
      "lms"->text,
      "lmtext"->text,
      "ms"->text,
      "mstext"->text,
      "c"->text
    )(RePlsdaData.apply)(RePlsdaData.unapply)
  )

  case class RePlsdaData2(big:String,xdamin:String,xdamax:String,ydamin:String,ydamax:String)
  val RePlsdaForm2: Form[RePlsdaData2] =Form(
    mapping (
      "big"->text,
      "xdamin"->text,
      "xdamax"->text,
      "ydamin"->text,
      "ydamax"->text
    )(RePlsdaData2.apply)(RePlsdaData2.unapply)
  )

  def redrawPlsda(taskname:String)=Action(parse.multipartFormData) { implicit request =>
    val data=RePlsdaForm.bindFromRequest.get
    val data2=RePlsdaForm2.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=Utils.path+"users/"+id+"/"+taskname
    val groupFile=new File(dutyDir,"group.txt")
    val groupdata=if(groupFile.exists()) " -g " + groupFile.getAbsolutePath else ""

    val elements= Json.obj("xdata"->data.xdata,"ydata"->data.ydata,"width"->"15","length"->"12",
      "showname"->data.showname,"showerro"->data.showerro,"color"->data.color,"resolution"->data.resolution,
      "xts"->data.xts,"yts"->data.yts,"xls"->data.xls,"yls"->data.yls,"lts"->data.lts,"lms"->data.lms,
      "lmtext"->data.lmtext,"ms"->data.ms,"mstext"->data.mstext,"c"->data.c,"big"->data2.big,
      "xdamin"->data2.xdamin,"xdamax"->data2.xdamax,"ydamin"->data2.ydamin,
      "ydamax"->data2.ydamax).toString()

    Await.result(dutydao.updateElements(id,taskname,elements),Duration.Inf)

    val c=
      if(!groupFile.exists()) " -oc \""+data.color+"\""
      else " -cs \""+ data.color+"\""

    val name=if(data.showname.equals("TRUE") && groupFile.exists()){
      val f=FileUtils.readLines(groupFile).asScala
      val n=f.map{_.split('\t').last}.distinct.drop(1).mkString(",")
      " -b " + n
    }else if(data.showname.equals("TRUE") && !groupFile.exists()){
      " -sl TRUE"
    }
    else ""

    val big=if(data2.big=="no") ""
    else if(data2.big=="x")
      " -da x:"+data2.xdamin+","+data2.xdamax
    else " -da y:"+data2.ydamin+","+data2.ydamax

    println(data.ydata)

    val lms=if(!data.lmtext.equals("")) " -lms sans:bold.italic:" + data.lms + ":\"" + data.lmtext+"\"" else ""
    val ms=if(!data.mstext.equals("")) " -ms sans:plain:" + data.ms + ":\"" + data.mstext+"\"" else ""

    val script = "R/pcoa/pcoa-plot.R" + " -i " + dutyDir + "/out/PLSDA_score.xls" + " -si " + dutyDir+"/out/PLSDA_sdev.xls"

    val command = "Rscript " + Utils.path + script + groupdata + " -o " +dutyDir+"/out" + " -pxy "+ data.xdata +
      ":" + data.ydata + " -is "+ data.width + ":" + data.length + c + " -dpi " + data.resolution + " -xts " +
      "sans:plain:"+data.xts + " -yts " + "sans:plain:"+data.yts + " -xls " + "sans:plain:"+data.xls + " -yls " +
      "sans:plain:"+data.yls + " -lts " + "sans:plain:" + data.lts + " -if pdf -in pls-da -ss " + data.showerro + name +
      lms + ms + " -c " + data.c + big

    FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
    val execCommand = new ExecCommand
    execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")

    println(command)

    if (execCommand.isSuccess) {
      Utils.pdf2Png(dutyDir+"/out/pls-da.pdf",dutyDir+"/out/pls-da.png") //替换图片
      Utils.pdf2Png(dutyDir+"/out/pls-da.pdf",dutyDir+"/out/pls-da.tiff") //替换图片
      rservice.creatZip(dutyDir) //替换压缩文件包
      val pics=dutyDir+"/out/pls-da.png"
      Ok(Json.obj("valid"->"true","pics"->pics))
    } else {
      Ok(Json.obj("valid"->"false"))
    }

  }


}
