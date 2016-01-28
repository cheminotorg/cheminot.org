package org.cheminot.api

import rapture.json._, jsonBackends.jackson._
import rapture.html._, htmlSyntax._

object Trips {

  def renderJson(trips: List[Trip]): Json = {
    json"""{ "results": ${trips} }"""
  }

  def renderHtml(trips: List[Trip]): HtmlDoc = HtmlDoc {
    Html(Body(H1("Trips")))
  }
}
