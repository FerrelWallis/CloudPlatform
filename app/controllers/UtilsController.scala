package controllers

import dao.{usersDao, utilsDao}
import services.onStart
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import utils.Utils

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class UtilsController @Inject()(cc: ControllerComponents,onstart:onStart,utilsdao:utilsDao,usersdao:usersDao)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  def sendMSG(phone:String,where:String)=Action{
    println(111)
    val checkphone=Await.result(usersdao.checkPhoneExist(phone),Duration.Inf)
    if(checkphone.length==1 || where=="upphone") {//注册发邮件不用理会是否exist
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
}
