import scala.concurrent.Future
import play.api.mvc._
import play.api._
import cheminotorg._

object Global extends GlobalSettings {

  override def beforeStart(app: Application) {

    System.load(Config.cheminotcPath(app))
  }

  override def onStart(app: Application) {

    Config.print(app)

    CheminotDB.clean()(app)

    cheminotm.Tasks.init(Config.graphPath(app), Config.calendardatesPath(app))

    if(Play.mode(app) == Mode.Prod) {

      monitor.Tasks.init(app)

    }
  }

  override def onHandlerNotFound(request: RequestHeader) = Future successful {

    implicit val app = Play.current

    implicit val rq = request

    Results.Ok(views.html.badresponse(404))
  }

  override def onError(request: RequestHeader, e: Throwable): Future[Result] = {

    implicit val app = Play.current

    implicit val rq = request

    Mailer.sendException(e)

    if (Play.mode == Mode.Dev) {

      super.onError(request, e)

    }

    Future successful Results.Ok(views.html.badresponse(500))
  }
}
