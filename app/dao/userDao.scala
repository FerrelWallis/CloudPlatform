package dao

import javax.inject.Inject
import models.Tables._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

import scala.concurrent.ExecutionContext

class userDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit exec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile]{

  import profile.api._

  def getByPhone(phone:String) : Future[UserRow] = {
    db.run(User.filter(_.phone === phone).result.head)
  }

  def addUser(row:UserRow) : Future[String] = {
    db.run(User returning  User.map(_.phone)+=row)

  }

  def checkPhoneExist(phone:String):Future[Seq[UserRow]]={
    db.run(User.filter(_.phone===phone).result)
  }

  def checkEmailExist(email:String):Future[Seq[UserRow]]={
    db.run(User.filter(_.email===email).result)
  }


  def checkUser(phone:String,pwd:String) : Future[Seq[UserRow]] = {
    db.run(User.filter(_.phone === phone).filter(_.pwd === pwd).result)
  }

  def updatePassword(phone:String,pwd:String) : Future[Unit] = {
    db.run(User.filter(_.phone === phone).map(_.pwd).update(pwd)).map(_=>())
  }



}

