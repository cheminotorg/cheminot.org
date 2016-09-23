package org.cheminot.web

import org.joda.time.DateTime

object Params {

  case class SearchTrips(
    vs: String,
    ve: String,
    at: DateTime,
    limit: Option[Int],
    previous: Boolean,
    json: Boolean = false
  )

  case class SearchDepartureTimes(
    vs: String,
    ve: String,
    monday: Option[Boolean],
    tuesday: Option[Boolean],
    wednesday: Option[Boolean],
    thursday: Option[Boolean],
    friday: Option[Boolean],
    saturday: Option[Boolean],
    sunday: Option[Boolean]
  )
}
