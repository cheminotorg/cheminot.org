package org.cheminot.api

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import rapture.json._, jsonBackends.jackson._
import rapture.html._, htmlSyntax._

object Entry {

  def formatDateTime(dateTime: DateTime): String =
    ISODateTimeFormat.dateTime.print(dateTime)

  def renderJson(apiEntry: ApiEntry): Json = {
    json"""
        {
          "ref": ${apiEntry.ref},
          "buildAt": ${formatDateTime(apiEntry.buildDate)},
          "subsets": ${apiEntry.subsets.map(Subset.toJson)}
        }
        """
  }

  def renderHtml(apiEntry: ApiEntry): HtmlDoc = HtmlDoc {
    Html(Body(H1("Api")))
  }
}
