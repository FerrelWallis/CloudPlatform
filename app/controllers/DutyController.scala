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
import scala.reflect.io.ZipArchive
import utils.FilesUtils.test

class DutyController @Inject()(cc: ControllerComponents,dutydao:dutyDao)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  def insertDuty(taskname:String,uid:String,sabbrename:String,sname:String,input:String,param:String,elements:String) ={
    val time=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date)

    val row = DutysRow(0,taskname,Integer.parseInt(uid),sabbrename,sname,time,"","运行中",input,param,elements)
    Await.result(dutydao.addDuty(row),Duration.Inf)
    time
  }

  def updateFini(uid:String,taskname:String) ={
    val finitime=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date)
    Await.result(dutydao.updateFini(uid,taskname,finitime),Duration.Inf)
    finitime
  }

  def getDutyByType(sabbrename:String)=Action{implicit request=>
    val uid=request.session.get("userId").get
    var running=false;
    val rows=Await.result(dutydao.getDutyByType(uid,sabbrename),Duration.Inf).map{x=>
      val status=
        if(x.status=="运行中"){
          running=true
          s"<span>运行中<img class='runningImage' src='/assets/images/timg.gif' style='width: 30px;height: 20px;float: right;padding-right: 11px;padding-top: 2px;'></span>"
        }else if(x.status=="已完成") s"<span class='success'>已完成</span>"
        else s"<span class='failed'>运行失败</span>"
      val taskname = s"<a id='taskname' href='/CloudPlatform/Mytask/teskPreview/"+sabbrename+"?taskname="+x.taskname+"' target='_blank'>" + x.taskname + "</a>"

      Json.obj("taskname"->taskname,"subtime"->x.subtime,"finitime"->x.finitime,"status"->status)
    }

    Ok(Json.obj("rows"->rows,"run"->running))
  }


  def getAllDutyById = Action { implicit request =>
    val uid=request.session.get("userId").get
    val table=Await.result(dutydao.getAllDutyById(uid),Duration.Inf)
    val row = table.map{x=>
      val taskname = s"<a id='taskname' href='/CloudPlatform/Mytask/teskPreview/"+x.sabbrename+"?taskname="+x.taskname+"' target='_blank'>" + x.taskname + "</a>"
      val sname=s"<a href='/CloudPlatform/SoftPage/"+x.sabbrename+"' target='_blank'>" + x.sname + "</a>"
      val color=if(x.status=="已完成") "success"
      else if(x.status=="运行失败") "failed"
      else if(x.status=="运行中") "running"
      val status=s"<p id='status' style='margin: 0;' class='"+color+"'>"+x.status+"</p>"
      Json.obj("taskname" -> taskname,"sname"-> sname,"submit" -> x.subtime,"finish"->x.finitime,"status"->status)
    }
    Ok(Json.obj("rows"->row))
  }




//  def taskPreview(taskname:String) = Action.async { implicit request =>
//    val id=request.session.get("userId").get
//    dutydao.getSingleDuty(id,taskname).map { x =>
//      x.sabbrename match {
//        case "PCA"=>Ok(views.html.task.RedrawPCA(x))
//        case "Boxplot"=>Ok(views.html.task.RedrawPCA(x))
//      }
//    }
//  }


  case class taskData(taskname:String)

  val taskForm=Form(
    mapping (
      "taskname"->text
    )(taskData.apply)(taskData.unapply)
  )

  def checktaskname=Action{implicit request=>
    val data=taskForm.bindFromRequest.get
    val task=Await.result(dutydao.checkTaskName(request.session.get("userId").get,data.taskname),Duration.Inf)
    val valid=if(task.isEmpty) "true"
    else "false"

    Ok(Json.obj("valid"->valid))
  }


//  def checkSoftBySname(sid:String): String ={
//    sid match {
//      case "1" => "/CloudPlatform/SoftPage/GO"
//      case "2" => "/CloudPlatform/SoftPage/KEGG"
//      case "3" => "/CloudPlatform/SoftPage/PCA"
//      case "4" => "/CloudPlatform/SoftPage/CCA"
//      case "5" => "/CloudPlatform/SoftPage/Heatmap"
//      case "6" => "/CloudPlatform/SoftPage/Boxplot"
//      case "7" => "/CloudPlatform/SoftPage/NetWeight"
//      case "8" => "/CloudPlatform/SoftPage/NetDirected"
//    }
//  }


  val userDutyDir=Utils.path+"users/"

  def deleteDuty(uid:String,taskname:String)=Action{implicit request=>
    try{
      Await.result(dutydao.deleteDuty(uid,taskname),Duration.Inf)
      FileUtils.deleteDirectory(new File(userDutyDir+uid+"/"+taskname))
      Ok(Json.obj("valid" -> "true"))
    }catch {
      case e : Exception => Ok(Json.obj("valid" ->"false","message" -> e.getMessage))
    }
  }



}
