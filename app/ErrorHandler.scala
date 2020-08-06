import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent._
import javax.inject.Singleton

@Singleton
class ErrorHandler extends HttpErrorHandler {
  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    if(statusCode== play.api.http.Status.NOT_FOUND){
      Future.successful(NotFound(views.html.errors.error404(statusCode.toString,"该页面地址不存在！")))
    }else{
      Future.successful(NotFound(views.html.errors.error404(statusCode.toString,message)))
    }
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    Future.successful(
      InternalServerError("A server error occurred: " + exception.getMessage)
    )
  }
}
