package org.cheminot.site.pages

import rapture.html._, htmlSyntax._

object Home {

  def apply(): HtmlDoc =
    Layout("cheminot.org")(H1("cheminot.org"))
}
