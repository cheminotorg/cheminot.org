package org.cheminot.web

import org.joda.time.DateTime
import rapture.http.HttpRequest
import org.cheminot.misc

sealed trait Params

object Params {

  def getBoolean(request: HttpRequest)(param: Symbol) =
    request.param(param) map {
      case "true" | "1" => true
      case _ => false
    }

  def ignore(request: HttpRequest) = Some(new Params {})

  case object FetchApiEntry extends Params

  case class SearchTrips(
    vs: String,
    ve: String,
    at: DateTime,
    limit: Option[Int],
    previous: Boolean,
    json: Boolean = false
  ) extends Params

  object SearchTrips {

    def fromRequest(request: HttpRequest): Option[SearchTrips] = {
      for {
        vs <- request.param('vs)
        ve <- request.param('ve)
        at <- request.param('at).flatMap(misc.DateTime.parse)
      } yield {
        val limit = request.param('limit).map(_.toInt)
        val previous = request.param('previous).map(_.toBoolean) getOrElse false
        SearchTrips(ve, ve, at, limit, previous)
      }
    }
  }

  case class FetchTrips(
    vs: String,
    ve: String,
    monday: Option[Boolean],
    tuesday: Option[Boolean],
    wednesday: Option[Boolean],
    thursday: Option[Boolean],
    friday: Option[Boolean],
    saturday: Option[Boolean],
    sunday: Option[Boolean]
  ) extends Params

  object FetchTrips {

    def fromRequest(request: HttpRequest): Option[FetchTrips] = {
      for {
        vs <- request.param('vs)
        ve <- request.param('ve)
      } yield {
        val booleanParam = getBoolean(request) _
        val monday = booleanParam('monday)
        val tuesday = booleanParam('tuesday)
        val wednesday = booleanParam('wednesday)
        val thursday = booleanParam('thursday)
        val friday = booleanParam('friday)
        val saturday = booleanParam('saturday)
        val sunday = booleanParam('sunday)

        Params.FetchTrips(vs, ve, monday, tuesday, wednesday, thursday, friday, saturday, sunday)
      }
    }
  }
}
