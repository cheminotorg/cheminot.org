package org.cheminot.site

import rapture.core._
import rapture.cli._
import rapture.http._, httpBackends.jetty._
import rapture.uri._
import rapture.codec._
import encodings.`UTF-8`._
import RequestExtractors._

object Main {

  def main(args: Array[String]): Unit = {
    HttpServer.listen(Args(args).port) { route =>
      route match {
        case Path(^) =>
          pages.Home()

        case Path(^ / "api") =>
          val trips = storage.Storage.fetchNextTrips().map(api.Trip.apply)
          api.Entry.renderJson(trips)
      }
    }
  }
}

case class Args(port: Int)

object Args {

  import modes.returnOption._

  val Port = New.Param[Int]('p', 'port)

  def apply(args: Array[String]): Args = {
    val params = New.ParamMap(args:_*)
    val port = Port.parse(params) getOrElse 8080
    Args(port = port)
  }
}
