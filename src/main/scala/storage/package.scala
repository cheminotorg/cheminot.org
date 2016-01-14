package org.cheminot.site

import scalaz._
import rapture.json._
import rapture.net._
import rapture.mime._
import rapture.io._

package object storage {

  object Tags {
    sealed trait Statement
    sealed trait TripId
    sealed trait StationId
  }

  type Statement = String @@ Tags.Statement
  def Statement(s: String): Statement = Tag[String, Tags.Statement](s)

  type TripId = String @@ Tags.TripId
  def TripId(s: String): TripId = Tag[String, Tags.TripId](s)

  type StationId = String @@ Tags.StationId
  def StationId(s: String): StationId = Tag[String, Tags.StationId](s)

  implicit val JsonPostType = new PostType[Json] {
    def contentType = Option(MimeTypes.`application/json`)
    def sender(content: Json) =
      ByteArrayInput(content.toString.getBytes("UTF-8"))
  }
}
