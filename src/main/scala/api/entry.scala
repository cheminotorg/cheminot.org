package org.cheminot.site.api

import rapture.json._, jsonBackends.jackson._

object Entry {

  case class Toto(id: String)

  def renderJson(trips: List[Trip]): Json = {
    json"""{
             "results": ${trips}
           }"""
  }
}
