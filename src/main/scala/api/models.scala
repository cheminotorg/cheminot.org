package org.cheminot.site.api

import org.cheminot.site.storage

case class StopTime(id: String, arrival: Int, departure: Option[Int])

case class Trip(id: String, serviceid: String, stopTimes: List[StopTime])

object Trip {

  def apply(trip: storage.Trip): Trip = {
    val (goesTo, _) = trip.stopTimes.unzip
    val stopTimes = trip.stopTimes.zipWithIndex.map {
      case ((to, stop), index) =>
        val departure = goesTo.lift(index + 1).flatMap(_.departure)
        StopTime(stop.stationid, to.arrival, departure)
    }
    Trip(trip.tripid, trip.serviceid, stopTimes)
  }
}
