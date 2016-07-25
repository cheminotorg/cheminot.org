package org.cheminot.web.storage

import org.joda.time.DateTime
import rapture.core._
import rapture.json._
import org.cheminot.misc

case class Meta(metaid: String, bundledate: DateTime, subsets: Seq[MetaSubset])

object MetaSubset {

  def fromJson(json: Json): MetaSubset =
    MetaSubset(
      json.metasubsetid.as[String],
      misc.DateTime.fromSecs(json.timestamp.as[Long])
    )
}

case class MetaSubset(metasubsetid: String, timestamp: DateTime)

case class Station(stationid: String, name: String, lat: Double, lng: Double)

case class Stop(stopid: String, stationid: String, parentid: Option[String])

case class GoesTo(arrival: DateTime, departure: Option[DateTime])

object GoesTo {

  private def withTime(at: DateTime, time: Int): DateTime = {
    at.withTimeAtStartOfDay.plusMinutes(time)
  }

  def toJson(json: Json, date: DateTime): GoesTo = {
    GoesTo(
      withTime(date, json.arrival.as[Int]),
      json.departure.as[Option[Int]].map(withTime(date, _))
    )
  }
}

case class Trip(tripid: String, serviceid: String, stopTimes: List[(GoesTo, Station)]) {

  override def equals(o: Any): Boolean =
    o match {
      case r: Trip if r.tripid == tripid => true
      case r: Trip =>
        (for {
          firstStopTime <- stopTimes.headOption
          otherFirstStopTime <- r.stopTimes.headOption
          if firstStopTime == otherFirstStopTime

          lastStopTime <- stopTimes.lastOption
          otherLastStopTime <- r.stopTimes.lastOption
          if lastStopTime == otherLastStopTime
        } yield true).isDefined
      case _ => false
    }

  override def hashCode =
    (for {
      firstStopTime <- stopTimes.headOption
      lastStopTime <- stopTimes.lastOption
      if firstStopTime != lastStopTime
    } yield {
      List(firstStopTime, lastStopTime).map(_.hashCode).mkString("#").hashCode
    }) getOrElse tripid.hashCode
}
