package org.cheminot.web

import rapture.core._
import rapture.http._, httpBackends.jetty._
import org.cheminot.misc

object Main {

  def main(args: Array[String]): Unit = {

    implicit val config = Config(args)

    config.print

    misc.mailer.Mailer.init(config, onError = {
      case e: Exception =>
        println("on error")
        Logger.error(s"Unable to send email: ${e.getMessage}", e)
    })

    storage.Stations.initCache()

    HttpServer.listen(config.port) {
      router.Global.catchError {
        router.Api.handle orElse
        router.Site.handle
      }
    }

    Logger.info(s"Listening on port ${config.port}")
  }
}
