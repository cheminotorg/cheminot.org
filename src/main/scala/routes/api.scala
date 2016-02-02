package org.cheminot.router

import rapture.uri._
import rapture.http._, RequestExtractors._
import rapture.codec._
import rapture.mime._
import encodings.`UTF-8`._
import org.cheminot.Config
import org.cheminot.api
import org.cheminot.storage

object Api {

  val refParam = getParam('ref)
  val atParam = getParam('at)
  val vsParam = getParam('vs)
  val veParam = getParam('ve)

  def handle(implicit config: Config): PartialFunction[HttpRequest, Response] = {

    case req@Path(^ / "api") =>
      val apiEntry = api.ApiEntry(storage.Storage.fetchMeta())

      ContentNegotiation(req) {
        case MimeTypes.`application/json` =>
          api.Entry.renderJson(apiEntry)

        case _ =>
          api.Entry.renderHtml(apiEntry)
      }

    case req@Path(^ / "api" / "trips" / "search") ~ refParam(ref) ~ vsParam(vs) ~ veParam(ve) ~ atParam(AsDateTime(at)) =>
      val limit = req.param('limit).map(_.toInt)
      val trips = storage.Storage.fetchNextTrips(ref, vs, ve, at, limit).map(api.Trip.apply(_, at))

      ContentNegotiation(req) {
        case MimeTypes.`application/json` =>
          api.Trips.renderJson(trips)

        case _ =>
          api.Trips.renderHtml(trips)

      }
  }
}