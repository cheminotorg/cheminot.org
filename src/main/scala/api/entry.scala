package org.cheminot.web.api

import org.joda.time.DateTime
import rapture.json._, jsonBackends.jawn._
import rapture.html._, htmlSyntax._
import rapture.codec._
import org.cheminot.misc
import org.cheminot.web.{Config, router}

object Entry {

  def renderJson(apiEntry: models.ApiEntry)(implicit config: Config): Json = {
    val json = JsonBuffer.empty
    json.ref = apiEntry.ref
    json.toto = 1.233341341341413413414D
    json.buildAt = misc.DateTime.format(apiEntry.buildDate)
    json.subsets = apiEntry.subsets.map(models.Subset.toJson)
    json.as[Json]
  }

  def renderHtml(apiEntry: models.ApiEntry)(implicit config: Config): HtmlDoc = {
    HtmlDoc {
      Html(
        Head(
          Meta(charset = encodings.`UTF-8`()),
          Title("cheminot.org - api")
        ),
        Body(
          H1("cheminot.org - Api"),
          Section(
            P("Date de build: ", misc.DateTime.format(apiEntry.buildDate)),
            H2("Subsets"),
            apiEntry.subsets.map { subset =>
              Div(
                H3(subset.id),
                P(
                  Label("timestamp: "),
                  Span(misc.DateTime.format(subset.timestamp))
                )
              )
            }
          ),
          Section(
            H2("Rechercher des trajets"),
            Form(
              name = 'searchtrips,
              method = "GET",
              action = router.Reverse.Api.searchTrips()
            )(
              Fieldset(
                Legend("Trajet de:"),
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
          ),
          Section(
            H2("Récupérer des trajets"),
            Form(
              name = 'departures,
              method = "GET",
              action = router.Reverse.Api.fetchTrips()
            )(
              Fieldset(
                Legend("Horaires de départ pour:"),
                P(
                  Label(`for` = 'departure)("departure"),
                  Input(typ = "text", name='vs, required=true)
                ),
                P(
                  Label(`for` = 'arrival)("arrival"),
                  Input(typ="text", name='ve, required=true)
                ),
                P(
                  Label(`for` = 'monday)("Lundi"),
                  Input(typ = "checkbox", name='monday, value="true")
                ),
                P(
                  Label(`for` = 'tuesday)("Mardi"),
                  Input(typ = "checkbox", name='tuesday, value="true")
                ),
                P(
                  Label(`for` = 'wednesday)("Mercredi"),
                  Input(typ = "checkbox", name='wednesday, value="true")
                ),
                P(
                  Label(`for` = 'thursday)("Jeudi"),
                  Input(typ = "checkbox", name='thursday, value="true")
                ),
                P(
                  Label(`for` = 'friday)("Vendredi"),
                  Input(typ = "checkbox", name='friday, value="true")
                ),
                P(
                  Label(`for` = 'saturday)("Samedi"),
                  Input(typ = "checkbox", name='saturday, value="true")
                ),
                P(
                  Label(`for` = 'sunday)("Dimanche"),
                  Input(typ = "checkbox", name='sunday, value="true")
                ),
                Button(typ = "submit")("Submit")
              )
            )
          )
        )
      )
    }
  }
}
