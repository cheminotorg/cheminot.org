package controllers

import scala.concurrent.Future
import play.api._
import play.api.mvc._

object Application extends Controller {

  def index = Common.Public { implicit request =>
    Future successful Ok(views.html.index())
  }
}
