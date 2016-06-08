package org.cheminot.web.router

import scala.util.{ Try, Success, Failure }
import rapture.http._
import org.cheminot.web.Config
import org.cheminot.web.pages
import org.cheminot.web.misc.Mailer
import org.cheminot.web.log.Logger

object Global {

  def notFound: PartialFunction[HttpRequest, Response] = {
    case _ =>
      pages.NotFound()
  }

  def catchError(route: PartialFunction[HttpRequest, Response])(implicit config: Config): PartialFunction[HttpRequest, Response] = {
    case request =>
      Try(route.orElse(notFound)(request)) match {
        case Success(response) =>
          response
        case Failure(e) =>
          Logger.error(e.getMessage, e)
          Mailer.sendException(e)(request, config.mailgun)
          pages.InternalServerError()
      }
  }
}
