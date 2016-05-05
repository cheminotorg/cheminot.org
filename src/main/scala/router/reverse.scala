package org.cheminot.web.router

import org.joda.time.DateTime
import rapture.net._
import org.cheminot.web.misc
import org.cheminot.web.Config

object Reverse {

  private def buildParams(params: List[(Symbol, Option[String])]): Map[Symbol, String] =
    params.foldLeft(Map.empty[Symbol, String]) {
      case (acc, (name, Some(value))) =>
        acc + (name -> value)
      case (acc, (_, None)) =>
        acc
    }

  object Api {

    def search(
      vs: Option[String] = None,
      ve: Option[String] = None,
      at: Option[DateTime] = None,
      limit: Option[Int] = None,
      previous: Boolean = false,
      json: Boolean = false
    )(implicit config: Config): HttpUrl = {
      val params = buildParams(List(
        'vs -> vs,
        've -> ve,
        'at -> at.map(misc.DateTime.format),
        'limit -> limit.map(_.toString),
        'previous -> Option(previous.toString))
      )
      val queryString = params.foldLeft("") {
        case ("", (param, value)) =>
          s"?${param.name}=${value}"
        case (acc, (param, value)) =>
          s"${acc}&${param.name}=${value}"
      }
      val format = if (json) ".json" else ""
      Http.parse(s"http://${config.domain}/api/trips/search${format}${queryString}")
    }
  }
}
