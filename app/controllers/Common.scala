package controllers

import scala.concurrent.Future
import play.api.mvc.{ Session => PlaySession, _ }
import models.Config

case class Ctx(sessionId: String)

object Common {

  case class RequestWithCtx[A](ctx: Ctx, request: Request[A]) extends WrappedRequest(request)

  case class RequestWithMaybeCtx[A](maybeCtx: Option[Ctx], request: Request[A]) extends WrappedRequest(request)

  def WithCtx(block: RequestWithCtx[AnyContent] => Future[Result]) = Action.async { req =>
    Session.get(req) match {
      case Some(sessionId) => block(RequestWithCtx(Ctx(sessionId), req))
      case _ => Future successful Results.Unauthorized
    }
  }

  def WithMaybeCtx(block: RequestWithMaybeCtx[AnyContent] => Future[Result]) = Action.async { req =>
    Session.get(req) match {
      case Some(sessionId) => block(RequestWithMaybeCtx(Some(Ctx(sessionId)), req))
      case _ => block(RequestWithMaybeCtx(None, req))
    }
  }

  object Session {

    def maxAge(implicit app: play.api.Application) =
      Some(Config.sessionDuration.toSeconds.toInt)

    val path = "/"
    val domain: Option[String] = None
    val secure = false
    val httpOnly = true

    def get(req: RequestHeader): Option[String] = {
      val session = PlaySession.decodeFromCookie(req.cookies.get(PlaySession.COOKIE_NAME))
      session.get("ID")
    }

    def clear() = Cookie(PlaySession.COOKIE_NAME, value = "", maxAge = Some(-1), domain = None)

    def create(sessionId: String)(implicit app: play.api.Application): Cookie = {
      val session = PlaySession(Map("ID" -> sessionId))
      val cookie = PlaySession.encode(PlaySession.serialize(session))
      Cookie(PlaySession.COOKIE_NAME, cookie, maxAge, path, domain, secure, httpOnly)
      PlaySession.encodeAsCookie(session)
    }
  }
}
