package org.cheminot.web

import scalaz._
import rapture.json._

package object storage {

  object Tags {
    sealed trait Statement
  }

  type Statement = String @@ Tags.Statement
  def Statement(s: String): Statement = Tag[String, Tags.Statement](s)

  type Row = List[Json]
}
