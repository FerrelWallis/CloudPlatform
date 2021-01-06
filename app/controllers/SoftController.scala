package controllers

import dao.{softDao, usersDao}
import javax.inject._
import play.api.libs.json.Json
import play.api.mvc._
import utils.TableUtils

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SoftController @Inject()(cc: ControllerComponents,softdao:softDao,userdao:usersDao) extends AbstractController(cc) {

  def pca(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.pca(abbre))
  }

  def heatmap(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.heatmap(abbre))
  }

  def go(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.go(abbre))
  }

  def kegg(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.kegg(abbre))
  }

  def boxplot(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.boxplot(abbre))
  }

  def cca(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.cca(abbre))
  }


  def netWeight(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.netWeight(abbre))
  }

  def netDirected=Action{implicit request=>
    Ok(views.html.soft.netDirected())
  }

  def net=Action{implicit request=>
    Ok(views.html.soft.net())
  }

  def venn(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.jvenn(abbre))
  }

  def innergroup(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.innerGroupCorrelation(abbre))
  }

  def tax4fun(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.tax4fun(abbre))
  }

  def faprotax(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.faprotax(abbre))
  }

  def picrust(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.picrust(abbre))
  }

  def bargroup(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.bar(abbre))
  }

  def lefse(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.lefse(abbre))
  }

  def lefse2(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.lefse2(abbre))
  }

  def abiview(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.abiview(abbre))
  }

  def revseq(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.revseq(abbre))
  }

  def getorf(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.getorf(abbre))
  }

  def eprimer(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.eprimer3(abbre))
  }

  def volcano(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.volcano(abbre))
  }

  def manhattan(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.manhattan(abbre))
  }

  def treemap(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.treemap(abbre))
  }

  def violin(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.violin(abbre))
  }

  def freqhisto(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.freqHisto(abbre))
  }

  def breakbar(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.breakbar(abbre))
  }

  def circos(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.circos(abbre))
  }

  def pcoa(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.pcoa(abbre))
  }

  def pie(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.pie(abbre))
  }

  def errorbreakline(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.errorBreakLine(abbre))
  }

  def bubble(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.bubble(abbre))
  }

  def tableTrans(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.tableTransposition(abbre))
  }

  def merge2table(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.mergeTwoTable(abbre))
  }

  def mergemultable(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.mergeMulTable(abbre))
  }

  def tree(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.treemapmap(abbre))
  }

  def ternary(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.ternaryDialog(abbre))
  }

  def intraclass(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.intraclassCorrelation(abbre))
  }

  def circosphylum(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.circosPhylum(abbre))
  }

  def tTest(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.ttest(abbre))
  }

  def wilcoxon(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.wilcoxon(abbre))
  }

  def kruskaWallis(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.KruskaWallis(abbre))
  }

  def anova(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.anova(abbre))
  }

  def nmds(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.nmds(abbre))
  }


  def scaplot(abbre:String)=Action{implicit request=>
    Ok(views.html.soft.scatterplots(abbre))
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






  def getlatestSix = Action { implicit request =>
    val table=Await.result(softdao.getLastestSix,Duration.Inf)
    val row = table.map{x=>
      val pics=s"<a onclick=checklog('/CloudPlatform/SoftPage/"+x.abbrename+"')><div class='mws-report-icon mws-ic "+x.abbrename+"'></div></a>"
      val descrip=s"<a><h4>"+x.sname+"</h4><p style='padding-right: 10px'>"+x.descreption+"</p></a>"
      val likeclass=
        if(request.session.get("userId").isEmpty)
          "icon icon-star-empty"
        else {
          val userlike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf)
          if(userlike.split("/").contains(Integer.toString(x.id))) "icon icon-star yellow-icon"
          else "icon icon-star-empty"
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
          "icon icon-star-empty"
        else {
          val userlike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf)

          if(userlike.split("/").contains(Integer.toString(x.id))) "icon icon-star yellow-icon"
          else "icon icon-star-empty"
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
          "icon icon-star-empty"
        else {
          val userlike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf)
          if(userlike.split("/").contains(Integer.toString(x.id))) "icon icon-star yellow-icon"
          else "icon icon-star-empty"
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
          "icon icon-star-empty"
        else {
          val userlike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf)
          if(userlike.split("/").contains(Integer.toString(x.id))) "icon icon-star yellow-icon"
          else "icon icon-star-empty"
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
          "icon icon-star-empty"
        else {
          val userlike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf)
          if(userlike.split("/").contains(Integer.toString(x.id))) "icon icon-star yellow-icon"
          else "icon icon-star-empty"
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
          "icon icon-star-empty"
        else {
          val userlike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf)
          if(userlike.split("/").contains(Integer.toString(x.id))) "icon icon-star yellow-icon"
          else "icon icon-star-empty"
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
          "icon icon-star-empty"
        else {
          val userlike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf)
          if(userlike.split("/").contains(Integer.toString(x.id))) "icon icon-star yellow-icon"
          else "icon icon-star-empty"
        }
      val like= s"<i class='"+likeclass+"' onmouseover='mover($(this))' onmouseout='mout($(this))' onclick='collect($(this),"+x.id+")'></i>"
      val onclick="<div style='height: 115px' " + click + ">" + descrip + "</div>"

      Json.obj("description"-> onclick,"pics"->pics,"like"->like,"status"->status)
    }
    Ok(Json.obj("rows"->row))
  }



}
