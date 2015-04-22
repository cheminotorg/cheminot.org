package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.mvc._
import play.api.libs.EventSource
import play.api.Play.current
import play.api.data._
import play.api.data.Forms._
import models.Config
//import play.api.libs.json._

object Cheminotm extends Controller {

  def init = Action.async {
    cheminotm.CheminotcActor.init(Config.cheminotDbPath, Config.graphPath, Config.calendardatesPath) map { meta =>
      Ok(meta)
    }
  }

  def lookForBestTrip = Action.async { implicit request =>
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
          cheminotm.CheminotcActor.lookForBestTrip(Config.cheminotDbPath, vsId, veId, at.toInt, te.toInt, max.toInt) map { trip =>
            Ok(trip)
          }
      }
    )
  }

  def lookForBestDirectTrip = Action.async { implicit request =>
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
          cheminotm.CheminotcActor.lookForBestDirectTrip(Config.cheminotDbPath, vsId, veId, at.toInt, te.toInt) map { trip =>
            Ok(trip)
          }
      }
    )
  }

  def abort = Action.async {
    cheminotm.CheminotcActor.abort(Config.cheminotDbPath) map { _ =>
      Ok
    }
  }

  def trace = Action {
    val enumerator = cheminotm.CheminotcActor.trace(Config.cheminotDbPath)
    Ok.chunked(enumerator &> EventSource()).as( "text/event-stream")
  }
}
