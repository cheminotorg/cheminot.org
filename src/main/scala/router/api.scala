package org.cheminot.router

import rapture.uri._
import rapture.http._, RequestExtractors._
import rapture.codec._
import rapture.mime._
import encodings.`UTF-8`._
import org.cheminot.{ Config, Params }
import org.cheminot.api
import org.cheminot.storage

object Api {

  val atParam = getParam('at)
  val vsParam = getParam('vs)
  val veParam = getParam('ve)

  def handle(implicit config: Config): PartialFunction[HttpRequest, Response] =
    handleApiEntry orElse
    handleFetchTrips { params =>
      val trips = if(params.previous) {
        storage.Storage.fetchPreviousTrips(params)
      } else {
        storage.Storage.fetchNextTrips(params)
      }
      trips.map(api.Trip(_, params.at)).sortBy(_.stopTimes.head.arrival.getMillis)
    }

  private def handleApiEntry(implicit config: Config): PartialFunction[HttpRequest, Response] = {
    case req@Path(^ / "api") =>
      val apiEntry = api.ApiEntry(storage.Storage.fetchMeta())
      ContentNegotiation(req) {
        case MimeTypes.`application/json` =>
          api.Entry.renderJson(apiEntry)
        case _ =>
          api.Entry.renderHtml(apiEntry)
      }

    case req@Path(^ / "api.json") =>
      val apiEntry = api.ApiEntry(storage.Storage.fetchMeta())
      api.Entry.renderJson(apiEntry)
  }

  private def handleFetchTrips(fetch: Params.FetchTrips => List[api.Trip])(implicit config: Config): PartialFunction[HttpRequest, Response] = {

    case req@Path(^ / "api" / "trips" / "search.json") ~ vsParam(vs) ~ veParam(ve) ~ atParam(AsDateTime(at)) =>
      val limit = req.param('limit).map(_.toInt)
      val previous = req.param('previous).map(_.toBoolean) getOrElse false
      val params = Params.FetchTrips(vs, ve, at, limit, previous, json = true)
      api.Trips.renderJson(params, fetch(params))

    case req@Path(^ / "api" / "trips" / "search") ~ vsParam(vs) ~ veParam(ve) ~ atParam(AsDateTime(at)) =>
      val limit = req.param('limit).map(_.toInt)
      val previous = req.param('previous).map(_.toBoolean) getOrElse false
      ContentNegotiation(req) {
        case MimeTypes.`application/json` =>
          val params = Params.FetchTrips(vs, ve, at, limit, previous, json = true)
          api.Trips.renderJson(params, fetch(params))
        case _ =>
          val params = Params.FetchTrips(vs, ve, at, limit, previous)
          api.Trips.renderHtml(params, fetch(params))
      }
  }
}
