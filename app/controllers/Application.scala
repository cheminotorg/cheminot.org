package controllers

import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits._

object Application extends Controller with PrismicController {

  def linkResolver(api: io.prismic.Api)(implicit request: RequestHeader) =
    io.prismic.DocumentLinkResolver(api) {
      case _ => routes.Application.index().absoluteURL()
    }

  def index = PrismicAction { implicit request =>

    request.ctx match {

      case Left(e) =>
        Future successful Ok(views.html.offline())

      case Right(prismicCtx) =>
        PrismicHelper.getBookmark("home")(prismicCtx).map {

          case Some(home) =>
            Ok(views.html.index(home, prismicCtx))

          case _ =>
            Ok(views.html.offline())
        }
    }
  }

  def preview(token: String) = PrismicAction { implicit req =>

    req.ctx match {

      case Right(prismicCtx) =>
        prismicCtx.api.previewSession(token, prismicCtx.linkResolver, routes.Application.index().url).map { redirectUrl =>
          Redirect(redirectUrl).withCookies(Cookie(io.prismic.Prismic.previewCookie, token, path = "/", maxAge = Some(30 * 60 * 1000), httpOnly = false))
        }

      case Left(e) =>
        Future successful Redirect(routes.Application.index)
    }
  }

  def triggerUnexpectedError = Action {
    throw new RuntimeException("No worry, this is just a simple test about error reporting.")
    Ok
  }

  def about = Action {
    Ok(Json.obj(
      "cheminotorg" -> cheminotorg.Config.version,
      "cheminotc" -> cheminotorg.Config.cheminotcVersion
    ))
  }
}
