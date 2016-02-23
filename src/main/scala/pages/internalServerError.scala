package org.cheminot.web.pages

import rapture.html._, htmlSyntax._

object InternalServerError {

  def apply(): HtmlDoc =
    Layout("cheminot.org")(H1("500 - Internal server error"))
}
