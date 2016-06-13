package org.cheminot.web.api

import org.joda.time.DateTime
import rapture.json._, jsonBackends.jawn._
import rapture.html._, htmlSyntax.{ Option => HOption, _ }
import rapture.net.HttpQuery
import org.cheminot.misc
import org.cheminot.web.{ router, Params, Config }

object Trips {

  def renderJson(params: Params.FetchTrips, trips: List[Trip])(implicit config: Config): Json = {

    val previousLink = if (trips.isEmpty) json"null" else {
      Json(buildPreviousLink(params, trips).toString)
    }

    val nextLink = if (trips.isEmpty) json"null" else {
      Json(buildNextLink(params, trips).toString)
    }

    val results = Trip.toJsonSeq(trips)
    val json = JsonBuffer.empty

    json.previous = previousLink
    json.next = nextLink
    json.results = results

    json.as[Json]
  }

  def renderHtml(params: Params.FetchTrips, trips: List[Trip])(implicit config: Config): HtmlDoc = {

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
          (trips.map { trip =>
            Section(
              H2(s"${trip.id}"),
              Table(
                Thead(
                  Tr(Td("id"), Td("name"), Td("lat"), Td("lng"), Td("arrival"), Td("departure"))
                ),
                Tbody(
                  Tr,
                  trip.stopTimes.map { stopTime =>
                    Tr(
                      Td(stopTime.id),
                      Td(stopTime.name),
                      Td(stopTime.lat.toString),
                      Td(stopTime.lng.toString),
                      Td(misc.DateTime.format(stopTime.arrival)),
                      stopTime.departure.map(misc.DateTime.format).map(Td(_)).getOrElse(Td("N/A"))
                    )
                  }:_*
                )
              )
            )
          } :+ navigation):_*
        )
      )
    }
  }

  private def link(params: Params.FetchTrips, at: Option[DateTime], previous: Boolean)(implicit config: Config): HttpQuery = {
    router.Reverse.Api.search(
      vs = Option(params.vs),
      ve = Option(params.ve),
      at = at,
      limit = params.limit,
      previous = previous,
      json = params.json
    )
  }

  private def buildPreviousLink(params: Params.FetchTrips, trips: List[Trip])(implicit config: Config): HttpQuery = {
    val at = trips.headOption.flatMap(_.departure)
    link(params, at, previous = true)
  }

  private def buildNextLink(params: Params.FetchTrips, trips: List[Trip])(implicit config: Config): HttpQuery = {
    val at = trips.lastOption.flatMap(_.departure)
    link(params, at, previous = false)
  }
}
