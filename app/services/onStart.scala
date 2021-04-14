package services

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.{Executors, TimeUnit}

import controllers.{DutyController, RService}
import dao.{dutyDao, softDao, utilsDao}
import javax.inject.Inject
import org.apache.commons.io.FileUtils
import utils.{ExecCommand, TableUtils, Utils}

import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import play.api.mvc.Action

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.sys.process._

class onStart @Inject()(softDao:softDao,utilsDao:utilsDao,dutyDao:dutyDao,dutyController: DutyController,rservice:RService){
  TableUtils.SoftsMap = Await.result(softDao.getAllSoft, Duration.Inf)

  var verifyMap: mutable.HashMap[String, String] = mutable.HashMap()

  var verifyTimeMap: mutable.HashMap[String, Long] = mutable.HashMap()

  verifyConfig
  restartRunningDuty
  softlikeUpdate

  if(Utils.path == "/mnt/sdb/ww/CloudPlatform/" || Utils.path == "/mnt/sdb/ww/bak/CloudPlatform/") backUpDB

  def backUpDB = {
    val path = Utils.path + "dbBackup"
    val dbname = "cloudplatform"
    val out = new StringBuilder()
    val err = new StringBuilder()
    val log = ProcessLogger(out append _ append "\n", err append _ append "\n")
    if (!new File(path).exists()) {
      new File(path).mkdirs()
    }
    val runnable = new Runnable {
      override def run() = {
        val cmd = s"mysqldump -u root -p123456 ${dbname} > ${path}/${dbname}_$date.sql"
        FileUtils.writeStringToFile(new File(s"$path/dbBackUp.sh"), cmd)
        val exitCode = Process(s"sh $path/dbBackUp.sh", new File(s"$path")) ! log
        if (exitCode != 0) println(err)
      }
    }
    val service = Executors.newSingleThreadScheduledExecutor()
    // 第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间
    service.scheduleAtFixedRate(runnable, 0, 7, TimeUnit.DAYS)
  }

  def date: String = {
    val now = new Date
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
    val date = dateFormat.format(now)
    date
  }

  def verifyConfig = {
    val runnable = new Runnable {
      override def run() = {
        val time = System.currentTimeMillis()
        val clean = verifyTimeMap.filter(x => (time - x._2) / 1000.0 > 600) //时间间隔大于10分钟
        clean.keys.foreach { x =>
          verifyMap.remove(x)
        }
        verifyTimeMap = clean
      }
    }
    val service = Executors.newSingleThreadScheduledExecutor()
    // 第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间
    service.scheduleAtFixedRate(runnable, 1, 3, TimeUnit.MINUTES)
  }


  //重新挂网站时检测是否存在运行中状态的任务，如果有，将之变为运行失败
  def restartRunningDuty ={
    val running=Await.result(dutyDao.getRunningDuty,Duration.Inf)
    running.foreach{duty=>
      val dutyDir=Utils.path+"users/"+duty.taskname
      FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"错误信息：\n文件格式错误，或云平台更新导致失效！\n\n")
      dutyDao.updateFailed(duty.uid.toString,duty.taskname)
    }
  }


  def softlikeUpdate = {
    val runnable = new Runnable {
      override def run() = {
        Await.result(softDao.gethotestFreq,Duration.Inf).map{result =>
          Await.result(softDao.updateLike(result._1,result._2),Duration.Inf)
        }
      }
    }
    val service = Executors.newSingleThreadScheduledExecutor()
    // 第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间
    service.scheduleAtFixedRate(runnable, 0, 3, TimeUnit.DAYS)
  }

}
