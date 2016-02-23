package org.cheminot

package object web {

  import scalaz._

  object Tags {
    sealed trait TripId
    sealed trait StationId
  }

  type TripId = String @@ Tags.TripId
  def TripId(s: String): TripId = Tag[String, Tags.TripId](s)

  type StationId = String @@ Tags.StationId
  def StationId(s: String): StationId = Tag[String, Tags.StationId](s)
}
