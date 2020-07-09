package services

import java.util.concurrent.{Executors, TimeUnit}

import dao.softDao
import javax.inject.Inject
import utils.{TableUtils, Utils}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class onStart @Inject()(softDao:softDao){
  TableUtils.SoftsMap = Await.result(softDao.getAllSoft, Duration.Inf)
  var verifyMap: mutable.HashMap[String, String] = mutable.HashMap()

  var verifyTimeMap: mutable.HashMap[String, Long] = mutable.HashMap()

  verifyConfig

  def verifyConfig = {
    val runnable = new Runnable {
      override def run() = {
        val time = System.currentTimeMillis()
        val clean = verifyTimeMap.filter(x => (time - x._2) / 1000.0 > 600) //时间间隔大于10分钟
        clean.keys.foreach { x =>
//          println("keys="+x)
//          println("map="+verifyMap)
//          println("timemap"+verifyTimeMap)
          verifyMap.remove(x)
        }
        verifyTimeMap = clean
      }
    }

    val service = Executors.newSingleThreadScheduledExecutor()
    // 第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间
    service.scheduleAtFixedRate(runnable, 1, 3, TimeUnit.MINUTES)
  }


}
