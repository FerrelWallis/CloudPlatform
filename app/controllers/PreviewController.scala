package controllers
import javax.inject._
import play.api.mvc._
import models.Tables.DutysRow
import dao.dutyDao
import play.api.libs.json.Json

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PreviewController @Inject()(cc: ControllerComponents,dutydao:dutyDao) extends AbstractController(cc)  {
  def prePCA(taskname:String) = Action {implicit request=>
    val id=request.session.get("userId").get
    val row=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf)
    //taskname不存在head会报错invoker.first
    if(row.length==1){
      Ok(views.html.task.RedrawPCA(row.head))
    }else{
      Redirect(routes.HomeController.mytask())
    }
  }

  def preBoxplot(taskname:String) = Action {implicit request=>
    val id=request.session.get("userId").get
    val row=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf)
    if(row.length==1){
      Ok(views.html.task.redrawBoxplot(row.head))
    }else{
      Redirect(routes.HomeController.mytask())
    }
  }

  def preHeatmap(taskname:String) = Action {implicit request=>
    val id=request.session.get("userId").get
    val row=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf)
    if(row.length==1){
      Ok(views.html.task.redrawHeat(row.head))
    }else{
      Redirect(routes.HomeController.mytask())
    }
  }

  def preCCA(taskname:String) = Action {implicit request=>
    val id=request.session.get("userId").get
    val row=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf)
    if(row.length==1){
      Ok(views.html.task.redrawCCA(row.head))
    }else{
      Redirect(routes.HomeController.mytask())
    }
  }

  def preNet(taskname:String) = Action {implicit request=>
    val id=request.session.get("userId").get
    val row=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf)
    if(row.length==1){
      Ok(views.html.task.redrawNet(row.head))
    }else{
      Redirect(routes.HomeController.mytask())
    }
  }

  def preGo(taskname:String) = Action {implicit request=>
    val id=request.session.get("userId").get
    val row=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf)

    if(row.length==1){
      Ok(views.html.task.redrawGokegg(row.head))
    }else{
      Redirect(routes.HomeController.mytask())
    }
  }

  def preKegg(taskname:String) = Action {implicit request=>
    val id=request.session.get("userId").get
    val row=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf)
    if(row.length==1){
      Ok(views.html.task.redrawKegg(row.head))
    }else{
      Redirect(routes.HomeController.mytask())
    }
  }

  def preIGC(taskname:String) = Action {implicit request=>
    val id=request.session.get("userId").get
    val row=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf)
    if(row.length==1){
      Ok(views.html.task.redrawIGC(row.head))
    }else{
      Redirect(routes.HomeController.mytask())
    }
  }

  def preTax(taskname:String) = Action {implicit request=>
    val id=request.session.get("userId").get
    val row=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf)
    if(row.length==1){
      Ok(views.html.task.redrawDataOnly(row.head))
    }else{
      Redirect(routes.HomeController.mytask())
    }
  }


}
