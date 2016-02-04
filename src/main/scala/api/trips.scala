package org.cheminot.api

import org.joda.time.DateTime
import rapture.json._, jsonBackends.jackson._
import rapture.html._, htmlSyntax.{ Option => HOption, _ }
import rapture.net.HttpUrl
import org.cheminot.misc
import org.cheminot.router
import org.cheminot.Config

object Trips {

  def renderJson(trips: List[Trip]): Json = {
    json"""
         {
           "results": ${trips.map(Trip.toJson)}
         }
        """
  }

  def renderHtml(query: Query, trips: List[Trip])(implicit config: Config): HtmlDoc = {
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
              A(href = previousLink(query, trips))("previous"),
              " # ",
              A(href = nextLink(query, trips))("next")
            )):_*
        )
      )
    }
  }

  private def link(query: Query, at: Option[DateTime], previous: Boolean)(implicit config: Config): HttpUrl =
    router.Reverse.Api.search(
      ref = Option(query.ref),
      vs = Option(query.vs),
      ve = Option(query.ve),
      at = at,
      limit = query.limit,
      previous = previous
    )

  private def previousLink(query: Query, trips: List[Trip])(implicit config: Config): HttpUrl = {
    val at = trips.headOption.flatMap(_.departure)
    link(query, at, previous = true)
  }

  private def nextLink(query: Query, trips: List[Trip])(implicit config: Config): HttpUrl = {
    val at = trips.lastOption.flatMap(_.departure)
    link(query, at, previous = false)
  }
}
