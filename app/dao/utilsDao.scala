package dao

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

}
