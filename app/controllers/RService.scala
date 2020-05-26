package controllers

import java.io.File
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date

import dao.dutyDao
import models.Tables.{Dutys, DutysRow}
import javax.inject.Inject
import org.apache.commons.io.FileUtils
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents, Headers}
import utils.{ExecCommand, Utils}
import controllers.DutyController

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class RService @Inject()(cc: ControllerComponents,dutydao:dutyDao,dutyController: DutyController)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  case class PCA1Data(taskname:String,text1:String,text2:String,txdata1:String,scale:String)

  val PCA1Form=Form(
    mapping (
      "taskname"->text,
      "text1"->text,
      "text2"->text,
      "txdata1"->text,
      "scale"->text
    )(PCA1Data.apply)(PCA1Data.unapply)
  )

  def doPCA(id:String)=Action(parse.multipartFormData){implicit request=>
    val data=PCA1Form.bindFromRequest.get
    //checktaskname重复
    val checkTaskname= Await.result(dutydao.checkTaskName(id,data.taskname),Duration.Inf)
    //1.check 矩阵表格是否上传
    if(data.text1.isEmpty)
      Ok(Json.obj("valid" -> "false", "message" -> "请上传矩阵表格文件！"))
    else if(checkTaskname.length==1) { //2.check taskname重复
      Ok(Json.obj("valid" -> "false", "message" -> "任务编号已存在，请换一个编号！"))
    } else {
        //数据库加入duty
        dutyController.insertDuty(data.taskname,id,"3","主成分分析（PCA）")
        //在用户下创建任务文件夹和结果文件夹
        val dutyDir=creatUserDir(id,data.taskname)
        val tableFile=new File(dutyDir,"table.txt")
        val groupFile=new File(dutyDir,"group.txt")
        val inputFile=new File(dutyDir,"input.txt")
        val paramFile=new File(dutyDir,"param.txt")

        val input=data.text1.substring(data.text1.lastIndexOf("\\")+1)+";"+data.text2.substring(data.text2.lastIndexOf("\\")+1)
        FileUtils.writeStringToFile(inputFile,input)

        val param="归一化："+data.scale
        FileUtils.writeStringToFile(paramFile,param)

        //矩阵文件读取写入任务文件下table.txt
        val file = request.body.file("table1").get
        val tabledatas = FileUtils.readFileToString(file.ref.file)
        FileUtils.writeStringToFile(tableFile, tabledatas)

      //是否有group文件
      val group=
        if(data.text2.isEmpty&&data.txdata1.isEmpty) ""
        else if(!data.text2.isEmpty){
          val file = request.body.file("table2").get
          val groupdatas = FileUtils.readFileToString(file.ref.file)
          FileUtils.writeStringToFile(groupFile, groupdatas)
          " -g "+groupFile.getAbsolutePath
        }else{
          FileUtils.writeStringToFile(groupFile, data.txdata1)
          " -g "+groupFile.getAbsolutePath
        }
      val command = "perl "+Utils.path+"R/pca/plot-pca.pl -i "+ tableFile.getAbsolutePath +" -html "+ dutyDir+"/out/"+"1.html" +" -zscal "+ data.scale + group
      val execCommand = new ExecCommand
      //exec需要指定结果输出路径的时候，不指定默认本地任务路径
      execCommand.exect(command,dutyDir+"/out")
      if (execCommand.isSuccess) {
        val excel = execCommand.getOutStr
        dutydao.updateFini(id,data.taskname)
        Ok(Json.obj("excel" -> excel,"taskname"->data.taskname,"userId"->id,"path"->dutyDir))
      } else {
        dutydao.updateFailed(id,data.taskname)
        Ok(Json.obj("valid" -> "false", "message" -> "文件格式错误,请检查！"))
      }
    }
  }



  case class HeatmapData(taskname:String,text1:String,colnum:String,text2:String,rownum:String,scale:String,cluster_rows:String,
                         cluster_cols:String,color:String,color1:String,color2:String,color3:String,fontsize:String,width:String,
                         height:String,hasnum:String,hasborder:String,hasrname:String,hascname:String)

  val HeatmapForm=Form(
    mapping (
      "taskname"->text,
      "text1"->text,
      "colnum"->text,
      "text2"->text,
      "rownum"->text,
      "scale"->text,
      "cluster_rows"->text,
      "cluster_cols"->text,
      "color"->text,
      "color1"->text,
      "color2"->text,
      "color3"->text,
      "fontsize"->text,
      "width"->text,
      "height"->text,
      "hasnum"->text,
      "hasborder"->text,
      "hasrname"->text,
      "hascname"->text,
    )(HeatmapData.apply)(HeatmapData.unapply)
  )


  def doHeatmap(id:String)=Action(parse.multipartFormData) { implicit request =>
    val data = HeatmapForm.bindFromRequest.get
    val checkTaskname= Await.result(dutydao.checkTaskName(id,data.taskname),Duration.Inf)
    if(data.text1.isEmpty)
      Ok(Json.obj("valid" -> "false", "message" -> "请上传矩阵表格文件！"))
    else if(checkTaskname.length==1) {
      Ok(Json.obj("valid" -> "false", "message" -> "任务编号已存在，请换一个编号！"))
    } else {
      dutyController.insertDuty(data.taskname,id,"5","Heatmap 热图")
      val dutyDir=creatUserDir(id,data.taskname)

      val tableFile=new File(dutyDir,"table.txt")
      val file = request.body.file("table1").get
      val tabledatas = FileUtils.readFileToString(file.ref.file)
      FileUtils.writeStringToFile(tableFile, tabledatas)

      val groupColFile=new File(dutyDir,"groupCol.txt")
      val group_col=
        if(data.colnum.isEmpty) ""
        else{
          FileUtils.writeStringToFile(groupColFile, tabledatas)
          " -g "+ groupColFile.getAbsolutePath
        }

      val groupRowFile=new File(dutyDir,"groupRow.txt")
      //对table文件做处理放入groupRow.txt
      val group_row=
        if(data.text2.isEmpty&&data.rownum.isEmpty) ""
        else if(!data.text2.isEmpty){
          val file = request.body.file("table2").get
          val groupdatas = FileUtils.readFileToString(file.ref.file)
          FileUtils.writeStringToFile(groupRowFile, groupdatas)
          " -g "+groupRowFile.getAbsolutePath
        }else{
          FileUtils.writeStringToFile(groupRowFile, tabledatas)
          " -g "+groupRowFile.getAbsolutePath
        }

      val scale=data.scale //(row,column,none)

      val color1=data.color match {
        case "0" => "#92d050"
        case "1" => "#000080"
        case "2" => data.color1
      }

      val color2=data.color match {
        case "0" => "#ff0000"
        case "1" => "#ffffff"
        case "2" => data.color2
      }

      val color3=data.color1 match {
        case "0" => "#000000"
        case "1" => "#B22222"
        case "2" => data.color3
      }

      val command= tableFile+group_row+group_col+scale+data.cluster_cols+data.cluster_rows+color1+color2+color3+data.fontsize+
        data.width+data.height+data.hasnum+data.hasborder+data.hasrname+data.hascname
      val execCommand = new ExecCommand
      //exec需要指定结果输出路径的时候，不指定默认本地任务路径
      execCommand.exect(command,dutyDir+"/out")
      if (execCommand.isSuccess) {
        val excel = execCommand.getOutStr
        dutydao.updateFini(id,data.taskname)
        Ok(Json.obj("excel" -> excel,"taskname"->data.taskname,"userId"->id,"path"->dutyDir))
      } else {
        dutydao.updateFailed(id,data.taskname)
        Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
      }
    }
  }

  case class BoxplotData(taskname:String,color:String,ylim1:String,ylim2:String)

  val BoxplotForm = Form(
    mapping (
      "taskname"->text,
      "color"->text,
      "ylim1"->text,
      "ylim2"->text
    )(BoxplotData.apply)(BoxplotData.unapply)
  )


  def doBoxplot(id:String,textnum:Int)=Action(parse.multipartFormData){ implicit request=>
    val data=BoxplotForm.bindFromRequest.get

    //checktaskname重复
    val checkTaskname= Await.result(dutydao.checkTaskName(id,data.taskname),Duration.Inf)
    //1.check 矩阵表格是否上传
    if(checkTaskname.length==1) { //2.check taskname重复
      Ok(Json.obj("valid" -> "false", "message" -> "任务编号已存在，请换一个编号！"))
    } else {
      //数据库加入duty
      dutyController.insertDuty(data.taskname,id,"6","Boxplot 盒型图")
      //在用户下创建任务文件夹和结果文件夹
      val dutyDir=creatUserDir(id,data.taskname)

      val t=1
      //这里有多个文件路径，使用的时候dutyDir/table(t).txt
      while(t<=textnum){
        val tableFile=new File(dutyDir,"table"+t+".txt")
        val file = request.body.file("table"+t).get
        val tabledatas = FileUtils.readFileToString(file.ref.file)
        FileUtils.writeStringToFile(tableFile, tabledatas)
      }

      //颜色的Array[String]
      val color=data.color.split(",")


      val command = ""
      val execCommand = new ExecCommand
      //exec需要指定结果输出路径的时候，不指定默认本地任务路径
      execCommand.exect(command,dutyDir+"/out")
      if (execCommand.isSuccess) {
        val excel = execCommand.getOutStr
        dutydao.updateFini(id,data.taskname)
        Ok(Json.obj("excel" -> excel,"taskname"->data.taskname,"userId"->id,"path"->dutyDir))
      } else {
        dutydao.updateFailed(id,data.taskname)
        Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
        //运行失败，修改状态，删除建立的文件夹
      }

    }
  }


  case class GoData(taskname:String,text1:String,txdata:String,parameter2:String,parameter3:String,parameter4:String,text2:String)

  val GoForm=Form(
    mapping (
      "taskname"->text,
      "text1"->text,
      "txdata"->text,
      "parameter2"->text,
      "parameter3"->text,
      "parameter4"->text,
      "text2"->text
    )(GoData.apply)(GoData.unapply)
  )

  def doGo(id:String,difftype:String,adddesc:Boolean)=Action(parse.multipartFormData){implicit request=>

    val data=GoForm.bindFromRequest.get
    val checkTaskname= Await.result(dutydao.checkTaskName(id,data.taskname),Duration.Inf)
    if(data.text1.isEmpty&&data.txdata.isEmpty){
      Ok(Json.obj("valid" -> "false", "message" -> "请上传或手动输入基因文件！"))
    }
    else if(checkTaskname.length==1) { //2.check taskname重复
      Ok(Json.obj("valid" -> "false", "message" -> "任务编号已存在，请换一个编号！"))
    } else {
      //数据库加入duty
      dutyController.insertDuty(data.taskname,id,"1","GO富集分析")
      //在用户下创建任务文件夹和结果文件夹
      val dutyDir=creatUserDir(id,data.taskname)
      val geneFile=new File(dutyDir,"gene.txt")
      val goFile=new File(dutyDir,"go.txt")

      if(!data.text1.isEmpty){
        val file = request.body.file("table1").get
        val genedatas = FileUtils.readFileToString(file.ref.file)
        FileUtils.writeStringToFile(geneFile, genedatas)
      } else{
        FileUtils.writeStringToFile(geneFile, data.txdata)
      }

      val command =
      if(adddesc){
        //perl的对应command
        geneFile.getAbsolutePath+data.parameter2+data.parameter3+data.parameter4;
      }else{
        val file=request.body.file("table2").get
        val godatas=FileUtils.readFileToString(file.ref.file)
        FileUtils.writeStringToFile(goFile,godatas)
        //perl的对应command
        geneFile.getAbsolutePath+goFile.getAbsolutePath
      }

      val execCommand = new ExecCommand
      //exec需要指定结果输出路径的时候，不指定默认本地任务路径
      execCommand.exect(command,dutyDir+"/out")
      if (execCommand.isSuccess) {
        val excel = execCommand.getOutStr
        dutydao.updateFini(id,data.taskname)
        Ok(Json.obj("excel" -> excel,"taskname"->data.taskname,"userId"->id,"path"->dutyDir))
      } else {
        dutydao.updateFailed(id,data.taskname)
        Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
        //运行失败，修改状态，删除建立的文件夹
      }
    }
  }

  case class KeggData(taskname:String,text1:String,txdata:String,para22:String,para33:String,para44:String,text2:String,parameter4:String,parameter3:String,text3:String)

  val KeggForm=Form(
    mapping (
      "taskname"->text,
      "text1"->text,
      "txdata"->text,
      "para22"->text,
      "para33"->text,
      "para44"->text,
      "text2"->text,
      "parameter4"->text,
      "parameter3"->text,
      "text3"->text
    )(KeggData.apply)(KeggData.unapply)
  )

  def doKegg(id:String,userefer:Boolean,desc:Boolean)=Action(parse.multipartFormData){implicit request=>
    val data=KeggForm.bindFromRequest.get
    val checkTaskname= Await.result(dutydao.checkTaskName(id,data.taskname),Duration.Inf)
    if(data.text1.isEmpty && data.txdata.isEmpty){
      Ok(Json.obj("valid" -> "false", "message" -> "请上传或手动输入基因列表文件！"))
    }
    else if(!userefer && data.text2.isEmpty){
      Ok(Json.obj("valid" -> "false", "message" -> "请上传背景基因表！"))
    }
    else if(desc && data.text3.isEmpty){
      Ok(Json.obj("valid" -> "false", "message" -> "请上传基因差异倍数表！"))
    }
    else if(checkTaskname.length==1) { //2.check taskname重复
      Ok(Json.obj("valid" -> "false", "message" -> "任务编号已存在，请换一个编号！"))
    }
    else {
      //数据库加入duty
      dutyController.insertDuty(data.taskname,id,"2","KEGG富集分析")
      //在用户下创建任务文件夹和结果文件夹
      val dutyDir=creatUserDir(id,data.taskname)
      val geneFile=new File(dutyDir,"gene.txt")
      val keggFile=new File(dutyDir,"kegg.txt")
      val diffFile=new File(dutyDir,"diff.txt")

      if(!data.text1.isEmpty){
        val file = request.body.file("table1").get
        val genedatas = FileUtils.readFileToString(file.ref.file)
        FileUtils.writeStringToFile(geneFile, genedatas)
      } else{
        FileUtils.writeStringToFile(geneFile, data.txdata)
      }

      if(userefer){
        //读取原有库放入kegg.txt
        data.para22+data.para33+data.para44
      }else{
        val file = request.body.file("table2").get
        val keggdatas = FileUtils.readFileToString(file.ref.file)
        FileUtils.writeStringToFile(keggFile, keggdatas)
        keggFile.getAbsolutePath+data.parameter4+data.parameter3
      }

      if(desc){
        val file = request.body.file("table3").get
        val diffdatas = FileUtils.readFileToString(file.ref.file)
        FileUtils.writeStringToFile(diffFile, diffdatas)
        diffFile.getAbsolutePath
      }


      val command =""

      val execCommand = new ExecCommand
      //exec需要指定结果输出路径的时候，不指定默认本地任务路径
      execCommand.exect(command,dutyDir+"/out")
      if (execCommand.isSuccess) {
        val excel = execCommand.getOutStr
        dutydao.updateFini(id,data.taskname)
        Ok(Json.obj("excel" -> excel,"taskname"->data.taskname,"userId"->id,"path"->dutyDir))
      } else {
        dutydao.updateFailed(id,data.taskname)
        Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
        //运行失败，修改状态，删除建立的文件夹
      }
    }

  }

  case class CcaData(taskname:String, text1:String, text2:String, text3:String, phototype:String, species:String, topspecies:String, photoname:String, plot:String, opacity:String)

  val CcaForm=Form(
    mapping (
      "taskname"->text,
      "text1"->text,
      "text2"->text,
      "text3"->text,
      "phototype"->text,
      "species"->text,
      "topspecies"->text,
      "photoname"->text,
      "plot"->text,
      "opacity"->text
    )(CcaData.apply)(CcaData.unapply)
  )

  def doCca(id:String,color:String)=Action(parse.multipartFormData){implicit request=>
    val data=CcaForm.bindFromRequest.get
    //checktaskname重复
    val checkTaskname= Await.result(dutydao.checkTaskName(id,data.taskname),Duration.Inf)
    //1.check 矩阵表格是否上传
    if(data.text1.isEmpty||data.text2.isEmpty)
      Ok(Json.obj("valid" -> "false", "message" -> "必须上传OTU丰度表和环境因子列表文件！"))
    else if(checkTaskname.length==1) { //2.check taskname重复
      Ok(Json.obj("valid" -> "false", "message" -> "任务编号已存在，请换一个编号！"))
    } else {
      //数据库加入duty
      dutyController.insertDuty(data.taskname,id,"4","CCA/RDA")
      //在用户下创建任务文件夹和结果文件夹
      val dutyDir=creatUserDir(id,data.taskname)
      val otuFile=new File(dutyDir,"otu.txt")
      val listFile=new File(dutyDir,"list.txt")
      val groupFile=new File(dutyDir,"group.txt")

      val file = request.body.file("table1").get
      val otudatas = FileUtils.readFileToString(file.ref.file)
      FileUtils.writeStringToFile(otuFile, otudatas)

      val file2 = request.body.file("table2").get
      val listdatas = FileUtils.readFileToString(file2.ref.file)
      FileUtils.writeStringToFile(listFile, listdatas)

      val group=
      if(!data.text3.isEmpty){
        val file3 = request.body.file("table3").get
        val groupdatas = FileUtils.readFileToString(file3.ref.file)
        FileUtils.writeStringToFile(groupFile, groupdatas)
        groupFile.getAbsolutePath
      }else ""

      //根据CCA/RDA 不同perl command
      val command = data.phototype match{
        case "CCA"=> otuFile.getAbsolutePath+listFile.getAbsolutePath+group+data.phototype+data.species+data.topspecies+data.photoname+data.plot+data.opacity
        case "RDA"=> otuFile.getAbsolutePath+listFile.getAbsolutePath+group+data.phototype+data.species+data.topspecies+data.photoname+data.plot+data.opacity
      }


      val execCommand = new ExecCommand
      //exec需要指定结果输出路径的时候，不指定默认本地任务路径
      execCommand.exect(command,dutyDir+"/out")
      if (execCommand.isSuccess) {
        val excel = execCommand.getOutStr
        dutydao.updateFini(id,data.taskname)
        Ok(Json.obj("excel" -> excel,"taskname"->data.taskname,"userId"->id,"path"->dutyDir))
      } else {
        dutydao.updateFailed(id,data.taskname)
        Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
      }
    }
  }


  case class WeightData(taskname:String,text1:String,txdata:String,lian_type:String,weight1:String,weight2:String)

  val WeightForm=Form(
    mapping (
      "taskname"->text,
      "text1"->text,
      "txdata"->text,
      "lian_type"->text,
      "weight1"->text,
      "weight2"->text
    )(WeightData.apply)(WeightData.unapply)
  )

  def doWeight(id:String)=Action(parse.multipartFormData){implicit  request=>
    val data=WeightForm.bindFromRequest.get
    val checkTaskname= Await.result(dutydao.checkTaskName(id,data.taskname),Duration.Inf)
    if(data.text1.isEmpty && data.txdata.isEmpty){
      Ok(Json.obj("valid" -> "false", "message" -> "请上传或手动输入边界文件！"))
    }
    else if(checkTaskname.length==1) { //2.check taskname重复
      Ok(Json.obj("valid" -> "false", "message" -> "任务编号已存在，请换一个编号！"))
    }
    else {
      //数据库加入duty
      dutyController.insertDuty(data.taskname,id,"7","权重网络图")
      //在用户下创建任务文件夹和结果文件夹
      val dutyDir=creatUserDir(id,data.taskname)
      val borderFile=new File(dutyDir,"border.txt")

      if(!data.text1.isEmpty){
        val file = request.body.file("table1").get
        val borderdatas = FileUtils.readFileToString(file.ref.file)
        FileUtils.writeStringToFile(borderFile, borderdatas)
      } else{
        FileUtils.writeStringToFile(borderFile, data.txdata)
      }

      val command = data.lian_type match {
        case "hard"=> borderFile.getAbsolutePath+data.weight1+data.weight2
        case "soft"=> borderFile.getAbsolutePath+data.weight1+data.weight2
      }

      val execCommand = new ExecCommand
      //exec需要指定结果输出路径的时候，不指定默认本地任务路径
      execCommand.exect(command,dutyDir+"/out")
      if (execCommand.isSuccess) {
        val excel = execCommand.getOutStr
        dutydao.updateFini(id,data.taskname)
        Ok(Json.obj("excel" -> excel,"taskname"->data.taskname,"userId"->id,"path"->dutyDir))
      } else {
        dutydao.updateFailed(id,data.taskname)
        Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
      }
    }
  }


  case class directedData(taskname:String,text1:String,txdata:String)

  val directedForm=Form(
    mapping (
      "taskname"->text,
      "text1"->text,
      "txdata"->text
    )(directedData.apply)(directedData.unapply)
  )

  def doDirected(id:String)=Action(parse.multipartFormData){implicit request=>
    val data=directedForm.bindFromRequest.get
    val checkTaskname= Await.result(dutydao.checkTaskName(id,data.taskname),Duration.Inf)
    if(data.text1.isEmpty && data.txdata.isEmpty){
      Ok(Json.obj("valid" -> "false", "message" -> "请上传或手动输入边界文件！"))
    }
    else if(checkTaskname.length==1) { //2.check taskname重复
      Ok(Json.obj("valid" -> "false", "message" -> "任务编号已存在，请换一个编号！"))
    }
    else {
      //数据库加入duty
      dutyController.insertDuty(data.taskname,id,"8","有向网络图")
      //在用户下创建任务文件夹和结果文件夹
      val dutyDir=creatUserDir(id,data.taskname)
      val borderFile=new File(dutyDir,"border.txt")

      if(!data.text1.isEmpty){
        val file = request.body.file("table1").get
        val borderdatas = FileUtils.readFileToString(file.ref.file)
        FileUtils.writeStringToFile(borderFile, borderdatas)
      } else{
        FileUtils.writeStringToFile(borderFile, data.txdata)
      }

      val command = ""

      val execCommand = new ExecCommand
      //exec需要指定结果输出路径的时候，不指定默认本地任务路径
      execCommand.exect(command,dutyDir+"/out")
      if (execCommand.isSuccess) {
        val excel = execCommand.getOutStr
        dutydao.updateFini(id,data.taskname)
        Ok(Json.obj("excel" -> excel,"taskname"->data.taskname,"userId"->id,"path"->dutyDir))
      } else {
        dutydao.updateFailed(id,data.taskname)
        Ok(Json.obj("valid" -> "false", "message" -> execCommand.getErrStr))
      }
    }
  }


  val userDutyDir=Utils.path+"users/"

  //创建用户任务文件夹和结果文件夹
  def creatUserDir(uid:String,taskname:String)={
    new File(userDutyDir+uid+"/"+taskname).mkdir()
    new File(userDutyDir+uid+"/"+taskname+"/out").mkdir()
    userDutyDir+uid+"/"+taskname
  }



  def getFiles(id:String,taskname:String)= Action{implicit request=>
    val files = new File(Utils.path+"/users/"+id+"/"+taskname+"/out").listFiles().filter(_.getName.contains("png")).map(_.getAbsolutePath)


    Ok(Json.toJson(files))
  }

  def download(path:String) = Action{implicit request=>
    val file=new File(path)
    Ok.sendFile(file).withHeaders(
      //缓存
      CACHE_CONTROL -> "max-age=3600",
      CONTENT_DISPOSITION -> ("attachment; filename=" + file.getName()),
      CONTENT_TYPE -> "application/x-download"
    )

  }


  def downloadExamples(name:String)=Action{implicit request=>
    val file=new File(Utils.path+"files/examples/"+name)
    Ok.sendFile(file).withHeaders(
      CACHE_CONTROL->"max-age=3600",
      CONTENT_DISPOSITION -> ("attachment; filename=" + file.getName()),
      CONTENT_TYPE -> "application/x-download"
    )
  }


  def getPic(path:String) = Action{implicit request=>
    val file = new File(path)
    SendImg(file,request.headers)
  }


  def SendImg(file: File,headers:Headers) = {
    val lastModifiedStr = file.lastModified().toString
    val MimeType = "image/jpg"
    val byteArray = Files.readAllBytes(file.toPath)
    val ifModifiedSinceStr = headers.get(IF_MODIFIED_SINCE)
    if (ifModifiedSinceStr.isDefined && ifModifiedSinceStr.get == lastModifiedStr) {
      NotModified
    } else {
      Ok(byteArray).as(MimeType).withHeaders(LAST_MODIFIED -> file.lastModified().toString)
    }
  }

}
