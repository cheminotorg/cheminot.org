package misc

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import play.api.libs.ws.WS
import play.api.Application
import play.api.libs.json._
import models.Config

object Dweet {

  def send(data: JsValue)(implicit app: Application): Future[Unit] = {

    WS.url("https://dweet.io:443/dweet/for/chemniotorg").post(data).map(r => println(r.body)).map(_ => ())
  }
}
