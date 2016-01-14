package org.cheminot.site.storage

case class Stop(stopid: String, stationid: String, parentid: Option[String])

case class GoesTo(arrival: Int, departure: Option[Int])

case class Trip(tripid: String, serviceid: String, stopTimes: List[(GoesTo, Stop)])
