package org.cheminot.api

import org.joda.time.DateTime
import rapture.json._, jsonBackends.jackson._
import rapture.html._, htmlSyntax.{ Option => HOption, _ }
import rapture.net.HttpUrl
import org.cheminot.Params
import org.cheminot.misc
import org.cheminot.router
import org.cheminot.Config

object Trips {

  def renderJson(params: Params.FetchTrips, trips: List[Trip])(implicit config: Config): Json = {
    json"""
         {
           "previous": ${previousLink(params, trips)},
           "next": ${nextLink(params, trips)},
           "results": ${trips.map(Trip.toJson)}
         }
        """
  }

  def renderHtml(params: Params.FetchTrips, trips: List[Trip])(implicit config: Config): HtmlDoc = {
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
          } :+
            P(
              A(href = previousLink(params, trips))("previous"),
              " # ",
              A(href = nextLink(params, trips))("next")
            )):_*
        )
      )
    }
  }

  private def link(params: Params.FetchTrips, at: Option[DateTime], previous: Boolean)(implicit config: Config): HttpUrl =
    router.Reverse.Api.search(
      ref = Option(params.ref),
      vs = Option(params.vs),
      ve = Option(params.ve),
      at = at,
      limit = params.limit,
      previous = previous,
      json = params.json
    )

  private def previousLink(params: Params.FetchTrips, trips: List[Trip])(implicit config: Config): HttpUrl = {
    val at = trips.headOption.flatMap(_.departure)
    link(params, at, previous = true)
  }

  private def nextLink(params: Params.FetchTrips, trips: List[Trip])(implicit config: Config): HttpUrl = {
    val at = trips.lastOption.flatMap(_.departure)
    link(params, at, previous = false)
  }
}
