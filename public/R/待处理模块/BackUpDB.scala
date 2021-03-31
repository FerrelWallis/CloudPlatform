package utils

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.{Executors, TimeUnit}

import org.apache.commons.io.FileUtils

import scala.sys.process._

object BackUpDB {

  val db = "fudan_out_sample_online"
  var isSuccess: Boolean = false
  val err = new StringBuilder
  val out = new StringBuilder
  val log = ProcessLogger(out append _ append "\n", err append _ append "\n")
  val path = s"${Global.path}/dbBackup"

  def backUpDB = {
    if (!new File(path).exists()) {
      new File(path).mkdirs()
    }
    val runnable = new Runnable {
      override def run() = {
        val cmd = s"mysqldump -u root -pYingfei123 $db > ${path}/${db}_$date.sql"
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
}
