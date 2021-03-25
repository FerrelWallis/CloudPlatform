package dao

import javax.inject.Inject
import models.Tables.{Mailbox, MailboxRow, Feedback}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class mailDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit exec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile]{
  import profile.api._

  def addMail(row:MailboxRow) : Future[Int] = {
    db.run(Mailbox returning Mailbox.map(_.mid) += row)
  }

  def getMailsByFid(fid:String) : Future[Seq[MailboxRow]] = {
    db.run(Mailbox.filter(_.fid===fid.toInt).sortBy(_.sendtime.desc).result)
  }

//  def getMailsByUid(uid:String) : Future[Seq[MailboxRow]] = {
//    db.run(Mailbox.filter(_.uid===uid.toInt).sortBy(_.sendtime.desc).result)
//  }

  def getMailsByUid(uid:String) : Future[Seq[(Int,Int,Int,String,String,String,Int,String)]] = {
    val query = for {
      c <- Mailbox if c.uid === uid.toInt && c.sender === "auth"
      s <- Feedback if c.fid === s.fid
    } yield (c.mid,c.uid,c.fid,c.sender,c.sendtime,c.content,c.status,s.title)
    db.run(query.sortBy(_._5.desc).result)
    //    db.run(Feedback.sortBy(_.subtime.desc).result)
  }

  def getMailsUnread(uid:String) : Future[Seq[MailboxRow]] = {
    db.run(Mailbox.filter(_.uid===uid.toInt).filter(_.sender==="auth").filter(_.status===1).result)
  }

  def updateStatus(mid:Int): Future[Unit]={
    db.run(Mailbox.filter(_.mid === mid).map(_.status).update(0)).map(_=>())
  }

  def deleteMail(fid:String):Future[Unit]={
    db.run(Mailbox.filter(_.fid===Integer.parseInt(fid)).delete).map(_=>())
  }
}
