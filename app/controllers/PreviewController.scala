package controllers
import javax.inject._
import play.api.mvc._
import models.Tables.DutysRow
import dao.dutyDao
import play.api.libs.json.Json

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PreviewController @Inject()(cc: ControllerComponents,dutydao:dutyDao) extends AbstractController(cc)  {

  def previewPage(abbre:String, taskname:String) = Action {implicit request=>
    val id=request.session.get("userId").get
    val row=Await.result(dutydao.getSingleDuty(id,taskname),Duration.Inf)
    //taskname不存在head会报错invoker.first
    if(row.length==1){
      Ok(abbre match {
        case "GO" => views.html.task.redrawGokegg(row.head)
        case "PCA" => views.html.task.RedrawPCA(row.head)
        case "Heatmap" => views.html.task.redrawHeat(row.head)
        case "KEGG" => views.html.task.redrawKegg(row.head)
        case "Boxplot" => views.html.task.redrawBoxplot(row.head)
        case "CCA" => views.html.task.redrawCCA(row.head)
        case "NetWeight" => views.html.task.redrawNet(row.head)
        case "IGC" => views.html.task.redrawIGC(row.head)
        case "TAX" => views.html.task.redrawTAX(row.head)
        case "FAP" => views.html.task.redrawDataOnly(row.head)
        case "PIC" => views.html.task.redrawPICRUST(row.head)
        case "BAR" => views.html.task.redrawBar(row.head)
        case "LEF" => views.html.task.redrawLefse(row.head)
        case "LF2" => views.html.task.redrawLefse2(row.head)
        case "ABI" => views.html.task.redrawAbiview(row.head)
        case "RSQ" => views.html.task.redrawDataOnly(row.head)
        case "GTF" => views.html.task.redrawDataOnly(row.head)
        case "PMR" => views.html.task.redrawDataOnly(row.head)
        case "VOC" => views.html.task.redrawVolcano(row.head)
        case "MHT" => views.html.task.redrawManhattan(row.head)
        case "TRM" => views.html.task.redrawTreemap(row.head)
        case "VIO" => views.html.task.redrawViolin(row.head)
        case "FH" => views.html.task.redrawFreqHisto(row.head)
        case "BRB" => views.html.task.redrawBreakbar(row.head)
        case "CIR" => views.html.task.redrawCircos(row.head)
        case "PCO" => views.html.task.RedrawPCA(row.head)
        case "BIS" => views.html.task.redrawPie(row.head)
        case "EBL" => views.html.task.redrawEBLine(row.head)
        case "BB" => views.html.task.redrawBubble(row.head)
        case "TT" => views.html.task.redrawDataOnly(row.head)
        case "MTT" => views.html.task.redrawDataOnly(row.head)
        case "MMT" => views.html.task.redrawDataOnly(row.head)
        case "TM" => views.html.task.redrawTreemapmap(row.head)
        case "TRY" => views.html.task.redrawTernary(row.head)
        case "ITC" => views.html.task.redrawIGC(row.head)
        case "CCS" => views.html.task.redrawCircosPhylum(row.head)
        case "TC" => views.html.task.redrawDataOnly(row.head)
        case "WT" => views.html.task.redrawDataOnly(row.head)
        case "KWT" => views.html.task.redrawDataOnly(row.head)
        case "AOV" => views.html.task.redrawDataOnly(row.head)
        case "NMD" => views.html.task.redrawNMDS(row.head)
        case "TPC" => views.html.task.redraw3DPCA(row.head)
        case "PLS" => views.html.task.redrawPlsda(row.head)
        case "LIN" => views.html.task.redrawLIN(row.head)
        case "MLN" => views.html.task.redrawDataOnly(row.head)
        case "RF" => views.html.task.redrawRandomForest(row.head)
        case "SCA" => views.html.task.redrawScatter(row.head)
        case "ADB" => views.html.task.redrawBoxDiveristy(row.head)
      })
    }else{
      Redirect(routes.HomeController.mytask())
    }
  }



}
