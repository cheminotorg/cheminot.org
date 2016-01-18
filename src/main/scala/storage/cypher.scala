package org.cheminot.storage

import scalaz._
import rapture.json._, jsonBackends.jawn._
import rapture.uri._
import rapture.net._
import rapture.io._
import rapture.mime._

object Cypher {

  case class Row()

  def commit(statement: Statement): Json =
    commitn(Seq(statement))

  def commitn(statements: Seq[Statement]): Json = {
    withAuthentication { implicit basicAuthentication =>
      val s = statements.map { statement =>
        val st = Tag.unwrap(statement)
        json"""{ "statement": ${st} }"""
      }

      val body = json"""{ "statements": ${s} }"""

      val endpoint: HttpUrl = uri"http://localhost:7474/db/data/transaction/commit"

      val headers = Map(
        "X-stream" -> "true",
        "Content-Type" -> MimeTypes.`application/json`.toString,
        "Accept" ->   MimeTypes.`application/json`.toString
      )

      val response = endpoint.httpPost(body, headers = headers)

      Json.parse(response.slurp[Char])
    }
  }

  def withAuthentication[A](f: HttpBasicAuthentication => A): A = {
    f(new HttpBasicAuthentication(Option("neo4j" -> "xxxxx")))
  }
}
