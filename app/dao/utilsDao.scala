package dao

import java.text.SimpleDateFormat
import java.util.Date

import javax.inject.Inject
import models.Tables._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent._

class utilsDao  @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit exec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile]{

  import profile.api._

  def getAk : Future[AkRow] = {
    db.run(Ak.result.head)
  }

  def addNotice(row:NoticeRow) : Future[Int] = {
    db.run(Notice returning Notice.map(_.id)+=row)
  }

  def lateNoteId : Future[Int] = {
    db.run(Notice.sortBy(_.id.desc).map(_.id).result.head)
  }

  def getLateNote:Future[NoticeRow]={
    val date=new SimpleDateFormat("yyyy-MM-dd").format(new Date())
    //发布时间未到，或者已经过了有效期
    db.run(Notice.filter(_.pubtime<=date).filter(_.failtime>=date).sortBy(_.id.desc).result.head)
  }

  def getNoteById(id:Int):Future[NoticeRow]={
    db.run(Notice.filter(_.id===id).result.head)
  }

  def getAllValidNotes:Future[Seq[NoticeRow]]={
    val date=new SimpleDateFormat("yyyy-MM-dd").format(new Date())
    //发布时间未到，或者已经过了有效期
    db.run(Notice.filter(_.pubtime<=date).sortBy(_.pubtime.desc).result)
  }

}
