package org.cheminot.router

import rapture.uri._
import rapture.http._
import RequestExtractors._
import org.cheminot.pages

object Site {

  def handle: PartialFunction[HttpRequest, Response] = {
    case Path(^) =>
      pages.Home()
  }
}
