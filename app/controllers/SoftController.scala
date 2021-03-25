package controllers

import dao.{dutyDao, softDao, usersDao}
import javax.inject._
import play.api.libs.json.Json
import play.api.mvc._
import utils.TableUtils

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SoftController @Inject()(cc: ControllerComponents,softdao:softDao,userdao:usersDao,dutydao:dutyDao) extends AbstractController(cc) {

  def softPage(abbre:String) = Action { implicit request =>
    Ok(abbre match {
      case "GO" => views.html.soft.go(abbre)
      case "PCA" => views.html.soft.pca(abbre)
      case "Heatmap" => views.html.soft.heatmap(abbre)
      case "KEGG" => views.html.soft.kegg(abbre)
      case "Boxplot" => views.html.soft.boxplot(abbre)
      case "CCA" => views.html.soft.cca(abbre)
      case "NetWeight" => views.html.soft.netWeight(abbre)
      case "Venn" => views.html.soft.jvenn(abbre)
      case "IGC" => views.html.soft.innerGroupCorrelation(abbre)
      case "TAX" => views.html.soft.tax4fun(abbre)
      case "FAP" => views.html.soft.faprotax(abbre)
      case "PIC" => views.html.soft.picrust(abbre)
      case "BAR" => views.html.soft.bar(abbre)
      case "LEF" => views.html.soft.lefse(abbre)
      case "LF2" => views.html.soft.lefse2(abbre)
      case "ABI" => views.html.soft.abiview(abbre)
      case "RSQ" => views.html.soft.revseq(abbre)
      case "GTF" => views.html.soft.getorf(abbre)
      case "PMR" => views.html.soft.eprimer3(abbre)
      case "VOC" => views.html.soft.volcano(abbre)
      case "MHT" => views.html.soft.manhattan(abbre)
      case "TRM" => views.html.soft.treemap(abbre)
      case "VIO" => views.html.soft.violin(abbre)
      case "FH" => views.html.soft.freqHisto(abbre)
      case "BRB" => views.html.soft.breakbar(abbre)
      case "CIR" => views.html.soft.circos(abbre)
      case "PCO" => views.html.soft.pcoa(abbre)
      case "BIS" => views.html.soft.pie(abbre)
      case "EBL" => views.html.soft.errorBreakLine(abbre)
      case "BB" => views.html.soft.bubble(abbre)
      case "TT" => views.html.soft.tableTransposition(abbre)
      case "MTT" => views.html.soft.mergeTwoTable(abbre)
      case "MMT" => views.html.soft.mergeMulTable(abbre)
      case "TM" => views.html.soft.treemapmap(abbre)
      case "TRY" => views.html.soft.ternaryDialog(abbre)
      case "ITC" => views.html.soft.intraclassCorrelation(abbre)
      case "CCS" => views.html.soft.circosPhylum(abbre)
      case "TC" => views.html.soft.ttest(abbre)
      case "WT" => views.html.soft.wilcoxon(abbre)
      case "KWT" => views.html.soft.KruskaWallis(abbre)
      case "AOV" => views.html.soft.anova(abbre)
      case "NMD" => views.html.soft.nmds(abbre)
      case "TPC" => views.html.soft.pca3d(abbre)
      case "PLS" => views.html.soft.plsda(abbre)
      case "LIN" => views.html.soft.linerRegression(abbre)
      case "MLN" => views.html.soft.multiLinear(abbre)
      case "RF" => views.html.soft.randomForest(abbre)
      case "SCA" => views.html.soft.scatterplots(abbre)
      case "ADB" => views.html.soft.boxDiversity(abbre)
    })
  }

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

  def getlatestSix = Action { implicit request =>
    val table=Await.result(softdao.getLastestSix,Duration.Inf)
    val row = table.map{x=>
      val pics=s"<a onclick=checklog('/CloudPlatform/SoftPage/"+x.abbrename+"')><div class='mws-report-icon mws-ic "+x.abbrename+"'></div></a>"
      val descrip=s"<a><h4>"+x.sname+"</h4><p style='padding-right: 10px'>"+x.descreption+"</p></a>"
      val likeclass=
        if(request.session.get("userId").isEmpty)
          "fa fa-lg  fa-star-o"
        else {
          val userlike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf)
          if(userlike.split("/").contains(Integer.toString(x.id))) "fa fa-lg  fa-star yellow-icon"
          else "fa fa-lg  fa-star-o"
        }
      val like= s"<i class='"+likeclass+"' onmouseover='mover($(this))' onmouseout='mout($(this))' onclick='collect($(this),"+x.id+")'></i>"
      val onclick="<div style='height: 115px' onclick=checklog('/CloudPlatform/SoftPage/"+x.abbrename+"')>" + descrip + "</div>"

      Json.obj("description"->onclick,"pics"->pics,"like"->like)
    }
    Ok(Json.obj("rows"->row))
  }

  def gethotestSix = Action { implicit request =>
    val table=Await.result(softdao.gethotestSix,Duration.Inf)
    val row = table.map{x=>
      val pics=s"<a onclick=checklog('/CloudPlatform/SoftPage/"+x.abbrename+"')><div class='mws-report-icon mws-ic "+x.abbrename+"'></div></a>"
      val descrip=s"<a><h4>"+x.sname+"</h4><p style='padding-right: 10px'>"+x.descreption+"</p></a>"
      val likeclass=
        if(request.session.get("userId").isEmpty)
          "fa fa-lg  fa-star-o"
        else {
          val userlike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf)

          if(userlike.split("/").contains(Integer.toString(x.id))) "fa fa-lg  fa-star yellow-icon"
          else "fa fa-lg  fa-star-o"
        }
      val like= s"<i class='"+likeclass+"' onmouseover='mover($(this))' onmouseout='mout($(this))' onclick='collect($(this),"+x.id+")'></i>"
      val onclick="<div style='height: 115px' onclick=checklog('/CloudPlatform/SoftPage/"+x.abbrename+"')>" + descrip + "</div>"
      Json.obj("description"->onclick,"pics"->pics,"like"->like)
    }
    Ok(Json.obj("rows"->row))
  }

  def getAllSoft = Action { implicit request =>
    val row = TableUtils.SoftsMap.map{x=>
      val status=if(x.status==1) "" else "waiting"
      val click=
        if(status=="") "onclick=checklog('/CloudPlatform/SoftPage/"+x.abbrename+"')"
        else "onclick=waiting()"
      val pics=s"<a " + click + "><div class='mws-report-icon mws-ic "+x.abbrename+"'></div></a>"
      val descrip=s"<a><h4>"+x.sname+"</h4><p style='padding-right: 10px'>"+x.descreption+"</p></a>"
      val likeclass=
        if(request.session.get("userId").isEmpty)
          "fa fa-lg fa-star-o"
        else {
          val userlike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf)
          if(userlike.split("/").contains(Integer.toString(x.id))) "fa fa-lg fa-star yellow-icon"
          else "fa fa-lg fa-star-o"
        }
      val like= s"<i class='"+likeclass+"' onmouseover='mover($(this))' onmouseout='mout($(this))' onclick='collect($(this),"+x.id+")'></i>"
      val onclick="<div style='height: 115px' " + click + ">" + descrip + "</div>"

      Json.obj("description"-> onclick,"pics"->pics,"like"->like,"status"->status)
    }
    Ok(Json.obj("rows"->row))
  }

  def getTypes(types:String)= Action { implicit request =>
    val row = TableUtils.SoftsMap.filter(_.types==types).map{x=>
      val status=if(x.status==1) "" else "waiting"
      val click=
        if(status=="") "onclick=checklog('/CloudPlatform/SoftPage/"+x.abbrename+"')"
        else "onclick=waiting()"
      val pics=s"<a " + click + "><div class='mws-report-icon mws-ic "+x.abbrename+"'></div></a>"
      val descrip=s"<a><h4>"+x.sname+"</h4><p style='padding-right: 10px'>"+x.descreption+"</p></a>"
      val likeclass=
        if(request.session.get("userId").isEmpty)
          "fa fa-lg  fa-star-o"
        else {
          val userlike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf)
          if(userlike.split("/").contains(Integer.toString(x.id))) "fa fa-lg  fa-star yellow-icon"
          else "fa fa-lg  fa-star-o"
        }
      val like= s"<i class='"+likeclass+"' onmouseover='mover($(this))' onmouseout='mout($(this))' onclick='collect($(this),"+x.id+")'></i>"
      val onclick="<div style='height: 115px' " + click + ">" + descrip + "</div>"

      Json.obj("description"-> onclick,"pics"->pics,"like"->like,"status"->status)
    }
    Ok(Json.obj("rows"->row))
  }

  def getLike= Action { implicit request =>
    val mylike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf).split("/").filter(_.trim!="")
    val row = TableUtils.SoftsMap.filter(x => mylike.contains(String.valueOf(x.id))).map{ x=>
      val status=if(x.status==1) "" else "waiting"
      val click=
        if(status=="") "onclick=checklog('/CloudPlatform/SoftPage/"+x.abbrename+"')"
        else "onclick=waiting()"
      val pics=s"<a " + click + "><div class='mws-report-icon mws-ic "+x.abbrename+"'></div></a>"
      val descrip=s"<a><h4>"+x.sname+"</h4><p style='padding-right: 10px'>"+x.descreption+"</p></a>"
      val likeclass=
        if(request.session.get("userId").isEmpty)
          "fa fa-lg  fa-star-o"
        else {
          val userlike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf)
          if(userlike.split("/").contains(Integer.toString(x.id))) "fa fa-lg  fa-star yellow-icon"
          else "fa fa-lg  fa-star-o"
        }
      val like= s"<i class='"+likeclass+"' onmouseover='mover($(this))' onmouseout='mout($(this))' onclick='collect($(this),"+x.id+")'></i>"
      val onclick="<div style='height: 115px' " + click + ">" + descrip + "</div>"

      Json.obj("description"-> onclick,"pics"->pics,"like"->like,"status"->status)
    }
    Ok(Json.obj("rows"->row))
  }

  def getLikebrief= Action { implicit request =>
    val mylike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf).split("/").filter(_.trim!="")
    val row = TableUtils.SoftsMap.filter(x => mylike.contains(String.valueOf(x.id))).map{ x=>
      val pics=s"<a href='/CloudPlatform/SoftPage/"+x.abbrename+"'><div class='mws-report-icon mws-ic "+x.abbrename+"'></div></a>"
      val descrip=s"<a><h4>"+x.sname+"</h4></a>"
      val likeclass=
        if(request.session.get("userId").isEmpty)
          "fa fa-lg  fa-star-o"
        else {
          val userlike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf)
          if(userlike.split("/").contains(Integer.toString(x.id))) "fa fa-lg  fa-star yellow-icon"
          else "fa fa-lg  fa-star-o"
        }
      val like= s"<i class='"+likeclass+"' onmouseover='mover($(this))' onmouseout='mout($(this))' onclick='collect($(this),"+x.id+")'></i>"
      val onclick="<div style='height: 115px' onclick=checklog('/CloudPlatform/SoftPage/"+x.abbrename+"')>" + descrip + "</div>"
      Json.obj("description"-> onclick,"pics"->pics,"like"->like)
    }
    Ok(Json.obj("rows"->row))
  }

  def getSearch(search:String)= Action { implicit request =>
    val row = TableUtils.SoftsMap.filter(y=> y.sname.toLowerCase.contains(search) || y.descreption.toLowerCase.contains(search)).map{x=>
      val status=if(x.status==1) "" else "waiting"
      val click=
        if(status=="") "onclick=checklog('/CloudPlatform/SoftPage/"+x.abbrename+"')"
        else "onclick=waiting()"
      val pics=s"<a " + click + "><div class='mws-report-icon mws-ic "+x.abbrename+"'></div></a>"
      val descrip=s"<a><h4>"+x.sname+"</h4><p style='padding-right: 10px'>"+x.descreption+"</p></a>"
      val likeclass=
        if(request.session.get("userId").isEmpty)
          "fa fa-lg  fa-star-o"
        else {
          val userlike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf)
          if(userlike.split("/").contains(Integer.toString(x.id))) "fa fa-lg  fa-star yellow-icon"
          else "fa fa-lg  fa-star-o"
        }
      val like= s"<i class='"+likeclass+"' onmouseover='mover($(this))' onmouseout='mout($(this))' onclick='collect($(this),"+x.id+")'></i>"
      val onclick="<div style='height: 115px' " + click + ">" + descrip + "</div>"

      Json.obj("description"-> onclick,"pics"->pics,"like"->like,"status"->status)
    }
    Ok(Json.obj("rows"->row))
  }


  //test below
  def softpage=Action{implicit request=>
    Ok(views.html.soft.softPage())
  }

  def testsoft=Action {implicit request=>
    Ok(views.html.test.testsoft())
  }

  def testcynet=Action {implicit request=>
    Ok(views.html.test.cynet())
  }

}
