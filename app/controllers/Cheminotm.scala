package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.mvc._
import play.api.libs.EventSource
import play.api.Play.current
import play.api.data._
import play.api.data.Forms._
import models.{ Config, CheminotDb }
//import play.api.libs.json._

object Cheminotm extends Controller {

  def init = Common.WithMaybeCtx { implicit request =>
    val sessionId = request.maybeCtx map(_.sessionId) getOrElse CheminotDb.Id.next
    cheminotm.CheminotcActor.init(sessionId, Config.graphPath, Config.calendardatesPath) map {
      case Right(meta) =>
        val result = Ok(meta)
        request.maybeCtx map (_ => result) getOrElse {
          result.withCookies(Common.Session.create(sessionId))
        }
      case _ => BadRequest
    }
  }

  def lookForBestTrip = Common.WithCtx { implicit request =>
    Form(tuple(
      "vsId" -> nonEmptyText,
      "veId" -> nonEmptyText,
      "at" -> nonEmptyText,
      "te" -> nonEmptyText,
      "max" -> nonEmptyText
    )).bindFromRequest.fold(
      error => Future successful BadRequest,
      {
        case (vsId, veId, at, te, max) =>
          cheminotm.CheminotcActor.lookForBestTrip(request.ctx.sessionId, vsId, veId, at.toInt, te.toInt, max.toInt) map {
            case Right(trip) => Ok(trip)
            case _ => BadRequest
          }
      }
    )
  }

  def lookForBestDirectTrip = Common.WithCtx { implicit request =>
    Form(tuple(
      "vsId" -> nonEmptyText,
      "veId" -> nonEmptyText,
      "at" -> nonEmptyText,
      "te" -> nonEmptyText
    )).bindFromRequest.fold(
      form => {
        Future successful BadRequest(form.errorsAsJson)
      },
      {
        case (vsId, veId, at, te) =>
          cheminotm.CheminotcActor.lookForBestDirectTrip(request.ctx.sessionId, vsId, veId, at.toInt, te.toInt) map {
            case Right(trip) => Ok(trip)
            case _ => BadRequest
          }
      }
    )
  }

  def abort = Common.WithCtx { implicit request =>
    cheminotm.CheminotcMonitorActor.abort(request.ctx.sessionId) map { _ =>
      Ok
    }
  }

  def trace = Common.WithCtx { implicit request =>
    cheminotm.CheminotcMonitorActor.trace(request.ctx.sessionId) map {
      case Right(enumerator) => Ok.chunked(enumerator &> EventSource()).as( "text/event-stream")
      case _ => BadRequest
    }
  }
}
