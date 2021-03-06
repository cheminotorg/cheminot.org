package org.cheminot.web.router

import rapture.uri._
import rapture.http._
import requestExtractors._
import org.cheminot.web.pages

object Site {

  def handle: PartialFunction[HttpRequest, Response] = {
    case Path(^ / "") =>
      pages.Home()
  }
}
