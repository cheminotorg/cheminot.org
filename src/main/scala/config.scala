package org.cheminot.web

import scala.language.postfixOps
import scala.concurrent.duration._
import rapture.core._
import rapture.cli._
import modes.returnOption._
import rapture.net._
import rapture.uri._

case class Config(port: Int, domain: String, mailgun: MailgunConfig, mailer: MailerConfig)

object Config {

  val Port = New.Param[Int]('p', 'port)

  val Domain = New.Param[String]('d', 'domain)

  def apply(args: Array[String]): Config = {
    val params = New.ParamMap(args:_*)
    val port = Port.parse(params) getOrElse 8080
    val domain = Domain.parse(params) getOrElse "localhost:8080"
    val mailgun = MailgunConfig("", "", uri"http://google.fr", "", "")
    val mailer = MailerConfig(0 seconds, 10 seconds)
    Config(port = port, domain = domain, mailgun = mailgun, mailer = mailer)
  }
}

case class MailerConfig(
  delay: FiniteDuration,
  period: FiniteDuration
)

case class MailgunConfig(
  from: String,
  to: String,
  endpoint: HttpQuery,
  username: String,
  password: String
)
