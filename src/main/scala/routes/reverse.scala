package org.cheminot.router

import rapture.net._
import org.cheminot.Config

object Reverse {

  object Api {

    def search(implicit config: Config): HttpUrl =
      Http.parse(s"http://${config.domain}/api/trips/search")
  }
}
