package org.cheminot.api

import rapture.json._, jsonBackends.jackson._

object Entry {

  def renderJson(meta: Meta): Json = {
    json"""{ "ref": ${meta.version} }"""
  }
}
