import scala.concurrent.Future
import play.api.mvc._
import play.api._

object Global extends GlobalSettings {

  override def beforeStart(app: Application) {

    models.Config.print(app)

    System.load(models.Config.cheminotcPath(app))

    models.CheminotDb.clean()(app)
  }

  override def onHandlerNotFound(request: RequestHeader) = Future successful {

    implicit val app = Play.current

    implicit val rq = request

    Results.Ok(views.html.badresponse(404))
  }

  override def onError(request: RequestHeader, e: Throwable): Future[Result] = {

    implicit val app = Play.current

    implicit val rq = request

    models.Mailer.sendException(e)

    if (Play.mode == Mode.Dev) {

      super.onError(request, e)

    }

    Future successful Results.Ok(views.html.badresponse(500))

  }
}
