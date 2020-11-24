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

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */


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

  //taskname
  def viewer= Action {implicit request=>
   Ok(views.html.service.pdfviewer())
  }

  def test= Action {implicit request=>
    Ok(views.html.test.header())
  }

  def testlog= Action {implicit request=>
    Ok(views.html.test.allsoft())
  }

  def testSlider=Action{implicit request=>
    Ok(views.html.test.myslider())
  }

  def testindex=Action{implicit request=>
    Ok(views.html.test.testindex())
  }

  def testperson=Action{implicit request=>
    Ok(views.html.test.personal())
  }

  def testtable=Action{implicit request=>
    Ok(views.html.test.testTable())

  }

  def SendPdf(file: File,headers:Headers): Result = {
    val lastModifiedStr = file.lastModified().toString
    val MimeType = "pdf"
    val byteArray = Files.readAllBytes(file.toPath)
    val ifModifiedSinceStr = headers.get(IF_MODIFIED_SINCE)
    if (ifModifiedSinceStr.isDefined && ifModifiedSinceStr.get == lastModifiedStr) {
      NotModified
    } else {
      Ok(byteArray).as(MimeType).withHeaders(LAST_MODIFIED -> file.lastModified().toString)
    }
  }


}
