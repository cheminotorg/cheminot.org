package org.cheminot.web.router

import org.joda.time.DateTime
import rapture.net._
import org.cheminot.misc
import org.cheminot.web.Config

object Reverse {

  object Api {

    def search(
      vs: Option[String] = None,
      ve: Option[String] = None,
      at: Option[DateTime] = None,
      limit: Option[Int] = None,
      previous: Boolean = false,
      json: Boolean = false
    )(implicit config: Config): HttpQuery = {
      val params = Map(
        "vs" -> vs,
        "ve" -> ve,
        "at" -> at.map(misc.DateTime.format),
        "limit" -> limit.map(_.toString),
        "previous" -> Option(previous.toString)
      ).collect {
        case (name, Some(value)) =>
          name -> value
      }
      val format = if (json) ".json" else ""
      val url = Http.parse(s"http://${config.domain}/api/trips/search${format}")
      url.query(params)
    }
  }
}
