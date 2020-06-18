package controllers

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

  def personal=Action{implicit request=>
    Ok(views.html.customer.personal())
  }

  def mytask=Action{implicit request=>
    Ok(views.html.task.myTask())
  }

  def allsoft(types:String)=Action{implicit request=>
    Ok(views.html.category.allsofts(types))
  }








  //test below

  def test= Action {implicit request=>
    Ok(views.html.test.soft())
  }

  def testlog= Action {implicit request=>
    Ok(views.html.test.header())
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


}
