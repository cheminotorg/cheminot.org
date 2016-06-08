package org.cheminot.web

import rapture.core._
import rapture.http._, httpBackends.jetty._
import org.cheminot.web.log.Logger

object Main {

  def main(args: Array[String]): Unit = {

    implicit val config = Config(args)

    misc.Mailer.init(config)

    HttpServer.listen(config.port) {
      router.Global.catchError {
        router.Api.handle orElse
        router.Site.handle
      }
    }

    Logger.info(s"Listening on port ${config.port}")
  }
}
