package org.cheminot.api

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import rapture.json._, jsonBackends.jackson._
import rapture.html._, htmlSyntax._
import rapture.codec._
import org.cheminot.Config
import org.cheminot.router
import org.cheminot.misc

object Entry {

  def renderJson(apiEntry: ApiEntry)(implicit config: Config): Json = {
    json"""
        {
          "ref": ${apiEntry.ref},
          "buildAt": ${misc.DateTime.format(apiEntry.buildDate)},
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
          (Div("Build date: ", misc.DateTime.format(apiEntry.buildDate)) +:
            H2("Subsets") +:
            apiEntry.subsets.map { subset =>
              Section(
                H3(subset.name),
                Dl(
                  Dt("Updated date"),
                  Dd(subset.updatedDate.map(misc.DateTime.format).getOrElse("N/A")),
                  Dt("Start date"),
                  Dd(subset.startDate.map(misc.DateTime.format).getOrElse("N/A")),
                  Dt("End date"),
                  Dd(subset.endDate.map(misc.DateTime.format).getOrElse("N/A"))
                )
              )
            } :+
            H2("Search") :+
            Form(
              name = 'trips,
              method = "GET",
              action = router.Reverse.Api.search()
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
                  Input(typ = "text", name='at, value=misc.DateTime.format(DateTime.now), required=true)
                ),
                P(
                  Label(`for` = 'limit)("limit"),
                  Input(typ = "number", name='limit, value="10")
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
