package org.cheminot.web.misc

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import rapture.core._
import rapture.http._
import org.cheminot.web.{Config, MailgunConfig}
import org.cheminot.web.pages
import org.cheminot.web.log.Logger

object Mailer {

  sealed trait Msg
  case class AddUp(mail: Mail) extends Msg
  case class Squash(config: MailgunConfig) extends Msg

  def init(config: Config) =
    Scheduler.schedule(config.mailer.delay, config.mailer.period, actor.cue(Squash(config.mailgun)))

  private val actor = {
    Actor.of[Msg](Seq.empty[Mail]) {
      case Transition(Squash(config), mails) if(!mails.isEmpty) =>
        mails.groupBy(_.subject) map {
          case (_, grouped) if !grouped.isEmpty =>
            val mail = grouped.head
            val count = if(grouped.size > 1) s"[${grouped.size} times] " else ""
            val squashed = mail.copy(subject=count + mail.subject)
            scala.util.Try(Mailgun.send(squashed)(config)).recover {
              case e: Exception =>
                Logger.error("Unable to send mail", e)
            }
          case _ =>
        }
        Update(Unit, Seq.empty)

      case Transition(AddUp(mail), mails) =>
        Update(Unit, mail +: mails)

      case Transition(_, mails) =>
        Update(Unit, mails)
    }
  }

  def send(subject: String, message: String)(implicit config: MailgunConfig): Unit = {
    actor.cue(AddUp(Mail(subject, message)))
  }

  def sendException(e: Throwable)(implicit request: HttpRequest, config: MailgunConfig) =
    send(s"[cheminot.web] [${request.requestMethod.toString} ${request.basePathString}] ${e.getMessage}", pages.Exception(e).toString)
}
