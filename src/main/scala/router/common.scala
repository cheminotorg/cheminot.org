package org.cheminot.web.router

import scala.util.{ Try, Success, Failure }
import rapture.http._
import org.cheminot.web.pages

object Common {

  def notFound: PartialFunction[HttpRequest, Response] = {
    case _ =>
      pages.NotFound()
  }

  def onError(route: PartialFunction[HttpRequest, Response]): PartialFunction[HttpRequest, Response] = {
    case request =>
      Try(route(request)) match {
        case Success(response) =>
          response
        case Failure(e) =>
          e.printStackTrace
          pages.InternalServerError()
      }
  }
}
