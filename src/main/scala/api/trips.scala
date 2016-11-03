package org.cheminot.web.api

import org.joda.time.DateTime
import rapture.json._, jsonBackends.jawn._
import rapture.html._, htmlSyntax.{ Option => HOption, _ }
import rapture.net.HttpQuery
import org.cheminot.misc
import org.cheminot.web.{ router, Params, Config }

object SearchTrips {

  def renderJson(params: Params.SearchTrips, trips: List[models.Trip])(implicit config: Config): Json = {

    val previousLink = if (trips.isEmpty) json"null" else {
      Json(buildPreviousLink(params, trips).toString)
    }

    val nextLink = if (trips.isEmpty) json"null" else {
      Json(buildNextLink(params, trips).toString)
    }

    val results = models.Trip.json.writesSeq(trips)(models.StopTime.json.DateTimeFormat)
    val json = JsonBuffer.empty

    json.previous = previousLink
    json.next = nextLink
    json.results = results

    json.as[Json]
  }

  def renderHtml(params: Params.SearchTrips, trips: List[models.Trip])(implicit config: Config): HtmlDoc = {

    val navigation = if(trips.isEmpty) P else {
      val previousLink = A(href = buildPreviousLink(params, trips))("previous")
      val nextLink = A(href = buildNextLink(params, trips))("next")
      P(previousLink, " - ", nextLink)
    }

    HtmlDoc {
      Html(
        Head(
          Style(
            """
            table td {
              padding: 10px;
              border: 1px solid;
            }
          """
          )
        ),
        Body(
          H1("Trips"),
          (models.Trip.html.writesSeq(trips) :+ navigation):_*
        )
      )
    }
  }

  private def link(params: Params.SearchTrips, at: Option[DateTime], previous: Boolean)(implicit config: Config): HttpQuery = {
    router.Reverse.Api.searchTrips(
      vs = Option(params.vs),
      ve = Option(params.ve),
      at = at,
      limit = params.limit,
      previous = previous,
      json = params.json
    )
  }

  private def buildPreviousLink(params: Params.SearchTrips, trips: List[models.Trip])(implicit config: Config): HttpQuery = {
    val at = trips.headOption.toList.flatMap(_.stopTimes).collectFirst {
      case stopTime if stopTime.id == params.vs => stopTime.departure
    }.headOption.flatten
    link(params, at, previous = true)
  }

  private def buildNextLink(params: Params.SearchTrips, trips: List[models.Trip])(implicit config: Config): HttpQuery = {
    val at = trips.lastOption.toList.flatMap(_.stopTimes).collectFirst {
      case stopTime if stopTime.id == params.vs => stopTime.departure
    }.headOption.flatten
    link(params, at, previous = false)
  }
}

object FetchTrips {

  def renderJson(params: Params.FetchTrips, trips: List[models.Trip])(implicit config: Config): Json = {
    val results = models.Trip.json.writesSeq(trips)(models.StopTime.json.MinutesFormat)
    val json = JsonBuffer.empty
    json.results = results
    json.as[Json]
  }


  def renderHtml(params: Params.FetchTrips, trips: List[models.Trip])(implicit config: Config): HtmlDoc = {
    HtmlDoc {
      Html(
        Head(
          Style(
            """
            table td {
              padding: 10px;
              border: 1px solid;
            }
          """
          )
        ),
        Body(
          H1("Trips"),
          models.Trip.html.writesSeq(trips)
        )
      )
    }
  }
}
