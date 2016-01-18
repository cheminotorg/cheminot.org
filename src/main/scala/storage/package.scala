package org.cheminot

import scalaz._
import rapture.json._
import rapture.net._
import rapture.mime._
import rapture.io._

package object storage {

  object Tags {
    sealed trait Statement
  }

  type Statement = String @@ Tags.Statement
  def Statement(s: String): Statement = Tag[String, Tags.Statement](s)

  type Row = List[Json]

  implicit val JsonPostType = new PostType[Json] {
    def contentType = Option(MimeTypes.`application/json`)
    def sender(content: Json) =
      ByteArrayInput(content.toString.getBytes("UTF-8"))
  }
}
