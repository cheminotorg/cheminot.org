package org.cheminot.web.pages

import org.apache.commons.lang3.exception.ExceptionUtils
import rapture.html._, htmlSyntax._
import rapture.http._
import rapture.dom._

object Exception {

  def apply(e: Throwable)(implicit request: HttpRequest): DomNode[_, _, _] =
    Div(
      H1(s"${request.requestMethod.toString} ${request.basePathString}]"),
      Div(ExceptionUtils.getRootCauseStackTrace(e).mkString("\n"))
    )
}
