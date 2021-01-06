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

class RChartController @Inject()(cc: ControllerComponents, dutydao: dutyDao, rservice: RService, dutyController: DutyController)(implicit exec: ExecutionContext) extends AbstractController(cc) with MyStringTool {

  val userDutyDir: String =Utils.path+"users/"

  case class TaxFunData(taskname:String)

  val TaxFunForm: Form[TaxFunData] =Form(
    mapping (
      "taskname"->text
    )(TaxFunData.apply)(TaxFunData.unapply)
  )

  //table Transposition
  def doTableTrans=Action(parse.multipartFormData){implicit request=>
    val data=TaxFunForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=rservice.creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val file1=request.body.file("table1").get
    val tableFile=new File(dutyDir,file1.filename)
    //矩阵文件读取写入任务文件下table.txt
    file1.ref.copyTo(tableFile)
    val input=file1.filename

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"TT","表格转置",input,"/","")

    Future{
      val command = "Rscript "+Utils.path+"R/table_t/table-t.R -i "+ tableFile.getAbsolutePath +
        " -o " +dutyDir+"/out/trans.xls"

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      //exec需要指定结果输出路径的时候，不指定默认本地任务路径
      execCommand.exect(command,dutyDir+"/temp")

      if (execCommand.isSuccess) {
        val finish = dutyController.updateFini(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\nFinish Time:"+finish+"\n\n运行成功！")
        rservice.creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }


  //Merge 2 table
  case class Merge2TData(taskname:String, b1:String, b2:String, ct:String, sn:String, mysn:String)

  val Merge2TForm: Form[Merge2TData] =Form(
    mapping (
      "taskname"->text,
      "b1"->text,
      "b2"->text,
      "ct"->text,
      "sn"->text,
      "mysn"->text,
    )(Merge2TData.apply)(Merge2TData.unapply)
  )

  def doMerge2Table=Action(parse.multipartFormData){implicit request=>
    val data=Merge2TForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=rservice.creatUserDir(id,data.taskname)
    //在用户下创建任务文件夹和结果文件夹
    val file1=request.body.file("table1").get
    val file2=request.body.file("table2").get
    val tableFile1=new File(dutyDir,file1.filename)
    val tableFile2=new File(dutyDir,file2.filename)
    //矩阵文件读取写入任务文件下table.txt
    file1.ref.copyTo(tableFile1)
    file2.ref.copyTo(tableFile2)
    val input=file1.filename+"/"+file2.filename

    val param="合并参照列 (矩阵1 - 矩阵2) ：第" + data.b1 + "列 : 第" + data.b2 + "列/设置合并方式：" + data.ct +
      "/缺省值设置：" + (if(data.sn=="default") data.mysn else if(data.sn=="N/A") "NA" else data.sn)

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"MTT","两表格合并",input,param,"")

    Future{
      val sn=if(data.sn=="default") " -sn \"" + data.mysn + "\"" else " -sn \"" + data.sn + "\""

      val command = "Rscript " + Utils.path + "R/table_two/table_two2.R -i1 " + tableFile1.getAbsolutePath +
        " -i2 " + tableFile2.getAbsolutePath + " -o " + dutyDir + "/out/merge2table.xls -b " + data.b1 +
        ":" + data.b2 + " -ct " + data.ct + sn

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      //exec需要指定结果输出路径的时候，不指定默认本地任务路径
      execCommand.exect(command,dutyDir+"/temp")

      if (execCommand.isSuccess) {
        val finish = dutyController.updateFini(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\nFinish Time:"+finish+"\n\n运行成功！")
        rservice.creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }


  //Merge Multiple table
  case class MergeMTData(taskname:String, b:String, ct:String, sn:String, mysn:String)

  val MergeMTForm: Form[MergeMTData] =Form(
    mapping (
      "taskname"->text,
      "b"->text,
      "ct"->text,
      "sn"->text,
      "mysn"->text,
    )(MergeMTData.apply)(MergeMTData.unapply)
  )

  def doMergeMulTable(filenum:Int)=Action(parse.multipartFormData){implicit request=>
    val data=MergeMTForm.bindFromRequest.get
    val id=request.session.get("userId").get
    val dutyDir=rservice.creatUserDir(id,data.taskname)
    val (files,input)=(1 to filenum).map{i=>
      val file=request.body.file("table"+i).get
      val tableFile=new File(dutyDir,file.filename)
      file.ref.copyTo(tableFile)
      (tableFile.getAbsolutePath,file.filename)
    }.unzip

    //在用户下创建任务文件夹和结果文件夹
    val param="合并参照列：" + data.b + "/设置合并方式：" + data.ct + "/缺省值设置：" +
      (if(data.sn=="default") data.mysn else if(data.sn=="N/A") "NA" else data.sn)

    //数据库加入duty（运行中）
    val start=dutyController.insertDuty(data.taskname,id,"MMT","多表格合并",input.mkString("/"),param,"")

    Future{
      val sn=if(data.sn=="default") " -sn \"" + data.mysn + "\"" else " -sn \"" + data.sn + "\""

      val b = " -b " + (if(data.b.split(":").length==filenum) data.b else (1 to filenum).map{x=>data.b}.mkString(":"))
      println(b)

      val command = "Rscript " + Utils.path + "R/table_more/table-more.R -i " + files.mkString(";") +
        " -o " + dutyDir + "/out/mergeTables.xls" + " -ct " + data.ct + sn + b

      println(command)

      FileUtils.writeStringToFile(new File(s"$dutyDir/temp/run.sh"),command)
      val execCommand = new ExecCommand
      //exec需要指定结果输出路径的时候，不指定默认本地任务路径
      execCommand.exect(command,dutyDir+"/temp")

      if (execCommand.isSuccess) {
        val finish = dutyController.updateFini(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\nFinish Time:"+finish+"\n\n运行成功！")
        rservice.creatZip(dutyDir)
      } else {
        dutydao.updateFailed(id,data.taskname)
        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"Start Time:"+start+"\n\n错误信息：\n"+execCommand.getErrStr+"\n\n")
      }
    }
    Ok(Json.obj("valid" -> "运行中！"))
  }



}
