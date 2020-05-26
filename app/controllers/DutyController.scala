package controllers

import java.io.File
import java.lang.reflect.Field
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date

import dao.dutyDao
import models.Tables.DutysRow
import play.api.data.Form
import play.api.data.Forms.{mapping, number, optional, text}
import utils.{ExecCommand, TableUtils, Utils}
import javax.inject.Inject
import org.apache.commons.io.FileUtils
import play.api.libs.json.Json
import play.api.mvc._
import utils.TableUtils.isDouble

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

class DutyController @Inject()(cc: ControllerComponents,dutydao:dutyDao)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  def insertDuty(taskname:String,uid:String,sid:String,sname:String) ={
    val time=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date)
    val row = DutysRow(0,taskname,Integer.parseInt(uid),sid,sname,time,"","运行中")
    Await.result(dutydao.addDuty(row),Duration.Inf)
  }

  def getAllDuty(uid:String)={
    Await.result(dutydao.getAllDutyById(uid),Duration.Inf)
  }

  case class PageData(limit: Int, offset: Int, order: String, search: Option[String], sort: Option[String])

  val pageForm = Form(
    mapping(
      "limit" -> number,
      "offset" -> number,
      "order" -> text,
      "search" -> optional(text),
      "sort" -> optional(text)
    )(PageData.apply)(PageData.unapply)
  )

  def getAllDutyById(uid:String) = Action { implicit request =>
    val table=Await.result(dutydao.getAllDutyById(uid),Duration.Inf)
    val row = table.map{x=>
      val taskname = s"<a id='taskname' href='/CloudPlatform/MyTasks/taskPreview?uid="+uid+"&taskname="+x.taskname+"' target='_blank'>" + x.taskname + "</a>"
      val sname=s"<a href="+checkSoftBySname(x.sid)+" target='_blank'>" + x.sname + "</a>"
      Json.obj("taskname" -> taskname,"sname"-> sname,"submit" -> x.subtime,"finish"->x.finitime,"status"->x.status)
    }
    Ok(Json.obj("rows"->row))
  }

  def taskPreview(uid: String,taskname:String) = Action.async { implicit request =>
    dutydao.getSingleDuty(uid,taskname).map { x =>
      val in=new File(Utils.path+"users/"+uid+"/"+taskname,"input.txt")
      val input = if(in.exists()){
        FileUtils.readFileToString(in).replaceAll(" "," / ")
      }else{
        "无"
      }
      val pa=new File(Utils.path+"users/"+uid+"/"+taskname,"param.txt")
      val param = if(pa.exists()){
        FileUtils.readFileToString(pa)
      }else {
        "无"
      }
      Ok(views.html.task.preview(taskname,x.sname,input,param,x.subtime,x.finitime,x.status))
    }

  }


  def checkSoftBySname(sid:String): String ={
    sid match {
      case "1" => "/CloudPlatform/SoftPage/GO"
      case "2" => "/CloudPlatform/SoftPage/KEGG"
      case "3" => "/CloudPlatform/SoftPage/PCA"
      case "4" => "/CloudPlatform/SoftPage/CCA"
      case "5" => "/CloudPlatform/SoftPage/Heatmap"
      case "6" => "/CloudPlatform/SoftPage/Boxplot"
      case "7" => "/CloudPlatform/SoftPage/NetWeight"
      case "8" => "/CloudPlatform/SoftPage/NetDirected"
    }
  }




  def deleteDuty(uid:String,taskname:String)=Action{implicit request=>
    try{
      Await.result(dutydao.deleteDuty(uid,taskname),Duration.Inf)
      deleteUserDir(new File(userDutyDir+uid+"/"+taskname))
      Ok(Json.obj("valid" -> "true"))
    }catch {
      case e : Exception => Ok(Json.obj("valid" ->"false","message" -> e.getMessage))
    }
  }


  val userDutyDir=Utils.path+"users/"

  //文件夹必须为空才能删除，因此需判断是否为空,不为空要读取内容，判断是文件还是文件夹，文件夹继续看内容，文件直接删除
  def deleteUserDir(file: File):Boolean={
    if(file.exists()){
      val listFile=file.listFiles()
      listFile.foreach{x=>
        if(x.isFile) x.delete()
        else {
          deleteUserDir(x)
        }
      }
      file.delete()
    }else{
      System.out.println("路径不存在")
      false
    }

  }

}
