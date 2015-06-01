package models

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import akka.actor.{ Actor, ActorRef, Props }
import play.twirl.api.Html
import play.api.Application
import play.api.mvc.RequestHeader
import play.api.libs.concurrent.Akka
import play.api.{ Play, Mode }
import play.twirl.api.Html

object Mailer {

  private var maybeRef: Option[ActorRef] = None

  def send(subject: String, html: Html)(implicit app: Application) {
    ref ! AddUp(Mail(subject, html))
  }

  def sendException(e: Throwable)(implicit request: RequestHeader, app: Application) {
    send(s"[cheminot.org] [${request.method} ${request.path}] ${e.getMessage}", views.html.mails.exception(e))
  }

  def prop(implicit app: Application) =
    Props(classOf[MailerActor], app)

  def ref(implicit app: Application) = maybeRef.getOrElse {
    val r = Akka.system.actorOf(prop(app), "mailer")
    Akka.system.scheduler.schedule(0 milliseconds, models.Config.mailerPeriod, r, Squash)
    maybeRef = Some(r)
    r
  }

  case class Mail(subject: String, html: Html)

  sealed trait Message
  case class Send(mail: Mail) extends Message
  case class AddUp(mail: Mail) extends Message
  case object Squash extends Message
}

class MailerActor(implicit app: Application) extends Actor {
  import Mailer._

  var mails: List[Mail] = Nil

  def receive = {

    case Mailer.Send(Mail(subject, html)) if Play.mode(app) == Mode.Prod =>
      misc.Mailgun.send(models.Config.mailgunFrom, models.Config.mailgunTo, subject, html)

    case Mailer.AddUp(mail) =>
      mails = mail +: mails

    case Mailer.Squash =>
      mails.groupBy(_.subject).foreach {
        case (_, grouped) => grouped.headOption.map { mail =>
          val count = if(grouped.size > 1) s"[${grouped.size} times] " else ""
          val squashed = mail.copy(subject=count + mail.subject)
          self ! Mailer.Send(squashed)
          mails = Nil
        }
      }
  }
}
