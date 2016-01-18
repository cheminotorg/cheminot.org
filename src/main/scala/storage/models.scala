package org.cheminot.storage

case class Meta(version: String)

case class Station(stationid: String, name: String, lat: Double, lng: Double)

case class Stop(stopid: String, stationid: String, parentid: Option[String])

case class GoesTo(arrival: Int, departure: Option[Int])

case class Trip(tripid: String, serviceid: String, stopTimes: List[(GoesTo, Station)])
