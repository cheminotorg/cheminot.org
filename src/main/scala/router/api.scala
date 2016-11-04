package org.cheminot.web.router

import rapture.uri._
import rapture.http._
import org.cheminot.web.{ api, storage, Config, Params }

object Api {

  def handle(implicit config: Config): PartialFunction[HttpRequest, Response] =
    handleApiEntry {
      api.models.ApiEntry(storage.Meta.fetch())
    } orElse
    handleFetchTrips { params =>
      storage.Trips.fetch(params).map(api.models.Trip.apply)
    } orElse
    handleSearchTrips { params =>
      val trips = if(params.previous) {
        storage.Trips.searchPrevious(params)
      } else {
        storage.Trips.searchNext(params)
      }
      trips.map(api.models.Trip.apply)
    }

  private def handleApiEntry(fetch: => api.models.ApiEntry)(implicit config: Config): PartialFunction[HttpRequest, Response] = {
    htmlJsonHandler(^ / "api")(Params.ignore) { _ =>
      api.Entry.renderHtml(fetch)
    } { _ =>
      api.Entry.renderJson(fetch)
    }
  }

  private def handleFetchTrips(fetch: Params.FetchTrips => List[api.models.Trip])(implicit config: Config): PartialFunction[HttpRequest, Response] =
    htmlJsonHandler(^ / "api" / "trips")(Params.FetchTrips.fromRequest) { params =>
      api.FetchTrips.renderHtml(params, fetch(params))
    } { params =>
      api.FetchTrips.renderJson(params, fetch(params))
    }

  private def handleSearchTrips(fetch: Params.SearchTrips => List[api.models.Trip])(implicit config: Config): PartialFunction[HttpRequest, Response] =
    htmlJsonHandler(^ / "api" / "trips" / "search")(Params.SearchTrips.fromRequest) { params =>
      api.SearchTrips.renderHtml(params, fetch(params))
    } { params =>
      api.SearchTrips.renderJson(params, fetch(params.copy(json = true)))
    }
}
