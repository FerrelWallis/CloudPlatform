package services

import dao.softDao
import javax.inject.Inject
import utils.TableUtils

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class onStart @Inject()(softDao:softDao){
  TableUtils.SoftsMap = Await.result(softDao.getAllSoft, Duration.Inf)
}
