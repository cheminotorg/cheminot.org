package misc

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import play.api.libs.ws.WS
import play.api.libs.ws.WSAuthScheme
import play.twirl.api.Html
import play.api.Application
import models.Config

object Mailgun {

  def send(from: String, to: String, subject: String, text: Html)(implicit app: Application): Future[Unit] = {

    WS.url(Config.mailgunEndpoint + "/messages")
      .withAuth(Config.mailgunUsername, Config.mailgunPassword, WSAuthScheme.BASIC)
      .post(Map(
        "from" -> Seq(from),
        "to" ->Seq(to),
        "subject" -> Seq(subject),
        "html" -> Seq(text.toString)
      )).map(_ => ())
  }
}
