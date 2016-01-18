package org.cheminot.api

import org.cheminot.storage

case class StopTime(id: String, name: String, lat: Double, lng: Double, arrival: Int, departure: Option[Int])

case class Trip(id: String, serviceid: String, stopTimes: List[StopTime])

case class Meta(version: String)

object Meta {

  def apply(m: storage.Meta): Meta = {
    Meta(m.version)
  }
}

object Trip {

  def apply(trip: storage.Trip): Trip = {
    val (goesTo, _) = trip.stopTimes.unzip
    val stopTimes = trip.stopTimes.zipWithIndex.map {
      case ((to, stop), index) =>
        val departure = goesTo.lift(index + 1).flatMap(_.departure)
        StopTime(stop.stationid, stop.name, stop.lat, stop.lng, to.arrival, departure)
    }
    Trip(trip.tripid, trip.serviceid, stopTimes)
  }
}
