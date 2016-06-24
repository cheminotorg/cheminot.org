package org.cheminot.web.storage

import scalaz._
import rapture.http.jsonInterop._
import rapture.json._, jsonBackends.jawn._
import rapture.net._
import rapture.io._
import rapture.mime._
import org.cheminot.web.Config

object Cypher {

  def commit(statement: Statement)(implicit config: Config): Json =
    commitn(Seq(statement))

  def commitn(statements: Seq[Statement])(implicit config: Config): Json = {
    withAuthentication { implicit basicAuthentication =>

      val s = statements.map { statement =>
        val st = Tag.unwrap(statement)
        json"""{ "statement": ${st} }"""
      }

      val body = json"""{ "statements": ${s} }"""

      val endpoint = Http.parse(s"http://${config.dbhost}:7474/db/data/transaction/commit")

      val headers = Map(
        "X-stream" -> "true",
        "Content-Type" -> MimeTypes.`application/json`.toString,
        "Accept" ->   MimeTypes.`application/json`.toString
      )

      val response = endpoint.httpPost(body, headers = headers)

      val json = Json.parse(response.slurp[Char])

      val errors = json.errors.as[List[Json]]

      if(!errors.isEmpty) {
        sys.error(s"Unable to perform commitn \n: ${errors}")
      }

      json
    }
  }

  def withAuthentication[A](f: HttpBasicAuthentication => A): A = {
    f(new HttpBasicAuthentication(Option("neo4j" -> "cheminotorg")))
  }
}
