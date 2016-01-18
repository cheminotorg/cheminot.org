package org.cheminot.router

import rapture.uri._
import rapture.http._
import RequestExtractors._
import rapture.codec._
import encodings.`UTF-8`._
import org.cheminot.api
import org.cheminot.storage

object Api {

  val refParam = getParam('ref)
  val atParam = getParam('ref)
  val vsParam = getParam('vs)
  val veParam = getParam('ve)

  def handle: PartialFunction[HttpRequest, Response] = {

    case Path(^ / "api") =>
      val meta = api.Meta(storage.Storage.fetchMeta())
      api.Entry.renderJson(meta)

    case Path(^ / "api" / "trips" / "search") ~ refParam(ref) ~ vsParam(vs) ~ veParam(ve) ~ refParam(AsDateTime(at)) =>
      val trips = storage.Storage.fetchNextTrips(ref, vs, ve, at).map(api.Trip.apply)
      api.Trips.renderJson(trips)
  }
}
