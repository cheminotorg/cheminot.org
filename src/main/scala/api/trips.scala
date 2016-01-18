package org.cheminot.api

import rapture.json._, jsonBackends.jackson._

object Trips {

  def renderJson(trips: List[Trip]): Json = {
    json"""{ "results": ${trips} }"""
  }
}
