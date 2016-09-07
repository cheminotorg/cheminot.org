package org.cheminot.web.api

import org.joda.time.Duration
import org.joda.time.format.{PeriodFormat, PeriodFormatterBuilder}
import rapture.json._, jsonBackends.jawn._
import rapture.html._, htmlSyntax._

import org.cheminot.web.{ Params, Config }

object DepartureTimes {

  private def formatDuration(duration: Duration): String = {
    val formatter = new PeriodFormatterBuilder()
      .printZeroAlways()
      .appendHours()
      .appendSeparator(":")
      .appendMinutes()
      .toFormatter();
    formatter.print(duration.toPeriod)
  }

  def renderJson(departureTimes: List[Duration])(implicit config: Config): Json = {
    val json = JsonBuffer.empty
    json.results = departureTimes.map(formatDuration)
    json.as[Json]
  }

  def renderHtml(departureTimes: List[Duration])(implicit config: Config): HtmlDoc = {
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
          H1("Departures"),
          Table(
            Thead(
              Tr(Td("Départs"))
            ),
            Tbody(
              Tr,
              departureTimes.map { departureTime =>
                Tr(Td(formatDuration(departureTime)))
              }
            )
          )
        )
      )
    }
  }
}
