package controllers
import javax.inject._
import play.api.mvc._
import models.Tables.DutysRow
import dao.dutyDao

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PreviewController @Inject()(cc: ControllerComponents,dutydao:dutyDao) extends AbstractController(cc)  {
  def prePCA(taskname:String) = Action {implicit request=>
    val id=request.session.get("userId").get
    val row=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf)
    Ok(views.html.task.RedrawPCA(row))
  }

  def preBoxplot(taskname:String) = Action {implicit request=>
    val id=request.session.get("userId").get
    val row=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf)
    Ok(views.html.task.redrawBoxplot(row))
  }

  def preHeatmap(taskname:String) = Action {implicit request=>
    val id=request.session.get("userId").get
    val row=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf)
    Ok(views.html.task.redrawHeat(row))
  }

  def preCCA(taskname:String) = Action {implicit request=>
    val id=request.session.get("userId").get
    val row=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf)
    Ok(views.html.task.redrawCCA(row))
  }

  def preNet(taskname:String) = Action {implicit request=>
    val id=request.session.get("userId").get
    val row=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf)
    Ok(views.html.task.redrawNet(row))
  }

  def preGo(taskname:String) = Action {implicit request=>
    val id=request.session.get("userId").get
    val row=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf)
    Ok(views.html.task.redrawGokegg(row))
  }

  def preKegg(taskname:String) = Action {implicit request=>
    val id=request.session.get("userId").get
    val row=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf)
    Ok(views.html.task.redrawKegg(row))
  }


}
