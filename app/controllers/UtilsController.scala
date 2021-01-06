package controllers

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import controllers.Assets.Asset
import dao.{feedbackDao, runningDao, softDao, usersDao, utilsDao}
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

class UtilsController @Inject()(cc: ControllerComponents,onstart:onStart,utilsdao:utilsDao,usersdao:usersDao,feedbackDao:feedbackDao,runningDao:runningDao, softDao:softDao)(implicit exec: ExecutionContext) extends AbstractController(cc) {


  //发送短信
  def sendMSG(phone:String,where:String)=Action{
    val checkphone=Await.result(usersdao.checkPhoneExist(phone),Duration.Inf)
    if(checkphone.length==1 || where=="upphone" || where=="mailnewphone") {//注册发邮件不用理会是否exist
      val ak=Await.result(utilsdao.getAk,Duration.Inf)
      val(valid,code,responsecode)=Utils.sendMessage(phone,ak.accesskeyid,ak.accesskeysecret)
      onstart.verifyMap.put(phone,code)
      onstart.verifyTimeMap.put(phone,System.currentTimeMillis())
      Ok(Json.obj("valid"->valid.toString,"responsecode"->responsecode,"message"->"成功"))
    }else Ok(Json.obj("valid"->"nophone","responsecode"->"false","message"->"无效手机号或该手机号尚未注册，请先注册！"))
  }

  def getver(phone:String)=Action{
    Ok(Json.obj("verify"->onstart.verifyMap.toList))
  }

  //轮播通知
  def getAllRunning = Action { implicit request =>
    val row=Await.result(runningDao.getRunning,Duration.Inf).map{row=>
      (row.id,row.content)
    }
    Ok(Json.obj("rows" -> row))
  }

  case class RunningData(runnote1:String,runnote2:String,runnote3:String,runnote4:String,runnote5:String,runnote6:String)

  val RunningForm: Form[RunningData] =Form(
    mapping (
      "runnote1"->text,
      "runnote2"->text,
      "runnote3"->text,
      "runnote4"->text,
      "runnote5"->text,
      "runnote6"->text
    )(RunningData.apply)(RunningData.unapply)
  )

  def updateRunning = Action { implicit request =>
    val data=RunningForm.bindFromRequest.get
    Await.result(runningDao.updateStatus(1,data.runnote1),Duration.Inf)
    Await.result(runningDao.updateStatus(2,data.runnote2),Duration.Inf)
    Await.result(runningDao.updateStatus(3,data.runnote3),Duration.Inf)
    Await.result(runningDao.updateStatus(4,data.runnote4),Duration.Inf)
    Await.result(runningDao.updateStatus(5,data.runnote5),Duration.Inf)
    Await.result(runningDao.updateStatus(6,data.runnote6),Duration.Inf)
    Ok(Json.obj("valid" -> "true"))
  }


  //问题反馈模块
  case class FeedData(title:String,txdata:String)

  val FeedForm: Form[FeedData] =Form(
    mapping (
      "title"->text,
      "txdata"->text
    )(FeedData.apply)(FeedData.unapply)
  )

  def addFeedback=Action(parse.multipartFormData) {implicit request=>
    val data=FeedForm.bindFromRequest.get
    val uid=request.session.get("userId").get
    val time=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
    val row=FeedbackRow(0,uid.toInt,time,data.title,data.txdata,1,"未读")
    Await.result(feedbackDao.addFeedback(row),Duration.Inf)
    Ok(Json.obj("valid"->"true"))
  }

  def getFeedsByUid = Action { implicit request =>
    val page = pageForm.bindFromRequest.get
    val uid=request.session.get("userId").get
    val x = Await.result(feedbackDao.getFeedsByUid(uid),Duration.Inf)
    val orderX = TableUtils.dealDataByPage(x, page)
    val total = orderX.size
    val tmpX = orderX.slice(page.offset, page.offset + page.limit)
    val row = tmpX.asInstanceOf[Seq[FeedbackRow]].map{x=>
      val fid=x.fid
      val content=s"<a onclick='openFeedpage($fid)' style='color: #2a6496;'>查看详情</a>"
      Json.obj("subtime" -> x.subtime,"title"-> x.title,"content" -> content)
    }
    Ok(Json.obj("rows" -> row, "total" -> total))
  }

  def getFeedsByFid(fid:String,adpage:Boolean)=Action{implicit request=>
    val auth=request.session.get("uAuthority").get
    if(adpage && auth=="admin") Await.result(feedbackDao.updateProcess(fid,"已读"),Duration.Inf)
    val row=Await.result(feedbackDao.getFeedsByFid(fid),Duration.Inf)
    Ok(Json.obj("title"->row.title,"subtime"->row.subtime,"content"->row.content))
  }

  def deleteFeeds(ids:String)=Action{implicit request=>
    val uid=request.session.get("userId").get
    try{
      ids.split(",").filter(_!="").foreach{x=>
        Await.result(feedbackDao.updateStatus(x),Duration.Inf)
      }
      Ok(Json.obj("valid" -> "true"))
    }catch {
      case e : Exception => Ok(Json.obj("valid" ->"false","message" -> e.getMessage))
    }
  }

  def realDeleteFeeds(ids:String)=Action{implicit request=>
    val uid=request.session.get("userId").get
    try{
      ids.split(",").filter(_!="").foreach{x=>
        Await.result(feedbackDao.deleteFeedback(x),Duration.Inf)
      }
      Ok(Json.obj("valid" -> "true"))
    }catch {
      case e : Exception => Ok(Json.obj("valid" ->"false","message" -> e.getMessage))
    }
  }

  def getAllFeeds = Action { implicit request =>
    val page = pageForm.bindFromRequest.get
    val x = Await.result(feedbackDao.getAllFeeds,Duration.Inf)
    val orderX = TableUtils.dealDataByPage(x, page)
    val total = orderX.size
    val tmpX = orderX.slice(page.offset, page.offset + page.limit)
    val row = tmpX.asInstanceOf[Seq[(Int,String,String,String,String,String)]].map{x=>
      val fid = x._1
      val control = "<a onclick='openFeedpage("+fid+",\""+x._5+"\",\""+x._6+"\")' style='color: #2a6496;'>查看详情</a>"
      val process = if(x._4=="已读") "<p style='color: cadetblue; margin: 0px;'>"+x._4+"</p>"
      else "<p style='color: #bf4064; margin: 0px;'>"+x._4+"</p>"
      Json.obj("subtime" -> x._2,"title" -> x._3,"process"->process,"control"->control,"name"->x._5,"phone"->x._6)
    }
    Ok(Json.obj("rows" -> row, "total" -> total))
  }




  //公告模块
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


  //bootstrap的table模块
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

  //结果文件预览模块
  def pdfViewer= Action {implicit request=>
    Ok(views.html.service.pdfviewer())
  }

  def previewFile(taskname:String,filename:String,filetype:String)= Action {implicit request=>
    Ok(views.html.service.viewFile(taskname,filename,filetype))
  }


  def readFileContent(taskname:String,filename:String) = Action { implicit request =>
    val id=request.session.get("userId").get
    val file=new File(Utils.path+"users/"+id+"/"+taskname+"/out/"+filename)
    val content=FileUtils.readFileToString(file)
    Ok(Json.obj("content" -> content))
  }

  def getFilePath(taskname:String,filename:String)= Action {implicit request=>
    val id=request.session.get("userId").get
    val pdfUrl=Utils.path+"/users/"+id+"/"+taskname+"/out/"+filename
    Ok(Json.obj("pdfUrl"->pdfUrl))
  }


  //md5加密
  import java.math.BigInteger
  import java.security.MessageDigest
  def getMD5String(str: String): String = try { // 生成一个MD5加密计算摘要
    val md = MessageDigest.getInstance("MD5")
    // 计算md5函数
    md.update(str.getBytes)
    // digest()最后确定返回md5 hash值，返回值为8位字符串。因为md5 hash值是16位的hex值，实际上就是8位的字符
    // BigInteger函数则将8位的字符串转换成16位hex值，用字符串来表示；得到字符串形式的hash值
    //一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方）
    new BigInteger(1, md.digest).toString(16)
  } catch {
    case e: Exception =>
      e.printStackTrace()
      null
  }


  val classcn = Seq(("功能分析", "function"), ("聚类分析", "cluster"), ("基础绘图", "paint"), ("统计检验与差异分析", "check"), ("菌群功能分析", "predict"), ("序列处理", "sequence"), ("注释工具", "annotation"), ("表格与表格转换", "chart"))

  //manager softFreq
  def softFreq = Action { implicit request =>
    val summary = Await.result(softDao.getAllSoft, Duration.Inf).map(_.likefreq).sum

    val (softpart, classpart) = classcn.map{c =>
      val softs = Await.result(softDao.getSoftByTypes(c._2), Duration.Inf)
      val y = (softs.map(_.likefreq).sum.toDouble / summary * 100)
      val drilldown = if(softs.length == 0) null else c._1
      val data = softs.map{s =>(s.sname, (s.likefreq.toDouble / summary * 100))}

      (Json.obj("data" -> data, "id" -> c._1, "name" -> c._1),
        Json.obj("drilldown" -> drilldown, "name" -> c._1, "y" -> y))
    }.unzip

    Ok(Json.obj("classpart"-> classpart, "softpart" -> softpart))
  }



}
