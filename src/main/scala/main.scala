package org.cheminot

import rapture.core._
import rapture.cli._
import rapture.http._, httpBackends.jetty._

object Main {

  def main(args: Array[String]): Unit = {
    HttpServer.listen(Args(args).port) {
      router.Api.handle orElse
      router.Site.handle
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
