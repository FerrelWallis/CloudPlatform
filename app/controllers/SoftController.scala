package controllers

import javax.inject._
import play.api.mvc._

class SoftController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def pca=Action{implicit request=>
    Ok(views.html.soft.pca())
  }

  def heatmap=Action{implicit request=>
    Ok(views.html.soft.heatmap())
  }

  def go=Action{implicit request=>
    Ok(views.html.soft.go())
  }

  def kegg=Action{implicit request=>
    Ok(views.html.soft.kegg())
  }

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

  //test below
  def softpage=Action{implicit request=>
    Ok(views.html.soft.softPage())
  }

  def testsoft=Action {implicit request=>
    Ok(views.html.test.testsoft())
  }

}
