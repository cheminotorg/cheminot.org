package cheminotorg
package misc

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import play.api.libs.ws.WS
import play.api.Application
import play.api.libs.json._

object Dweet {

  def send(thing: String, data: JsValue)(implicit app: Application): Future[Unit] = {

    WS.url("https://dweet.io:443/dweet/for/" + thing).post(data).map(_ => ())
  }
}
