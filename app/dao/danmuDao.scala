package dao

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import models.Tables.{DanmuRow, Danmu}

import scala.concurrent.{ExecutionContext, Future}

class danmuDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit exec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile]{
  import profile.api._

  def updateDanmus(sabbre:String,danmus:String) : Future[Unit] = {
    db.run(Danmu.filter(_.sabbre === sabbre).map(_.danmus).update(danmus)).map(_=>())
  }

  def getDanmus(sabbre:String):Future[String]={
    db.run(Danmu.filter(_.sabbre===sabbre).map(_.danmus).result.head)
  }


}
