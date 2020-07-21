package controllers

import dao.{softDao, usersDao}
import javax.inject._
import play.api.libs.json.Json
import play.api.mvc._
import utils.TableUtils

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SoftController @Inject()(cc: ControllerComponents,softdao:softDao,userdao:usersDao) extends AbstractController(cc) {

  def pca(abbre:String,sname:String)=Action{implicit request=>
    Ok(views.html.soft.pca(abbre,sname))
  }

  def heatmap(abbre:String,sname:String)=Action{implicit request=>
    Ok(views.html.soft.heatmap(abbre,sname))
  }

  def go(abbre:String,sname:String)=Action{implicit request=>
    Ok(views.html.soft.go(abbre,sname))
  }

  def kegg(abbre:String,sname:String)=Action{implicit request=>
    Ok(views.html.soft.kegg(abbre,sname))
  }

  def boxplot(abbre:String,sname:String)=Action{implicit request=>
    Ok(views.html.soft.boxplot(abbre,sname))
  }

  def cca(abbre:String,sname:String)=Action{implicit request=>
    Ok(views.html.soft.cca(abbre,sname))
  }


  def netWeight(abbre:String,sname:String)=Action{implicit request=>
    Ok(views.html.soft.netWeight(abbre,sname))
  }

  def netDirected=Action{implicit request=>
    Ok(views.html.soft.netDirected())
  }

  def net=Action{implicit request=>
    Ok(views.html.soft.net())
  }

  def venn(abbre:String,sname:String)=Action{implicit request=>
    Ok(views.html.soft.jvenn(abbre,sname))
  }

  def vennChart(abbre:String,sname:String)=Action{implicit request=>
    Ok(views.html.soft.vennChart(abbre,sname))
  }

  def innergroup(abbre:String,sname:String)=Action{implicit request=>
    Ok(views.html.soft.innerGroupCorrelation(abbre,sname))
  }

  def scaplot(abbre:String,sname:String)=Action{implicit request=>
    Ok(views.html.soft.scatterplots(abbre,sname))
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
      val pics=s"<a onclick=checklog('/CloudPlatform/SoftPage/"+x.abbrename+"?abbre="+x.abbrename+"&sname="+x.sname+"')><div class='mws-report-icon mws-ic "+x.abbrename+"'></div></a>"
      val descrip=s"<a><h4>"+x.sname+"</h4><p style='padding-right: 10px'>"+x.descreption+"</p></a>"
      val likeclass=
        if(request.session.get("userId").isEmpty)
          "icon icon-star-empty"
        else {
          val userlike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf)
          if(userlike.indexOf(Integer.toString(x.id))>=0) "icon icon-star yellow-icon"
          else "icon icon-star-empty"
        }
      val like= s"<i class='"+likeclass+"' onmouseover='mover($(this))' onmouseout='mout($(this))' onclick='collect($(this),"+x.id+")'></i>"
      val onclick="<div style='height: 115px' onclick=checklog('/CloudPlatform/SoftPage/"+x.abbrename+"?abbre="+x.abbrename+"&sname="+x.sname+"')>" + descrip + "</div>"

      Json.obj("description"->onclick,"pics"->pics,"like"->like)
    }
    Ok(Json.obj("rows"->row))
  }

  def gethotestSix = Action { implicit request =>
    val table=Await.result(softdao.gethotestSix,Duration.Inf)
    val row = table.map{x=>
      val pics=s"<a onclick=checklog('/CloudPlatform/SoftPage/"+x.abbrename+"?abbre="+x.abbrename+"&sname="+x.sname+"')><div class='mws-report-icon mws-ic "+x.abbrename+"'></div></a>"
      val descrip=s"<a><h4>"+x.sname+"</h4><p style='padding-right: 10px'>"+x.descreption+"</p></a>"
      val likeclass=
        if(request.session.get("userId").isEmpty)
          "icon icon-star-empty"
        else {
          val userlike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf)
          if(userlike.indexOf(Integer.toString(x.id))>=0) "icon icon-star yellow-icon"
          else "icon icon-star-empty"
        }
      val like= s"<i class='"+likeclass+"' onmouseover='mover($(this))' onmouseout='mout($(this))' onclick='collect($(this),"+x.id+")'></i>"
      val onclick="<div style='height: 115px' onclick=checklog('/CloudPlatform/SoftPage/"+x.abbrename+"?abbre="+x.abbrename+"&sname="+x.sname+"')>" + descrip + "</div>"
      Json.obj("description"->onclick,"pics"->pics,"like"->like)
    }
    Ok(Json.obj("rows"->row))
  }

  def getAllSoft = Action { implicit request =>
    val row = TableUtils.SoftsMap.map{x=>
      val pics=s"<a onclick=checklog('/CloudPlatform/SoftPage/"+x.abbrename+"?abbre="+x.abbrename+"&sname="+x.sname+"')><div class='mws-report-icon mws-ic "+x.abbrename+"'></div></a>"
      val descrip=s"<a><h4>"+x.sname+"</h4><p style='padding-right: 10px'>"+x.descreption+"</p></a>"
      val likeclass=
        if(request.session.get("userId").isEmpty)
          "icon icon-star-empty"
        else {
          val userlike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf)
          if(userlike.indexOf(Integer.toString(x.id))>=0) "icon icon-star yellow-icon"
          else "icon icon-star-empty"
        }
      val like= s"<i class='"+likeclass+"' onmouseover='mover($(this))' onmouseout='mout($(this))' onclick='collect($(this),"+x.id+")'></i>"
      val onclick="<div style='height: 115px' onclick=checklog('/CloudPlatform/SoftPage/"+x.abbrename+"?abbre="+x.abbrename+"&sname="+x.sname+"')>" + descrip + "</div>"
      Json.obj("description"-> onclick,"pics"->pics,"like"->like)
    }
    Ok(Json.obj("rows"->row))
  }

  def getTypes(types:String)= Action { implicit request =>
    val row = TableUtils.SoftsMap.filter(_.types==types).map{x=>
      val pics=s"<a onclick=checklog('/CloudPlatform/SoftPage/"+x.abbrename+"?abbre="+x.abbrename+"&sname="+x.sname+"')><div class='mws-report-icon mws-ic "+x.abbrename+"'></div></a>"
      val descrip=s"<a><h4>"+x.sname+"</h4><p style='padding-right: 10px'>"+x.descreption+"</p></a>"
      val likeclass=
        if(request.session.get("userId").isEmpty)
          "icon icon-star-empty"
        else {
          val userlike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf)
          if(userlike.indexOf(Integer.toString(x.id))>=0) "icon icon-star yellow-icon"
          else "icon icon-star-empty"
        }
      val like= s"<i class='"+likeclass+"' onmouseover='mover($(this))' onmouseout='mout($(this))' onclick='collect($(this),"+x.id+")'></i>"
      val onclick="<div style='height: 115px' onclick=checklog('/CloudPlatform/SoftPage/"+x.abbrename+"?abbre="+x.abbrename+"&sname="+x.sname+"')>" + descrip + "</div>"
      Json.obj("description"-> onclick,"pics"->pics,"like"->like)
    }
    Ok(Json.obj("rows"->row))
  }

  def getLike= Action { implicit request =>
    val mylike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf).split("/").filter(_.trim!="")
    val row = TableUtils.SoftsMap.filter(x => mylike.contains(String.valueOf(x.id))).map{ x=>
      val pics=s"<a href='/CloudPlatform/SoftPage/"+x.abbrename+"?abbre="+x.abbrename+"&sname="+x.sname+"'><div class='mws-report-icon mws-ic "+x.abbrename+"'></div></a>"
      val descrip=s"<a><h4>"+x.sname+"</h4><p style='padding-right: 10px'>"+x.descreption+"</p></a>"
      val likeclass=
        if(request.session.get("userId").isEmpty)
          "icon icon-star-empty"
        else {
          val userlike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf)
          if(userlike.indexOf(Integer.toString(x.id))>=0) "icon icon-star yellow-icon"
          else "icon icon-star-empty"
        }
      val like= s"<i class='"+likeclass+"' onmouseover='mover($(this))' onmouseout='mout($(this))' onclick='collect($(this),"+x.id+")'></i>"
      val onclick="<div style='height: 115px' onclick=checklog('/CloudPlatform/SoftPage/"+x.abbrename+"?abbre="+x.abbrename+"&sname="+x.sname+"')>" + descrip + "</div>"
      Json.obj("description"-> onclick,"pics"->pics,"like"->like)
    }
    Ok(Json.obj("rows"->row))
  }

  def getLikebrief= Action { implicit request =>
    val mylike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf).split("/").filter(_.trim!="")
    val row = TableUtils.SoftsMap.filter(x => mylike.contains(String.valueOf(x.id))).map{ x=>
      val pics=s"<a href='/CloudPlatform/SoftPage/"+x.abbrename+"?abbre="+x.abbrename+"&sname="+x.sname+"'><div class='mws-report-icon mws-ic "+x.abbrename+"'></div></a>"
      val descrip=s"<a><h4>"+x.sname+"</h4></a>"
      val likeclass=
        if(request.session.get("userId").isEmpty)
          "icon icon-star-empty"
        else {
          val userlike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf)
          if(userlike.indexOf(Integer.toString(x.id))>=0) "icon icon-star yellow-icon"
          else "icon icon-star-empty"
        }
      val like= s"<i class='"+likeclass+"' onmouseover='mover($(this))' onmouseout='mout($(this))' onclick='collect($(this),"+x.id+")'></i>"
      val onclick="<div style='height: 115px' onclick=checklog('/CloudPlatform/SoftPage/"+x.abbrename+"?abbre="+x.abbrename+"&sname="+x.sname+"')>" + descrip + "</div>"
      Json.obj("description"-> onclick,"pics"->pics,"like"->like)
    }
    Ok(Json.obj("rows"->row))
  }

  def getSearch(search:String)= Action { implicit request =>
    val row = TableUtils.SoftsMap.filter(x=> x.sname.indexOf(search)>=0 || x.descreption.indexOf(search)>=0).map{x=>
      val pics=s"<a href='/CloudPlatform/SoftPage/"+x.abbrename+"'><div class='mws-report-icon mws-ic "+x.abbrename+"'></div></a>"
      val descrip=s"<a href='/CloudPlatform/SoftPage/"+x.abbrename+"'><h4>"+x.sname+"</h4><p style='padding-right: 10px'>"+x.descreption+"</p></a>"
      val likeclass=
        if(request.session.get("userId").isEmpty)
          "icon icon-star-empty"
        else {
          val userlike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf)
          if(userlike.indexOf(Integer.toString(x.id))>=0) "icon icon-star yellow-icon"
          else "icon icon-star-empty"
        }
      val like= s"<i class='"+likeclass+"' onmouseover='mover($(this))' onmouseout='mout($(this))' onclick='collect($(this),"+x.id+")'></i>"
      Json.obj("description"-> descrip,"pics"->pics,"like"->like)
    }
    Ok(Json.obj("rows"->row))
  }

}
