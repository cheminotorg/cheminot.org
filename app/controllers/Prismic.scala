package controllers

import play.api._
import play.api.mvc._

import scala.concurrent._
import play.api.libs.concurrent.Execution.Implicits._

import Play.current

import io.prismic._


trait PrismicController {
  self: Controller =>

  def PrismicAction(block: PrismicHelper.Request[AnyContent] => Future[Result]): Action[AnyContent] =
    bodyAction(parse.anyContent)(block)

  def bodyAction[A](bodyParser: BodyParser[A])(block: PrismicHelper.Request[A] => Future[Result]) =
    Action.async(bodyParser) { implicit request =>
      for {
        ctx <- PrismicHelper.buildContext()
        result <- block(PrismicHelper.Request(request, ctx))
      } yield result
    }
}

object PrismicHelper {

  private val ACCESS_TOKEN = "ACCESS_TOKEN"

  private val Cache = BuiltInCache(200)

  private val Logger = (level: Symbol, message: String) => level match {
    case 'DEBUG => play.api.Logger("prismic").debug(message)
    case 'ERROR => play.api.Logger("prismic").error(message)
    case _      => play.api.Logger("prismic").info(message)
  }

  case class Request[A](request: play.api.mvc.Request[A], ctx: Either[Exception, Ctx]) extends WrappedRequest(request)

  case class Ctx(api: Api, ref: String, accessToken: Option[String], linkResolver: DocumentLinkResolver) {
    def hasPrivilegedAccess = accessToken.isDefined
  }

  def buildContext()(implicit request: RequestHeader): Future[Either[Exception, Ctx]] = {
    val token = request.session.get(ACCESS_TOKEN).orElse(models.Config.prismicToken)
    apiHome(token) map { apiOrError =>
      apiOrError.right.map { api =>
        val ref = {
          request.cookies.get(Prismic.experimentsCookie) map (_.value) flatMap api.experiments.refFromCookie orElse {
            request.cookies.get(Prismic.previewCookie) map (_.value)
          }
        } getOrElse api.master.ref
        Ctx(api, ref, token, Application.linkResolver(api)(request))
      }
    }
  }

  def apiHome(token: Option[String] = None): Future[Either[Exception, io.prismic.Api]] = {
    try {
      Api.get(models.Config.prismicApi, accessToken = token, cache = Cache, logger = Logger).map(Right(_))
    } catch {
      case e: Exception => Future successful Left(e)
    }
  }

  def getDocument(id: String)(implicit ctx: PrismicHelper.Ctx): Future[Option[Document]] =
    ctx.api.forms("everything")
      .query(Predicate.at("document.id", id))
      .ref(ctx.ref).submit() map (_.results.headOption)

  def getBookmark(bookmark: String)(implicit ctx: PrismicHelper.Ctx): Future[Option[Document]] =
    ctx.api.bookmarks.get(bookmark).map(id => getDocument(id)).getOrElse(Future.successful(None))

}
