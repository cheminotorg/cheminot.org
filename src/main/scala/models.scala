package org.cheminot

import org.joda.time.DateTime

object Params {

  case class FetchTrips(
    ref: String,
    vs: String,
    ve: String,
    at: DateTime,
    limit: Option[Int],
    previous: Boolean,
    json: Boolean = false
  )
}
