package controllers

import java.io.File

import dao.usersDao
import javax.inject.{Inject, Singleton}
import models.Tables.{UserRow, UsersRow}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents, Session}
import utils.{ExecCommand, Utils}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class UserController  @Inject()(cc: ControllerComponents,usersdao:usersDao)(implicit exec: ExecutionContext) extends AbstractController(cc) {

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



  def signInSuccess(path:String,id:String) = Action.async{implicit request=>
    val session = new Session
    usersdao.getById(id).map{x=>
      Redirect(path).withNewSession.withSession(session + ("userId" -> x.id.toString)+ ("userPhone"->x.phone) + ("userName"->x.name) +("userEmail"->x.email) + ("userCompany"->x.company))
    }
  }

  case class AddUserData(phone:String,name:String,email:String,company:String,password:String,password1:String)

  val userForm2 = Form(
    mapping(
      "phone" -> text,
      "name" -> text,
      "email" -> text,
      "company"->text,
      "password" -> text,
      "password1"->text
    )(AddUserData.apply)(AddUserData.unapply)
  )


  def addUser = Action { implicit request =>
    try{
      val form = userForm2.bindFromRequest.get
      val row = UsersRow(0,form.phone,form.email,form.password,form.name,form.company,"user")
      val checkPhone=Await.result(usersdao.checkPhoneExist(form.phone),Duration.Inf)
      val checkEmail=Await.result(usersdao.checkEmailExist(form.email),Duration.Inf)
      if(checkPhone.length==1) {
        Ok(Json.obj("valid"->"false","message"->"该手机号已注册!"))
      }else if(checkEmail.length==1){
        Ok(Json.obj("valid"->"false","message"->"该邮箱地址已注册!"))
      }else{
        val id=
        Await.result(usersdao.addUser(row),Duration.Inf)
        creatUserDir(id.toString)
        Ok(Json.obj("valid"->"true"))
      }
    }catch {
      case e:Exception=>Ok(Json.obj("valid"->"false","message"->e.getMessage))
    }
  }

  case class PasswordData(phone:String,password:String,password1:String,password2:String)

  val PasswordForm = Form(
    mapping(
      "phone"->text,
      "password"->text,
      "password1"-> text,
      "password2" -> text
    )(PasswordData.apply)(PasswordData.unapply)
  )

  def changePassword = Action{implicit request=>
    try{
      val form = PasswordForm.bindFromRequest.get
      val check = Await.result(usersdao.checkUserByPhone(form.phone,form.password),Duration.Inf)
      if(check.length == 1){
        Await.result(usersdao.updatePassword(form.phone,form.password1),Duration.Inf)
        Ok(Json.obj("valid" -> "true"))
      } else {
        Ok(Json.obj("valid"->"false" ,"message" -> "原密码错误!"))
      }
    }catch {
      case e : Exception => Ok(Json.obj("valid" ->"false","message" -> e.getMessage))
    }
  }

  def signout = Action{implicit request=>
    Redirect(routes.HomeController.home()).withNewSession
  }

  val userDir=Utils.path+"users\\"

  //创建用户文件夹
  def creatUserDir(id:String)={
    new File(userDir+id).mkdir()
  }

}
