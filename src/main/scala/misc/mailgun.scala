package org.cheminot.web.misc

import rapture.net._
import org.cheminot.web.MailgunConfig

object Mailgun {

  def send(mail: Mail)(implicit config: MailgunConfig): Unit = {
    val body = Map(
      'from -> config.from,
      'to -> config.to,
      'subject -> mail.subject,
      'html -> mail.content
    )
    withAuthentication { implicit basicAuthentication =>
      config.endpoint.httpPost(body)
    }(config)
  }

  private def withAuthentication[A](f: HttpBasicAuthentication => A)(implicit config: MailgunConfig): A =
    f(new HttpBasicAuthentication(Option(config.username -> config.password)))
}

case class Mail(
  subject: String,
  content: String
)
