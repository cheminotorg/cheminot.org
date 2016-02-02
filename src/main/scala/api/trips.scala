package org.cheminot.api

import rapture.json._, jsonBackends.jackson._
import rapture.html._, htmlSyntax._

object Trips {

  def renderJson(trips: List[Trip]): Json = {
    json"""
         {
           "results": ${trips.map(Trip.toJson)}
         }
        """
  }

  def renderHtml(trips: List[Trip]): HtmlDoc = HtmlDoc {
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
        trips.map { trip =>
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
                    Td(formatDateTime(stopTime.arrival)),
                    stopTime.departure.map(formatDateTime).map(Td(_)).getOrElse(Td("N/A"))
                  )
                }:_*
              )
            )
          )
        }:_*
      )
    )
  }
}
