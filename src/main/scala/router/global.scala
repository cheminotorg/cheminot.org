package org.cheminot.web.router

import scala.util.{ Try, Success, Failure }
import rapture.http._
import org.cheminot.misc
import org.cheminot.web.{Logger, Config, pages}

object Global {

  def badRequest(message: String = "") =
    ErrorResponse(400, Nil, "Bad Request", message) //TODO

  def notFound(message: String = "") = 
    ErrorResponse(404, Nil, "Error 404 - Not found", message)

  private def notFoundHandler: PartialFunction[HttpRequest, Response] = {
    case _ => notFound()
  }

  def catchError(route: PartialFunction[HttpRequest, Response])(implicit config: Config): PartialFunction[HttpRequest, Response] = {
    case request =>
      Try(route.orElse(notFoundHandler)(request)) match {
        case Success(response) =>
          response
        case Failure(e) =>
          e.printStackTrace
          Logger.error(e.getMessage, e)
          misc.mailer.Mailer.sendException(e)(request, config.mailgun)
          pages.InternalServerError()
      }
  }
}
