package utils

import java.io.File
import java.nio.file.Files

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Tool {



  def execFuture[T](f: Future[T]): T = {
    Await.result(f, Duration.Inf)
  }




}
