package org.cheminot.router

import org.joda.time.DateTime
import rapture.net._
import org.cheminot.misc
import org.cheminot.Config

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
      ref: Option[String] = None,
      vs: Option[String] = None,
      ve: Option[String] = None,
      at: Option[DateTime] = None,
      limit: Option[Int] = None,
      previous: Boolean = false
    )(implicit config: Config): HttpUrl = {
      val params = buildParams(List(
        'ref -> ref,
        'vs -> vs,
        've -> ve,
        'at -> at.map(misc.DateTime.format),
        'limit -> limit.map(_.toString),
        'previous -> Option(previous.toString))
      )
      Http.parse(s"http://${config.domain}/api/trips/search").query(params)
    }
  }
}
