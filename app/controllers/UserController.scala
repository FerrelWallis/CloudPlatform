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
import utils.{ExecCommand, Utils}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class UserController  @Inject()(cc: ControllerComponents,onstart:onStart,usersdao:usersDao,softdao:softDao)(implicit exec: ExecutionContext) extends AbstractController(cc) {

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
      usersdao.checkUserByEmail(form.user, form.password).map { x =>
        val valid = (x.length == 1).toString
        val id = if(x.length == 1) x.head.id.toString else ""
        Ok(Json.obj("valid" -> valid,"id" -> id))
      }
    }else{
      usersdao.checkUserByPhone(form.user, form.password).map { x =>
        val valid = (x.length == 1).toString
        val id = if(x.length == 1) x.head.id.toString else ""
        Ok(Json.obj("valid" -> valid,"id" -> id))
      }
    }
  }

  case class mailLogData(phone: String, code: String)

  val mailLogForm = Form(
    mapping(
      "phone" -> text,
      "code" -> text,

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



  def signInSuccess(path:String,id:String) = Action.async{implicit request=>
    val session = new Session
    usersdao.getById(id).map{x=>
      Redirect(routes.HomeController.home()).withNewSession.withSession(session + ("userId" -> x.id.toString) + ("userPhone"->x.phone) + ("userName"->x.name) + ("userEmail"->x.email) + ("userCompany"->x.company))
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
      val row = UsersRow(0,form.phone,form.email,form.password,form.name,form.company,"user"," ")
      val checkPhone=Await.result(usersdao.checkPhoneExist(form.phone),Duration.Inf)
      val checkEmail=Await.result(usersdao.checkEmailExist(form.email),Duration.Inf)
      if(checkPhone.length==1) {
        Ok(Json.obj("valid"->"false","message"->"该手机号已注册!"))
      }else if(checkEmail.length==1){
        Ok(Json.obj("valid"->"false","message"->"该邮箱地址已注册!"))
      }else{
        //判断验证码
        val (valid,message)=
          if(onstart.verifyMap.getOrElse(form.phone,"null")=="null") ("false","请先发送验证码！")
          else if(onstart.verifyMap.getOrElse(form.phone,"null")==form.validnumber) {
            onstart.verifyMap.remove(form.phone)
            val id=Await.result(usersdao.addUser(row),Duration.Inf)
            creatUserDir(id.toString)
            ("true","注册成功")
          } else ("false","验证码不正确！") //验证码不正确
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
        if(onstart.verifyMap.getOrElse(form.phone,"null")=="null") ("false","请先发送验证码！")
        else if(onstart.verifyMap.getOrElse(form.phone,"null")==form.validnumber) {
          onstart.verifyMap.remove(form.phone)
          Await.result(usersdao.updatePassword(form.phone,form.password1),Duration.Inf)
          ("true","修改成功")
        } else ("false","验证码不正确！") //验证码不正确
        Ok(Json.obj("valid" -> valid,"message"->message))
    }catch {
      case e : Exception => Ok(Json.obj("valid" ->"false","message" -> e.getMessage))
    }
  }

  def signout = Action{implicit request=>
    Redirect(routes.HomeController.home()).withNewSession
  }

  val userDir=Utils.path+"users/"

  //创建用户文件夹
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
      val like=Await.result(usersdao.getLike(id),Duration.Inf).split("/"+sid).mkString("")
      Await.result(usersdao.updateLike(id,like),Duration.Inf)
      Await.result(softdao.delLike(sid),Duration.Inf)
      Ok(Json.obj("valid" -> "true"))
    }catch {
      case e : Exception => Ok(Json.obj("valid" ->"false","message" -> e.getMessage))
    }
  }





}
