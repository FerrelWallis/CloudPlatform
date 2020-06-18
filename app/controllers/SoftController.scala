package controllers

import dao.{softDao, usersDao}
import javax.inject._
import play.api.libs.json.Json
import play.api.mvc._
import utils.TableUtils

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SoftController @Inject()(cc: ControllerComponents,softdao:softDao,userdao:usersDao) extends AbstractController(cc) {

  def pca=Action{implicit request=>
    Ok(views.html.soft.pca())
  }

  def heatmap=Action{implicit request=>
    Ok(views.html.soft.heatmap())
  }

  def gokegg=Action{implicit request=>
    Ok(views.html.soft.go())
  }

//  def kegg=Action{implicit request=>
//    Ok(views.html.soft.kegg())
//  }

  def boxplot=Action{implicit request=>
    Ok(views.html.soft.boxplot())
  }

  def cca=Action{implicit request=>
    Ok(views.html.soft.cca())
  }


  def netWeight=Action{implicit request=>
    Ok(views.html.soft.netWeight())
  }

  def netDirected=Action{implicit request=>
    Ok(views.html.soft.netDirected())
  }

  def net=Action{implicit request=>
    Ok(views.html.soft.net())
  }

  //test below
  def softpage=Action{implicit request=>
    Ok(views.html.soft.softPage())
  }

  def testsoft=Action {implicit request=>
    Ok(views.html.test.testsoft())
  }






  def getlatestSix = Action { implicit request =>
    val table=Await.result(softdao.getLastestSix,Duration.Inf)
    val row = table.map{x=>
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

  def gethotestSix = Action { implicit request =>
    val table=Await.result(softdao.gethotestSix,Duration.Inf)
    val row = table.map{x=>
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

  def getAllSoft = Action { implicit request =>
    val row = TableUtils.SoftsMap.map{x=>
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

  def getTypes(types:String)= Action { implicit request =>
    val row = TableUtils.SoftsMap.filter(_.types==types).map{x=>
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

  def getLike= Action { implicit request =>
    val mylike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf).split("/").filter(_.trim!="")
    val row = TableUtils.SoftsMap.filter(x => mylike.contains(String.valueOf(x.id))).map{ x=>
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

  def getLikebrief= Action { implicit request =>
    val mylike=Await.result(userdao.getLike(request.session.get("userId").get),Duration.Inf).split("/").filter(_.trim!="")
    val row = TableUtils.SoftsMap.filter(x => mylike.contains(String.valueOf(x.id))).map{ x=>
      val pics=s"<a href='/CloudPlatform/SoftPage/"+x.abbrename+"'><div class='mws-report-icon mws-ic "+x.abbrename+"'></div></a>"
      val descrip=s"<a href='/CloudPlatform/SoftPage/"+x.abbrename+"'><h4>"+x.sname+"</h4></a>"
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
