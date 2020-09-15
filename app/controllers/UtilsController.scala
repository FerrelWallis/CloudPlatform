package controllers

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import controllers.Assets.Asset
import dao.{usersDao, utilsDao}
import models.Tables._
import services.onStart
import javax.inject.Inject
import org.apache.commons.io.FileUtils
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.{TableUtils, Utils}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, blocking}
import scala.collection.JavaConverters._

class UtilsController @Inject()(cc: ControllerComponents,onstart:onStart,utilsdao:utilsDao,usersdao:usersDao)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  def sendMSG(phone:String,where:String)=Action{
    val checkphone=Await.result(usersdao.checkPhoneExist(phone),Duration.Inf)
    if(checkphone.length==1 || where=="upphone" || where=="mailnewphone") {//注册发邮件不用理会是否exist
      val ak=Await.result(utilsdao.getAk,Duration.Inf)
      val(valid,code,responsecode)=Utils.sendMessage(phone,ak.accesskeyid,ak.accesskeysecret)
      onstart.verifyMap.put(phone,code)
      onstart.verifyTimeMap.put(phone,System.currentTimeMillis())
      println(valid)
      println(code)
      println(responsecode)
      Ok(Json.obj("valid"->valid.toString,"responsecode"->responsecode,"message"->"成功"))
    }else Ok(Json.obj("valid"->"nophone","responsecode"->"false","message"->"无效手机号或该手机号尚未注册，请先注册！"))
  }

  def getver(phone:String)=Action{
    Ok(Json.obj("verify"->onstart.verifyMap.toList))
  }


  case class NoticeData(title:String,pubtime:String,failtime:String,width:String,top:String,left:String,txdata:String)

  val NoticeForm: Form[NoticeData] =Form(
    mapping (
      "title"->text,
      "pubtime"->text,
      "failtime"->text,
      "width"->text,
      "top"->text,
      "left"->text,
      "txdata"->text
    )(NoticeData.apply)(NoticeData.unapply)
  )

  def insertNotice=Action{implicit request=>
    val data=NoticeForm.bindFromRequest.get
    val row=NoticeRow(0,data.title,data.pubtime,data.failtime,data.width,data.top,data.left,data.txdata)
    Await.result(utilsdao.addNotice(row),Duration.Inf)
    Ok(Json.obj("valid"->"true"))
  }

  def getLatestNotice=Action{implicit request=>
    val row=Await.result(utilsdao.getLateNote,Duration.Inf)
    val id=request.session.get("userId").get
    val oldReadNote= Await.result(usersdao.getById(id),Duration.Inf).readnote
    val newNote=oldReadNote+","+row.id.toString
    Await.result(usersdao.updateReadnote(id,newNote),Duration.Inf)
    //不跳转页面的时候，修改session Ok后面加withSession()
    Ok(Json.obj("title"->row.title,"pubtime"->row.pubtime,"content"->row.content,"width"->row.width,"top"->row.top,"left"->row.left)).withSession(request.session.+("noteId"->row.id.toString))
  }

  def getNoticeById(noteid:Int)=Action{implicit request=>
    val row=Await.result(utilsdao.getNoteById(noteid),Duration.Inf)
    Ok(Json.obj("title"->row.title,"pubtime"->row.pubtime,"content"->row.content,"width"->row.width,"top"->row.top,"left"->row.left))
  }

  def checkReadNote=Action{implicit request=>
    val id=request.session.get("userId").get
    val noterow=Await.result(utilsdao.getLateNote,Duration.Inf)
    val row=Await.result(usersdao.getById(id),Duration.Inf)
    val valid=if(row.readnote.indexOf(noterow.id.toString)>=0) "true" else "false"
    Ok(Json.obj("valid"->valid))
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

  def getValidNotes = Action { implicit request =>
    val page = pageForm.bindFromRequest.get
    val x = Await.result(utilsdao.getAllValidNotes,Duration.Inf)
    val orderX = TableUtils.dealDataByPage(x, page)
    val total = orderX.size
    val tmpX = orderX.slice(page.offset, page.offset + page.limit)
    val row = tmpX.asInstanceOf[Seq[NoticeRow]].map{x=>
      val title = s"<a onclick='openNotice("+x.id+")'>" + x.title + "</a>"
      Json.obj("title" -> title,"pubtime"-> x.pubtime,"failtime" -> x.failtime)
    }
    Ok(Json.obj("rows" -> row, "total" -> total))
  }

  def pdfViewer= Action {implicit request=>
    Ok(views.html.service.pdfviewer())
  }

  def previewFile(taskname:String,filename:String)= Action {implicit request=>
    Ok(views.html.service.viewFile(taskname,filename))
  }


  def readFileContent(taskname:String,filename:String) = Action { implicit request =>
    println(taskname)
    println(filename)
    val id=request.session.get("userId").get
    val file=new File(Utils.path+"users/"+id+"/"+taskname+"/out/"+filename)
    val content=FileUtils.readFileToString(file)
    Ok(Json.obj("content" -> content))
  }


}
