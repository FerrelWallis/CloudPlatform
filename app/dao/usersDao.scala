package dao

import javax.inject.Inject
import models.Tables.{Users, UsersRow}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class usersDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit exec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile]{

  import profile.api._

  def getById(id:String) : Future[UsersRow] = {
    db.run(Users.filter(_.id === Integer.parseInt(id)).result.head)
  }

  def addUser(row:UsersRow) : Future[Int] = {
    db.run(Users returning  Users.map(_.id)+=row)
  }

  def checkPhoneExist(phone:String):Future[Seq[UsersRow]]={
    db.run(Users.filter(_.phone===phone).result)
  }

  def checkEmailExist(email:String):Future[Seq[UsersRow]]={
    db.run(Users.filter(_.email===email).result)
  }


  def checkUserByPhone(phone:String,pwd:String) : Future[Seq[UsersRow]] = {
    db.run(Users.filter(_.phone === phone).filter(_.pwd === pwd).result)
  }

  def checkUserByEmail(email:String,pwd:String) : Future[Seq[UsersRow]] = {
    db.run(Users.filter(_.email === email).filter(_.pwd === pwd).result)
  }

  def updatePassword(phone:String,pwd:String) : Future[Unit] = {
    db.run(Users.filter(_.phone === phone).map(_.pwd).update(pwd)).map(_=>())
  }

  def updateLike(id:String,like:String) : Future[Unit] = {
    db.run(Users.filter(_.id === Integer.parseInt(id)).map(_.like).update(like)).map(_=>())
  }

  def getLike(id:String) : Future[String] = {
    db.run(Users.filter(_.id === Integer.parseInt(id)).map(_.like).result.head)
  }

}
