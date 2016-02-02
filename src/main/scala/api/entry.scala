package org.cheminot.api

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import rapture.json._, jsonBackends.jackson._
import rapture.html._, htmlSyntax._
import rapture.codec._
import org.cheminot.Config
import org.cheminot.router

object Entry {

  def renderJson(apiEntry: ApiEntry)(implicit config: Config): Json = {
    json"""
        {
          "ref": ${apiEntry.ref},
          "buildAt": ${formatDateTime(apiEntry.buildDate)},
          "subsets": ${apiEntry.subsets.map(Subset.toJson)}
        }
        """
  }

  def renderHtml(apiEntry: ApiEntry)(implicit config: Config): HtmlDoc = {
    HtmlDoc {
      Html(
        Head(
          Meta(charset = encodings.`UTF-8`()),
          Title("cheminot.org - api")
        ),
        Body(
          H1("cheminot.org - Api"),
          (P("Build date: ", formatDateTime(apiEntry.buildDate)) +:
            H2("Subsets") +:
            apiEntry.subsets.map { subset =>
              Section(
                H3(subset.name),
                Dl(
                  Dt("Updated date"),
                  Dd(subset.updatedDate.map(formatDateTime).getOrElse("N/A")),
                  Dt("Start date"),
                  Dd(subset.startDate.map(formatDateTime).getOrElse("N/A")),
                  Dt("End date"),
                  Dd(subset.endDate.map(formatDateTime).getOrElse("N/A"))
                )
              )
            } :+
            H2("Search") :+
            Form(
              name = 'trips,
              method = "GET",
              action = router.Reverse.Api.search
            )(
              Fieldset(
                Legend("Trips"),
                P(
                  Label(`for` = 'name)("ref"),
                  Input(typ="text", name='ref, value=apiEntry.ref, required=true)
                ),
                P(
                  Label(`for` = 'departure)("departure"),
                  Input(typ = "text", name='vs, required=true)
                ),
                P(
                  Label(`for` = 'arrival)("arrival"),
                  Input(typ="text", name='ve, required=true)
                ),
                P(
                  Label(`for` = 'at)("at"),
                  Input(typ = "text", name='at, value=formatDateTime(DateTime.now), required=true)
                ),
                P(
                  Label(`for` = 'limit)("limit"),
                  Input(typ = "number", name='limit, value="10", required=true)
                ),
                Button(typ = "submit")("Submit")
              )
            )
          ):_*
        )
      )
    }
  }
}
