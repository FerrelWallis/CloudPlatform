package controllers

import java.io.File
import java.nio.file.Files

import javax.inject._
import play.api.mvc._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def index = Action {implicit request=>
    Redirect(routes.HomeController.home())
  }

  def home = Action {implicit request=>
    Ok(views.html.home.index())
  }

  def home2= Action {implicit request=>
    Ok(views.html.home.index2())
  }

  def personal=Action{implicit request=>
    Ok(views.html.panels.personal())
  }

  def mytask=Action{implicit request=>
    Ok(views.html.panels.myTask())
  }

  def allsoft(types:String)=Action{implicit request=>
    Ok(views.html.panels.allsofts(types))
  }

  def updatenews=Action{implicit request=>
    Ok(views.html.panels.news())
  }

  def faq=Action{implicit request=>
    Ok(views.html.panels.faq())
  }

  def manager=Action{implicit request=>
    Ok(views.html.panels.manager())
  }

  def notelist=Action{implicit request=>
    Ok(views.html.panels.noteList())
  }

  def feedback=Action{implicit request=>
    Ok(views.html.panels.feedbak())
  }

  //test below
  def viewer= Action {implicit request=>
   Ok(views.html.service.pdfviewer())
  }

  def test= Action {implicit request=>
    Ok(views.html.test.header())
  }



}
