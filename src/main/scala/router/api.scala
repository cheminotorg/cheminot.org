package org.cheminot.web.router

import rapture.uri._
import rapture.http._, requestExtractors._
import rapture.codec._
import rapture.mime._
import rapture.json._, jsonBackends.jawn._
import encodings.`UTF-8`._
import org.cheminot.web.{ api, storage, Config, Params }

object Api {

  val atParam = getParam('at)
  val vsParam = getParam('vs)
  val veParam = getParam('ve)

  def handle(implicit config: Config): PartialFunction[HttpRequest, Response] =
    handleApiEntry orElse
    handleSearchTrips { params =>
      val trips = if(params.previous) {
        storage.Trips.searchPrevious(params)
      } else {
        storage.Trips.searchNext(params)
      }
      trips.map(api.models.Trip(_, params.at)).sortBy(_.stopTimes.head.arrival.getMillis)
    } orElse
    handleSearchDepartureTimes { params =>
      storage.DepartureTimes.search(params).map(api.models.DepartureTime.apply)
    }

  private def formatJson(json: Json): String = {
    import rapture.json.formatters.compact._
    import rapture.core.decimalFormats.exact._
    Json.format(json)
  }

  private def handleApiEntry(implicit config: Config): PartialFunction[HttpRequest, Response] = {
    case req@Path(^ / "api") =>
      val apiEntry = api.models.ApiEntry(storage.Meta.fetch())
      ContentNegotiation(req) {
        case MimeTypes.`application/json` =>
          formatJson(api.Entry.renderJson(apiEntry))
        case _ =>
          api.Entry.renderHtml(apiEntry)
      }

    case req@Path(^ / "api.json") =>
      val apiEntry = api.models.ApiEntry(storage.Meta.fetch())
      formatJson(api.Entry.renderJson(apiEntry))
  }

  private def handleSearchTrips(fetch: Params.SearchTrips => List[api.models.Trip])(implicit config: Config): PartialFunction[HttpRequest, Response] = {
    case req@Path(^ / "api" / "trips" / "search.json") ~ vsParam(vs) ~ veParam(ve) ~ atParam(AsDateTime(at)) =>
      val limit = req.param('limit).map(_.toInt)
      val previous = req.param('previous).map(_.toBoolean) getOrElse false
      val params = Params.SearchTrips(vs, ve, at, limit, previous, json = true)
      formatJson(api.Trips.renderJson(params, fetch(params)))

    case req@Path(^ / "api" / "trips" / "search") ~ vsParam(vs) ~ veParam(ve) ~ atParam(AsDateTime(at)) =>
      val limit = req.param('limit).map(_.toInt)
      val previous = req.param('previous).map(_.toBoolean) getOrElse false
      ContentNegotiation(req) {
        case MimeTypes.`application/json` =>
          val params = Params.SearchTrips(vs, ve, at, limit, previous, json = true)
          formatJson(api.Trips.renderJson(params, fetch(params)))
        case _ =>
          val params = Params.SearchTrips(vs, ve, at, limit, previous)
          api.Trips.renderHtml(params, fetch(params))
      }
  }

  private def handleSearchDepartureTimes(fetch: Params.SearchDepartureTimes => List[api.models.DepartureTime])(implicit config: Config): PartialFunction[HttpRequest, Response] = {
    val buildParams = (request: HttpRequest, vs: String, ve: String) => {
      val booleanParam = getBooleanParam(request)(_)
      val monday = booleanParam('monday)
      val tuesday = booleanParam('tuesday)
      val wednesday = booleanParam('wednesday)
      val thursday = booleanParam('thursday)
      val friday = booleanParam('friday)
      val saturday = booleanParam('saturday)
      val sunday = booleanParam('sunday)
      Params.SearchDepartureTimes(vs, ve, monday, tuesday, wednesday,
        thursday, friday, saturday, sunday)
    }

    val handle: PartialFunction[HttpRequest, Response] = {
      case req@Path(^ / "api" / "departures" / "search.json") ~ vsParam(vs) ~ veParam(ve) =>
        api.DepartureTimes.renderJson(fetch(buildParams(req, vs, ve)))

      case req@Path(^ / "api" / "departures" / "search") ~ vsParam(vs) ~ veParam(ve)  =>
        ContentNegotiation(req) {
          case MimeTypes.`application/json` =>
            val params = buildParams(req, vs, ve)
            api.DepartureTimes.renderJson(fetch(params))

          case _ =>
            val params = buildParams(req, vs, ve)
            api.DepartureTimes.renderHtml(fetch(params))
        }
    }
    handle
  }
}
