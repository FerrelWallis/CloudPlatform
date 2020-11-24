package dao

import com.typesafe.sslconfig.ssl.FakeChainedKeyStore.User
import javax.inject.Inject
import models.Tables.{Running,RunningRow}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class runningDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit exec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile]{

  import profile.api._

  def getRunning : Future[Seq[RunningRow]] = {
    db.run(Running.result)
  }

  def updateStatus(id:Int,content:String): Future[Unit]={
    db.run(Running.filter(_.id ===id).map(_.content).update(content)).map(_=>())
  }



}
