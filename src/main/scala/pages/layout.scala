package org.cheminot.pages

import rapture.html._, htmlSyntax._
import rapture.dom._
import rapture.codec._
import rapture.uri.{Link => ULink, _}
object Layout {

  def apply(title: String)(main: DomNode[_ <: ElementType, Html5.Flow, _ <: AttributeType]*): HtmlDoc = HtmlDoc(
    Html(
      Head(
        Meta(charset = encodings.`UTF-8`()),
        Title(title),
        Meta(name = 'description, content = ""),
        Meta(name = 'author, content = ""),
        Meta(name = 'viewport, content = "width=device-width,initial-scale=1,maximum-scale=1"),
        Link(rel = stylesheet, href = ^ / "css" / "main.css"),
        Script(typ = "text/javascript", src = ^ / "script" / "main.js")
      ),
      Body(
        H1("cheminot.org"),
        main:_*
      )
    )
  )
}
