package org.cheminot

import org.joda.time.DateTime

object Params {

  case class FetchTrips(
    vs: String,
    ve: String,
    at: DateTime,
    limit: Option[Int],
    previous: Boolean,
    json: Boolean = false
  )
}
