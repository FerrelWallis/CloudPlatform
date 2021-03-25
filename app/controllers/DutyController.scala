package controllers

import java.io.File
import java.lang.reflect.Field
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.{Calendar, Date, Locale}

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


case class PageData(limit: Int, offset: Int, order: String, search: Option[String], sort: Option[String])
class DutyController @Inject()(cc: ControllerComponents, dutydao:dutyDao)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  def insertDuty(taskname:String,uid:String,sabbrename:String,sname:String,input:String,param:String,elements:String) ={
//    val date=Calendar.getInstance(Locale.CHINA).getTime
    val time=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
    val row = DutysRow(0,taskname,Integer.parseInt(uid),sabbrename,sname,time,"","运行中",input,param,elements)
    Await.result(dutydao.addDuty(row),Duration.Inf)
    time
  }

  def updateFini(uid:String,taskname:String) ={
//    val date=Calendar.getInstance(Locale.CHINA).getTime
    val finitime=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
    Await.result(dutydao.updateFini(uid,taskname,finitime),Duration.Inf)
    finitime
  }

  def updateFinish(uid:String,taskname:String,state:Int,sname:String,input:String,param:String,elements:String) ={
    val finitime=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
    if(state == 2) {
      Await.result(dutydao.updateFailure(uid,taskname,sname,input,param,elements),Duration.Inf)
    } else {
      Await.result(dutydao.updateFinish(uid,taskname,finitime,sname,input,param,elements),Duration.Inf)
    }
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
      val taskname = s"<a id='taskname' href='/CloudPlatform/Mytask/taskPreview/"+sabbrename+"?abbre="+sabbrename+"&taskname="+x.taskname+"' target='_blank'>" + x.taskname + "</a>"

      Json.obj("taskname"->taskname,"finitime"->x.finitime,"status"->status)
    }
    Ok(Json.obj("rows"->rows,"run"->running))
  }


  def getAllDutyById = Action { implicit request =>
    val uid=request.session.get("userId").get
    val table=Await.result(dutydao.getAllDutyById(uid),Duration.Inf)
    val row = table.map{x=>
      val taskname = s"<a id='taskname' href='/CloudPlatform/Mytask/taskPreview/"+x.sabbrename+"?abbre="+x.sabbrename+"&taskname="+x.taskname+"' target='_blank'>" + x.taskname + "</a>"
      val sname=s"<a href='/CloudPlatform/SoftPage/"+x.sabbrename+"' target='_blank'>" + x.sname + "</a>"
      val color=if(x.status=="已完成") "success"
      else if(x.status=="运行失败") "failed"
      else if(x.status=="运行中") "running"
      val status=s"<p id='status' style='margin: 0;' class='"+color+"'>"+x.status+"</p>"
      Json.obj("taskname" -> taskname,"sname"-> sname,"submit" -> x.subtime,"finish"->x.finitime,"status"->status)
    }
    Ok(Json.obj("rows"->row))
  }

  val pageForm = Form(
    mapping(
      "limit" -> number,
      "offset" -> number,
      "order" -> text,
      "search" -> optional(text),
      "sort" -> optional(text)
    )(PageData.apply)(PageData.unapply)
  )

  def getAllDutys = Action { implicit request =>
    val uid=request.session.get("userId").get

    val page = pageForm.bindFromRequest.get
    val x = Await.result(dutydao.getAllDutyById(uid),Duration.Inf)
    val orderX = TableUtils.dealDataByPage(x, page)
    val total = orderX.size
    val tmpX = orderX.slice(page.offset, page.offset + page.limit)
    val row = tmpX.asInstanceOf[Seq[DutysRow]].map{x=>
      val taskname = s"<a id='taskname' href='/CloudPlatform/Mytask/taskPreview/"+x.sabbrename+"?taskname="+x.taskname+"' target='_blank'>" + x.taskname + "</a>"
      val sname=s"<a href='/CloudPlatform/SoftPage/"+x.sabbrename+"' target='_blank'>" + x.sname + "</a>"
      val color=if(x.status=="已完成") "success"
      else if(x.status=="运行失败") "failed"
      else if(x.status=="运行中") "running"
      val status=s"<p id='status' style='margin: 0;' class='"+color+"'>"+x.status+"</p>"
      val manage=if(new File(Utils.path+"users/"+uid+"/"+x.taskname+"/log.txt").length()==0) "ic-logfile-unable" else "ic-logfile"
      val control=s"<a class='control-icon mws-ic-16 ic-delete2' onclick=deltask('"+ x.taskname +"')></a><a class='control-icon mws-ic-16 ic-cloud-download-1' onclick=downtask('"+ x.taskname +"','"+ x.status +"')></a><a class='control-icon mws-ic-16 "+manage+"' onclick=showlog('"+ x.taskname +"')></a>"
      Json.obj("taskname" -> taskname,"sname"-> sname,"subtime" -> x.subtime,"finitime"->x.finitime,"status"->status,"control"->control)
    }

    Ok(Json.obj("rows" -> row, "total" -> total))
  }


  def getDuties = Action { implicit request =>
    val page = pageForm.bindFromRequest.get
    val x = Await.result(dutydao.getDuties,Duration.Inf)
    val orderX = TableUtils.dealDataByPage(x, page)
    val total = orderX.size
    val tmpX = orderX.slice(page.offset, page.offset + page.limit)
    val row = tmpX.asInstanceOf[Seq[(String,String,String,String,String,Int)]].map{x=>
      val color=if(x._5=="已完成") "success"
      else if(x._5=="运行失败") "failed"
      else if(x._5=="运行中") "running"
      val status=s"<p id='status' style='margin: 0;' class='"+color+"'>"+x._5+"</p>"
      val manage=if(new File(Utils.path+"users/"+x._6+"/"+x._3+"/log.txt").length()==0) "ic-logfile-unable" else "ic-logfile"
      val control=s"<a class='control-icon mws-ic-16 ic-cloud-download-1' onclick=downduty('"+ x._3 +"','"+ x._6 +"')></a><a class='control-icon mws-ic-16 "+manage+"' onclick=showlog('"+ x._3 +"'," + x._6 + ")></a>"
      Json.obj("subtime" -> x._1,"taskname" -> x._3,"sname"-> x._4,"username"->x._2,"status"->status,"control"->control)
    }
    Ok(Json.obj("rows" -> row, "total" -> total))
  }


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


  val userDutyDir=Utils.path+"users/"

  def deleteDuty(taskname:String)=Action{implicit request=>
    val uid=request.session.get("userId").get
    try{
      Await.result(dutydao.deleteDuty(uid,taskname),Duration.Inf)
      FileUtils.deleteDirectory(new File(userDutyDir+uid+"/"+taskname))
      Ok(Json.obj("valid" -> "true"))
    }catch {
      case e : Exception => Ok(Json.obj("valid" ->"false","message" -> e.getMessage))
    }
  }

  def showLog(taskname:String)=Action{implicit request=>
    val uid=request.session.get("userId").get
    try{
      val file=new File(userDutyDir+uid+"/"+taskname+"/log.txt")
      val (canmanage,content)=if(file.length()==0) (0,"") else (1,FileUtils.readFileToString(file))
      Ok(Json.obj("content" -> content,"canmanage"->canmanage))
    }catch {
      case e : Exception => Ok(Json.obj("valid" ->"false","message" -> e.getMessage))
    }
  }

  def showLogByUid(taskname:String, uid:String)=Action{implicit request=>
    try{
//      println(userDutyDir+uid+"/"+taskname+"/log.txt")
      val file=new File(userDutyDir+uid+"/"+taskname+"/log.txt")
      val (canmanage,content)=if(file.length()==0) (0,"") else (1,FileUtils.readFileToString(file))
      Ok(Json.obj("content" -> content,"canmanage"->canmanage))
    }catch {
      case e : Exception => Ok(Json.obj("valid" ->"false","message" -> e.getMessage))
    }
  }

  def deleteDutys(ids:String)=Action{implicit request=>
    val uid=request.session.get("userId").get
    try{
      ids.split(",").filter(_!="").foreach{x=>
        Await.result(dutydao.deleteDuty(uid,x),Duration.Inf)
        FileUtils.deleteDirectory(new File(userDutyDir+uid+"/"+x))
      }
      Ok(Json.obj("valid" -> "true"))
    }catch {
      case e : Exception => Ok(Json.obj("valid" ->"false","message" -> e.getMessage))
    }
  }

}
