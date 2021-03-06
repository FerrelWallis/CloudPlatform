package controllers

import java.io.File

import dao.{softDao, usersDao}
import services.onStart
import javax.inject.{Inject, Singleton}
import models.Tables.UsersRow
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents, Session}
import utils.{ExecCommand, TableUtils, Utils}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class UserController  @Inject()(cc: ControllerComponents,onstart:onStart,utilsController: UtilsController,usersdao:usersDao,softdao:softDao)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  case class UserData(user: String, password: String)

  val userForm = Form(
    mapping(
      "user" -> text,
      "password" -> text,
    )(UserData.apply)(UserData.unapply)
  )

  def signIn = Action.async { implicit request =>
    val form = userForm.bindFromRequest.get
    if(form.user.contains("@")){
      usersdao.getByEmail(form.user).map{x=>
        val valid = form.password.equals(utilsController.getMD5String(x.pwd)).toString
        val id = if(form.password.equals(utilsController.getMD5String(x.pwd))) x.id.toString else ""
        Ok(Json.obj("valid" -> valid,"id" -> id))
      }
    }else{
      usersdao.getByPhone(form.user).map { x =>
        val valid = form.password.equals(utilsController.getMD5String(x.pwd)).toString
        val id = if(form.password.equals(utilsController.getMD5String(x.pwd))) x.id.toString else ""
        Ok(Json.obj("valid" -> valid,"id" -> id))
      }
    }
  }

  case class mailLogData(phone: String, code: String)

  val mailLogForm = Form(
    mapping(
      "phone" -> text,
      "code" -> text
    )(mailLogData.apply)(mailLogData.unapply)
  )

  def mailSignIn = Action{ implicit request =>
    val form = mailLogForm.bindFromRequest.get
    val checkphone=Await.result(usersdao.checkPhoneExist(form.phone),Duration.Inf)
    val (valid,id)=
      if(checkphone.length==1){
        println(onstart.verifyMap.toList)
        if(onstart.verifyMap.getOrElse(form.phone,"null")=="null") ("null",checkphone.head.id.toString)
        else if(onstart.verifyMap.getOrElse(form.phone,"null")==form.code) {
          onstart.verifyMap.remove(form.phone)
          ("true",checkphone.head.id.toString)
        } else ("false",checkphone.head.id.toString)
      }else{
        ("nophone","nophone")
      }
    Ok(Json.obj("valid" -> valid,"id"-> id))
  }

  case class changeDetailData(phone:String, name: String, email: String, company:String, code:String)

  val changeDetailForm = Form(
    mapping(
      "phone" -> text,
      "name" -> text,
      "email" -> text,
      "company" -> text,
      "code" -> text
    )(changeDetailData.apply)(changeDetailData.unapply)
  )

  def changeDetail = Action{ implicit request =>
    val form = changeDetailForm.bindFromRequest.get
      if(onstart.verifyMap.getOrElse(form.phone,"null")=="null") Ok(Json.obj("valid" -> "false","message"->"????????????????????????"))
      else if(onstart.verifyMap.getOrElse(form.phone,"null")==form.code) {
        onstart.verifyMap.remove(form.phone)
        Await.result(usersdao.updateDetail(form.phone,form.name,form.email,form.company),Duration.Inf)
        Ok(Json.obj("valid" -> "true","message"->"?????????","id"->request.session.get("userId").get))
      } else Ok(Json.obj("valid" -> "false","message"->"?????????????????????"))
  }

  def getDetail = Action{implicit request=>
    val id=request.session.get("userId").get
    val detial=Await.result(usersdao.getById(id),Duration.Inf)
    Ok(Json.obj("name" -> detial.name,"phone"->detial.phone,"email"->detial.email,"company"->detial.company))
  }

  case class changePhoneData(oldphone:String, oldcode: String, newphone: String, newcode:String)

  val changePhoneForm = Form(
    mapping(
      "oldphone" -> text,
      "oldcode" -> text,
      "newphone" -> text,
      "newcode" -> text
    )(changePhoneData.apply)(changePhoneData.unapply)
  )

  def changePhone = Action{ implicit request =>
    val form = changePhoneForm.bindFromRequest.get
    val checkPhone=Await.result(usersdao.checkPhoneExist(form.newphone),Duration.Inf)
    if(checkPhone.length>0) Ok(Json.obj("valid"->"false","message"->"???????????????????????????!"))
    else {
      val id=request.session.get("userId").get
      if(onstart.verifyMap.getOrElse(form.oldphone,"null")=="null") Ok(Json.obj("valid" -> "false","message"->"????????????,????????????????????????"))
      else if(onstart.verifyMap.getOrElse(form.newphone,"null")=="null") Ok(Json.obj("valid" -> "false","message"->"????????????,????????????????????????"))
      else if(onstart.verifyMap.getOrElse(form.oldphone,"null")!=form.oldcode) Ok(Json.obj("valid" -> "false","message"->"????????????????????????????????????"))
      else if(onstart.verifyMap.getOrElse(form.newphone,"null")!=form.newcode) Ok(Json.obj("valid" -> "false","message"->"????????????????????????????????????"))
      else {
        onstart.verifyMap.remove(form.oldphone)
        onstart.verifyMap.remove(form.newphone)
        Await.result(usersdao.updatePhone(id,form.newphone),Duration.Inf)
        Ok(Json.obj("valid" -> "true","message"->"?????????","id"->id))
      }
    }
  }


  def signInSuccess(path:String,id:String) = Action.async{implicit request=>
    usersdao.updateIp(id,request.remoteAddress)
    val session = new Session
    val direct=if(path.indexOf("allsoft")>=0 || path=="/CloudPlatform") "/CloudPlatform/home" else path
    usersdao.getById(id).map{x=>
      println(x.readnote)
      Redirect(direct).withNewSession.withSession(session + ("userId" -> x.id.toString) + ("userPhone"->x.phone) + ("userName"->x.name) + ("userEmail"->x.email) + ("userCompany"->x.company)+("uAuthority"->x.authority)+("noteId"->x.readnote))
    }
  }

  case class AddUserData(phone:String,name:String,email:String,company:String,password:String,password1:String,validnumber:String)

  val userForm2 = Form(
    mapping(
      "phone" -> text,
      "name" -> text,
      "email" -> text,
      "company"->text,
      "password" -> text,
      "password1"->text,
      "validnumber"->text
    )(AddUserData.apply)(AddUserData.unapply)
  )


  def addUser = Action { implicit request =>
    try{
      val form = userForm2.bindFromRequest.get
      val row = UsersRow(0,form.phone,form.email,form.password,form.name,form.company,"user"," ","","")
      val checkPhone=Await.result(usersdao.checkPhoneExist(form.phone),Duration.Inf)
      val checkEmail=Await.result(usersdao.checkEmailExist(form.email),Duration.Inf)
      if(checkPhone.length>0) {
        Ok(Json.obj("valid"->"false","message"->"?????????????????????!"))
      }else if(checkEmail.length>0){
        Ok(Json.obj("valid"->"false","message"->"????????????????????????!"))
      }else{
        //???????????????
        val (valid,message)=
          if(onstart.verifyMap.getOrElse(form.phone,"null")=="null") ("false","????????????????????????")
          else if(onstart.verifyMap.getOrElse(form.phone,"null")==form.validnumber) {
            onstart.verifyMap.remove(form.phone)
            val id=Await.result(usersdao.addUser(row),Duration.Inf)
            creatUserDir(id.toString)
            ("true","????????????")
          } else ("false","?????????????????????") //??????????????????
        Ok(Json.obj("valid"->valid,"message"->message))
      }
    }catch {
      case e:Exception=>Ok(Json.obj("valid"->"false","message"->e.getMessage))
    }
  }

  case class PasswordData(phone:String,password1:String,validnumber:String)

  val PasswordForm = Form(
    mapping(
      "phone"->text,
      "password1"-> text,
      "validnumber"->text
    )(PasswordData.apply)(PasswordData.unapply)
  )

  def changePassword = Action{implicit request=>
    try{
      val form = PasswordForm.bindFromRequest.get
      val (valid,message)=
        if(onstart.verifyMap.getOrElse(form.phone,"null")=="null") ("false","????????????????????????")
        else if(onstart.verifyMap.getOrElse(form.phone,"null")==form.validnumber) {
          onstart.verifyMap.remove(form.phone)
          Await.result(usersdao.updatePassword(form.phone,form.password1),Duration.Inf)
          ("true","????????????")
        } else ("false","?????????????????????") //??????????????????
        Ok(Json.obj("valid" -> valid,"message"->message))
    }catch {
      case e : Exception => Ok(Json.obj("valid" ->"false","message" -> e.getMessage))
    }
  }

  def signout = Action{implicit request=>
    Redirect(routes.HomeController.home()).withNewSession
  }

  val userDir=Utils.path+"users/"

  //?????????????????????
  def creatUserDir(id:String)={
    new File(userDir+id).mkdir()
  }

  def addLike(sid:String) = Action{implicit request=>
    try{
      val id=request.session.get("userId").get
      val like=Await.result(usersdao.getLike(id),Duration.Inf)+"/"+sid
      Await.result(usersdao.updateLike(id,like),Duration.Inf)
      Await.result(softdao.addLike(sid),Duration.Inf)
      Ok(Json.obj("valid" -> "true"))
    }catch {
      case e : Exception => Ok(Json.obj("valid" ->"false","message" -> e.getMessage))
    }
  }

  def deleteLike(sid:String) = Action{implicit request=>
    try{
      val id=request.session.get("userId").get
      val like=Await.result(usersdao.getLike(id),Duration.Inf)
      val newlike = if(like.indexOf("/"+sid+"/")>=0) like.split("/"+sid+"/").mkString("/")
      else if(like.substring(like.lastIndexOf("/")+1).equals(sid)) like.substring(0,like.lastIndexOf("/"))
      else like
      Await.result(usersdao.updateLike(id,newlike),Duration.Inf)
      Await.result(softdao.delLike(sid),Duration.Inf)
      Ok(Json.obj("valid" -> "true"))
    }catch {
      case e : Exception => Ok(Json.obj("valid" ->"false","message" -> e.getMessage))
    }
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

  def getAllUsers = Action { implicit request =>
    val page = pageForm.bindFromRequest.get
    val x = Await.result(usersdao.getAllUser,Duration.Inf)
    val orderX = TableUtils.dealDataByPage(x, page)
    val total = orderX.size
    val tmpX = orderX.slice(page.offset, page.offset + page.limit)
    val row = tmpX.asInstanceOf[Seq[UsersRow]].map{x=>

      val authority=if(x.authority=="user") "ic-control-inactive" else "ic-control-active"
      val control=s"<a class=' control-icon mws-ic-16 " + authority + "' onclick=\"changeAuth('"+x.id+"','"+x.authority+"')\"></a>"
      Json.obj("id" -> x.id,"name"-> x.name,"phone" -> x.phone,"email"->x.email,"company"->x.company,"ip"->x.ip,"authority"->control)
    }
    Ok(Json.obj("rows" -> row, "total" -> total))
  }


  def updateAutho(uid:String,authority:String) = Action{implicit request=>
    try{
      val autho=if(authority=="user") "admin" else "user"
      Await.result(usersdao.updateAuthority(uid,autho),Duration.Inf)
      Ok(Json.obj("valid" -> "true"))
    }catch {
      case e : Exception => Ok(Json.obj("valid" ->"false","message" -> e.getMessage))
    }
  }


}
