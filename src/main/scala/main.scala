package org.cheminot.web

import rapture.core._
import rapture.http._, httpBackends.jetty._
import org.cheminot.misc

object Main {

  def main(args: Array[String]): Unit = {

    implicit val config = Config(args)

    misc.mailer.Mailer.init(config, onError = {
      case e: Exception => Logger.error(s"Unable to send email: ${e.getMessage}", e)
    })

    HttpServer.listen(config.port) {
      router.Global.catchError {
        router.Api.handle orElse
        router.Site.handle
      }
    }

    Logger.info(s"Listening on port ${config.port}")
  }
}
