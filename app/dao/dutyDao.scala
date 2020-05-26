package dao

import java.text.SimpleDateFormat
import java.util.Date

import javax.inject.Inject
import models.Tables.{Dutys, DutysRow, Users}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class dutyDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit exec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile]{


  import profile.api._

  def addDuty(row:DutysRow) : Future[Unit] = {
    db.run(Dutys += row).map(_=>())
  }

  def updateFini(uid:String,taskname:String): Future[Unit]={
    val finitime=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date)
    db.run(Dutys.filter(_.uid ===Integer.parseInt(uid)).filter(_.taskname === taskname).map(x=>(x.finitime,x.status)).update(finitime,"已完成")).map(_=>())
  }

  def checkTaskName(uid:String,taskname:String):Future[Seq[DutysRow]]={
    db.run(Dutys.filter(_.uid===Integer.parseInt(uid)).filter(_.taskname===taskname).result)
  }

  def updateFailed(uid:String,taskname:String):Future[Unit]={
    db.run(Dutys.filter(_.uid ===Integer.parseInt(uid)).filter(_.taskname === taskname).map(_.status).update("运行失败")).map(_=>())
  }

  def getAllDutyById(uid:String):Future[Seq[DutysRow]]={
    db.run(Dutys.filter(_.uid===Integer.parseInt(uid)).result)
  }

  def getSingleDuty(uid:String,taskname:String):Future[DutysRow]={
    db.run(Dutys.filter(_.uid===Integer.parseInt(uid)).filter(_.taskname===taskname).result.head)
  }

  def deleteDuty(uid:String,taskname:String):Future[Unit]={
    db.run(Dutys.filter(_.uid===Integer.parseInt(uid)).filter(_.taskname===taskname).delete).map(_=>())
  }

}
