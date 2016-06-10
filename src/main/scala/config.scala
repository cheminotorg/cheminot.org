package org.cheminot.web

import scala.language.postfixOps
import scala.concurrent.duration._
import rapture.core._
import rapture.cli._
import rapture.net._
import rapture.json._, jsonBackends.jawn._
import rapture.fs._
import rapture.io._
import rapture.codec._, encodings.`UTF-8`._
import org.cheminot.web.log.Logger

case class Config(
  port: Int,
  domain: String,
  mailgun: MailgunConfig,
  mailer: MailerConfig
)

object Config {

  val Port = New.Param[Int]('p', 'port)

  val Domain = New.Param[String]('d', 'domain)

  val ConfigFilePath = New.Param[String]('c', 'config)

  def apply(args: Array[String]): Config = {
    import modes.returnOption._
    val params = New.ParamMap(args:_*)
    val configFilePath = ConfigFilePath.parse(params).map(File.parse) getOrElse ConfigFile.defaultPath
    val configFile = ConfigFile.parse(configFilePath)
    val port = Port.parse(params) orElse configFile.port getOrElse 8080
    val domain = Domain.parse(params) orElse configFile.domain getOrElse "localhost:8080"
    Config(
      port = port,
      domain = domain,
      mailgun = configFile.mailgun,
      mailer = configFile.mailer
    )
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

case class ConfigFile(
  port: Option[Int],
  domain: Option[String],
  mailgun: MailgunConfig,
  mailer: MailerConfig
)

object ConfigFile {

  lazy val defaultPath: FsUrl = {
    val currentDir = new java.io.File("")
    File.parse(s"${currentDir.getAbsolutePath}/application.json")
  }

  def parse(path: FsUrl): ConfigFile = {
    Logger.info(s"Reading application.json ($path)")
    val file = File.parse(path.toString)
    val json = Json.parse(file.slurp[Char])
    val mailgun = json.mailgun
    val mailer = json.mailer
    ConfigFile(
      port = json.port.as[Option[Int]],
      domain = json.domain.as[Option[String]],
      mailgun = MailgunConfig(
        from = mailgun.from.as[String],
        to = mailgun.to.as[String],
        endpoint = HttpQuery.parse(mailgun.endpoint.as[String]),
        username = mailgun.username.as[String],
        password = mailgun.password.as[String]
      ),
      mailer = MailerConfig(
        delay = mailer.delay.as[Int] seconds,
        period = mailer.delay.as[Int] seconds
      )
    )
  }
}
