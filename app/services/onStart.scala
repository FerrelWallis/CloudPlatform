package services

import java.io.File
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

class onStart @Inject()(softDao:softDao,utilsDao:utilsDao,dutyDao:dutyDao,dutyController: DutyController,rservice:RService){
  TableUtils.SoftsMap = Await.result(softDao.getAllSoft, Duration.Inf)

  var verifyMap: mutable.HashMap[String, String] = mutable.HashMap()

  var verifyTimeMap: mutable.HashMap[String, Long] = mutable.HashMap()

//  var latestNote:Int=Await.result(utilsDao.lateNoteId,Duration.Inf)

  verifyConfig
  restartRunningDuty

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


  //重新挂网站时检测是否存在运行中状态的任务，如果有，将之变为运行失败
  def restartRunningDuty ={
    val running=Await.result(dutyDao.getRunningDuty,Duration.Inf)
    running.foreach{duty=>
//      val dutyDir=Utils.path+"users/"+duty.uid+"/"+duty.taskname
//      val execCommand = new ExecCommand
//      execCommand.exect(s"sh $dutyDir/temp/run.sh",dutyDir+"/temp")
//      if (execCommand.isSuccess) {
//        new File(dutyDir+"/out").listFiles().filter(_.getName.contains("pdf")).map(_.getAbsolutePath)
////        Utils.pdf2Png(dutyDir+"/out/circle.pdf",dutyDir+"/out/circle.png")
////        Utils.pdf2Png(dutyDir+"/out/circle.pdf",dutyDir+"/out/circle.tiff")
//        dutyController.updateFini(duty.uid.toString,duty.taskname)
//        rservice.creatZip(dutyDir)
//      } else {
//
//        FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"错误信息：\n"+execCommand.getErrStr+"\n\n")
//      }
      val dutyDir=Utils.path+"users/"+duty.taskname
      FileUtils.writeStringToFile(new File(dutyDir,"log.txt"),"错误信息：\n文件格式错误，或云平台更新导致失效！\n\n")
      dutyDao.updateFailed(duty.uid.toString,duty.taskname)
    }
  }


}
