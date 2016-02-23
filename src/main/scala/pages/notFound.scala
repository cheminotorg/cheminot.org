package org.cheminot.web.pages

import rapture.html._, htmlSyntax._

object NotFound {

  def apply(): HtmlDoc =
    Layout("cheminot.org")(H1("404 - Not found"))
}
