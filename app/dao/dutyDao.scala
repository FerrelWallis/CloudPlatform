package dao

import java.text.SimpleDateFormat
import java.util.Date

import javax.inject.Inject
import models.Tables.{Dutys, DutysRow, Users}
import net.sf.ehcache.search.aggregator.Count
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class dutyDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit exec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile]{


  import profile.api._

  def addDuty(row:DutysRow) : Future[Unit] = {
    db.run(Dutys += row).map(_=>())
  }

  def updateElements(uid:String,taskname:String,elements:String): Future[Unit]={
    db.run(Dutys.filter(_.uid ===Integer.parseInt(uid)).filter(_.taskname === taskname).map(_.elements).update(elements)).map(_=>())
  }

  def updateFini(uid:String,taskname:String,finitime:String): Future[Unit]={
    db.run(Dutys.filter(_.uid ===Integer.parseInt(uid)).filter(_.taskname === taskname).map(x=>(x.finitime,x.status)).update(finitime,"已完成")).map(_=>())
  }

  def updateFinish(uid:String,taskname:String,finitime:String,sname:String,input:String,param:String,elements:String): Future[Unit]={
    db.run(Dutys.filter(_.uid ===Integer.parseInt(uid)).filter(_.taskname === taskname).map{row =>
      (row.sname,row.input,row.param,row.elements,row.finitime,row.status)
    }.update((sname,input,param,elements,finitime,"已完成"))).map(_=>())
  }

  def checkTaskName(uid:String,taskname:String):Future[Seq[DutysRow]]={
    db.run(Dutys.filter(_.uid===Integer.parseInt(uid)).filter(_.taskname===taskname).result)
  }

  def updateFailed(uid:String,taskname:String):Future[Unit]={
    db.run(Dutys.filter(_.uid ===Integer.parseInt(uid)).filter(_.taskname === taskname).map(_.status).update("运行失败")).map(_=>())
  }

  def updateFailure(uid:String,taskname:String,sname:String,input:String,param:String,elements:String):Future[Unit]={
    db.run(Dutys.filter(_.uid ===Integer.parseInt(uid)).filter(_.taskname === taskname).map{row =>
      (row.sname,row.input,row.param,row.elements,row.status)
    }.update((sname,input,param,elements,"运行失败"))).map(_=>())
  }

  def getAllDutyById(uid:String):Future[Seq[DutysRow]]={
    db.run(Dutys.filter(_.uid===Integer.parseInt(uid)).sortBy(_.subtime.desc).result)
  }

  def getDuties:Future[Seq[(String,String,String,String,String,Int)]]={
    val query = for {
      c <- Dutys
      s <- Users if c.uid===s.id
    } yield (c.subtime, s.name, c.taskname, c.sname, c.status, c.uid)
    db.run(query.sortBy(_._1.desc).result)
  }

//  def getSingleDuty(uid:String,taskname:String):Future[DutysRow]={
//  db.run(Dutys.filter(_.uid===Integer.parseInt(uid)).filter(_.taskname===taskname).result.head)
//  }

    def getSingleDuty(uid:String,taskname:String):Future[Seq[DutysRow]]={
      db.run(Dutys.filter(_.uid===Integer.parseInt(uid)).filter(_.taskname===taskname).result)
    }

  def getRunningDuty:Future[Seq[DutysRow]]={
    db.run(Dutys.filter(_.status==="运行中").result)
  }


  def getDutyByType(uid:String,sabbrename:String):Future[Seq[DutysRow]]={
    db.run(Dutys.filter(_.uid===Integer.parseInt(uid)).filter(_.sabbrename===sabbrename).sortBy(_.subtime.desc).result)
  }

  def deleteDuty(uid:String,taskname:String):Future[Unit]={
    db.run(Dutys.filter(_.uid===Integer.parseInt(uid)).filter(_.taskname===taskname).delete).map(_=>())
  }

}
