package org.cheminot.web

import rapture.core._
import rapture.cli._
import rapture.http._, httpBackends.jetty._

object Main {

  def main(args: Array[String]): Unit = {

    implicit val config = Config(args)

    println(s"Starting http server...")

    HttpServer.listen(config.port) {
      router.Common.onError {
        router.Api.handle orElse
        router.Site.handle orElse
        router.Common.notFound
      }
    }

    println(s"Listening on port ${config.port}")
  }
}

case class Config(port: Int, domain: String)

object Config {

  import modes.returnOption._

  val Port = New.Param[Int]('p', 'port)

  val Domain = New.Param[String]('d', 'domain)

  def apply(args: Array[String]): Config = {
    val params = New.ParamMap(args:_*)
    val port = Port.parse(params) getOrElse 8080
    val domain = Domain.parse(params) getOrElse "localhost:8080"
    Config(port = port, domain = domain)
  }
}
