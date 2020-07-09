package controllers

import dao.utilsDao
import services.onStart
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import utils.Utils

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class UtilsController @Inject()(cc: ControllerComponents,onstart:onStart,utilsdao:utilsDao)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  def sendMSG(phone:String)=Action{
    val ak=Await.result(utilsdao.getAk,Duration.Inf)
    val(valid,code,responsecode)=Utils.sendMessage(phone,ak.accesskeyid,ak.accesskeysecret)
    onstart.verifyMap.put(phone,code)
    onstart.verifyTimeMap.put(phone,System.currentTimeMillis())
    println(onstart.verifyMap.toList)
    println(onstart.verifyTimeMap.toList)
    Ok(Json.obj("valid"->valid.toString,"responsecode"->responsecode))
  }

  def getver(phone:String)=Action{
    Ok(Json.obj("verify"->onstart.verifyMap.toList))
  }
}
