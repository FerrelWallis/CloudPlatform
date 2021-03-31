package controllers

import java.io.{File, FileInputStream}
import java.nio.file.{Files, Path}

import dao.dutyDao
import javax.inject.Inject
import jdk.nashorn.internal.objects.Global
import org.apache.commons.io.FileUtils
import org.json.JSONObject
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Headers, Result}
import utils.{CompressUtil, ExecCommand, MyStringTool, Utils}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.collection.JavaConverters._
import utils.Implicits._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.parsing.json
import scala.util.parsing.json.JSONObject


class RFunctionController @Inject()(cc: ControllerComponents, dutydao: dutyDao, rservice: RService, dutyController: DutyController)(implicit exec: ExecutionContext) extends AbstractController(cc) with MyStringTool {



}
