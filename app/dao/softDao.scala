package dao

import javax.inject.Inject
import models.Tables._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}


class softDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit exec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile]{

  import profile.api._

  def getLastestSix : Future[Seq[SoftRow]] = {
    db.run(Soft.filter(_.status === 1).sortBy(_.id.desc).take(6).result)
  }

  def gethotestSix : Future[Seq[SoftRow]] = {
    db.run(Soft.filter(_.status === 1).sortBy(_.likefreq.desc).take(6).result)
  }

  def gethotestFreq : Future[Seq[(String,Long)]] = {
    val sql = sql"select sabbrename,COUNT(taskname) from dutys GROUP BY sabbrename ORDER BY COUNT(taskname) DESC".as[(String,Long)]
    db.run(sql)
  }

  def updateLike(sabbrename:String,likefreq:Long): Future[Unit]={
    db.run(Soft.filter(_.abbrename === sabbrename).map(_.likefreq).update(likefreq.toInt)).map(_=>())
  }


  def getAllSoft : Future[Seq[SoftRow]] = {
    db.run(Soft.sortBy(_.status.desc).result)
  }

  def getSoftByTypes(types:String) : Future[Seq[SoftRow]] = {
    db.run(Soft.filter(_.status === 1).filter(_.types === types).result)
  }

  def getlikefreq(id:String): Future[Int] = {
    db.run(Soft.filter(_.id === Integer.parseInt(id)).map(_.likefreq).result.head)
  }

  def addLike(id:String) : Future[Seq[String]] = {
    val sql = sql"update soft set likefreq=likefreq+1 where id=#$id".as[String]
    db.run(sql)
  }

  def delLike(id:String) : Future[Seq[String]] = {
    val sql = sql"update soft set likefreq=likefreq-1 where id=#$id".as[String]
    db.run(sql)
  }



}
