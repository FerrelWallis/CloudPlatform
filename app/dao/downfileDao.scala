package dao

import javax.inject.Inject
import models.Tables.{Downfiles, DownfilesRow}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class downfileDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit exec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile]{
  import profile.api._

  def getFilesByAbbre(abbre:String) : Future[DownfilesRow] = {
    db.run(Downfiles.filter(_.abbre===abbre).result.head)
  }
}
