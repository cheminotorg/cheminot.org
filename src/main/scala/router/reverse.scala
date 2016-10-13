package org.cheminot.web.router

import org.joda.time.DateTime
import rapture.net._
import org.cheminot.misc
import org.cheminot.web.Config

object Reverse {

  object Api {

    def searchTrips(
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
        "previous" -> booleanParam(previous)
      ).collect {
        case (name, Some(value)) =>
          name -> value
      }
      val format = if (json) ".json" else ""
      val url = Http.parse(s"http://${config.domain}/api/trips/search${format}")
      url.query(params)
    }

    def searchDepartureTimes(
      vs: Option[String] = None,
      ve: Option[String] = None,
      monday: Boolean = false,
      tuesday: Boolean = false,
      wednesday: Boolean = false,
      thursday: Boolean = false,
      friday: Boolean = false,
      saturday: Boolean = false,
      sunday: Boolean = false,
      json: Boolean = false
    )(implicit config: Config): HttpQuery = {
      val params = Map(
        "vs" -> vs,
        "ve" -> ve,
        "monday" -> booleanParam(monday),
        "tuesday" -> booleanParam(tuesday),
        "wednesday" -> booleanParam(wednesday),
        "thursday" -> booleanParam(thursday),
        "friday" -> booleanParam(friday),
        "saturday" -> booleanParam(saturday),
        "sunday" -> booleanParam(sunday)
      ).collect {
        case (name, Some(value)) =>
          name -> value
      }
      val format = if (json) ".json" else ""
      val url = Http.parse(s"http://${config.domain}/api/departures/search${format}")
      url.query(params)
    }

    private def booleanParam(b: Boolean): Option[String] =
      if(b) Some("true") else None
  }
}
