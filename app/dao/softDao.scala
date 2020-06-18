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
    db.run(Soft.sortBy(_.id.desc).take(6).result)
  }

  def gethotestSix : Future[Seq[SoftRow]] = {
    db.run(Soft.sortBy(_.likefreq.desc).take(6).result)
  }

  def getAllSoft : Future[Seq[SoftRow]] = {
    db.run(Soft.result)
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
