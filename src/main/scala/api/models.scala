package org.cheminot.web.api

import org.joda.time.DateTime
import rapture.json._, jsonBackends.jawn._
import org.cheminot.misc
import org.cheminot.web.storage

case class ApiEntry(ref: String, buildDate: DateTime, subsets: Seq[Subset])

object ApiEntry {

  def apply(m: storage.Meta): ApiEntry = {
    ApiEntry(m.metaid, m.bundledate, m.subsets.map(Subset.apply))
  }
}

case class Subset(
  id: String,
  name: String,
  updatedDate: Option[DateTime],
  startDate: Option[DateTime],
  endDate: Option[DateTime]
)

object Subset {

  def apply(s: storage.MetaSubset): Subset =
    Subset(s.metasubsetid, s.metasubsetname, s.updateddate, s.startdate, s.enddate)

  def toJson(subset: Subset): Json = {
    val json = JsonBuffer.empty

    json.id = subset.id

    json.name = subset.name

    subset.updatedDate.foreach { date =>
      json.updatedDate = misc.DateTime.format(date)
    }

    subset.startDate.foreach { date =>
      json.startDate = misc.DateTime.format(date)
    }

    subset.endDate.foreach { date =>
      json.endDate = misc.DateTime.format(date)
    }

    json.as[Json]
  }
}

case class StopTime(
  id: String,
  name: String,
  lat: Double,
  lng: Double,
  arrival: DateTime,
  departure: Option[DateTime]
)

object StopTime {

  def fromJson(json: Json): StopTime = {
    StopTime(
      json.id.as[String],
      json.name.as[String],
      json.lat.as[Double],
      json.lng.as[Double],
      misc.DateTime.parseOrFail(json.arrival.as[String]),
      json.departure.as[Option[String]].map(misc.DateTime.parseOrFail)
    )
  }

  def toJson(stopTime: StopTime): Json = {
    val json = JsonBuffer.empty
    json.id = stopTime.id
    json.name = stopTime.name
    json.lat = stopTime.lat
    json.lng = stopTime.lng
    json.arrival = misc.DateTime.format(stopTime.arrival)
    stopTime.departure.foreach { departure =>
      json.departure = misc.DateTime.format(departure)
    }
    json.as[Json]
  }
}

case class Trip(id: String, serviceid: String, stopTimes: List[StopTime]) {

  lazy val departure: Option[DateTime] =
    stopTimes.headOption.flatMap(_.departure)
}

object Trip {

  def apply(trip: storage.Trip, at: DateTime): Trip = {
    val (goesTo, _) = trip.stopTimes.unzip
    val stopTimes = trip.stopTimes.zipWithIndex.map {
      case ((to, stop), index) =>
        val departure = goesTo.lift(index + 1).flatMap(_.departure)
        StopTime(
          stop.stationid,
          stop.name,
          stop.lat,
          stop.lng,
          arrival = to.arrival,
          departure = departure
        )
    }
    Trip(trip.tripid, trip.serviceid, stopTimes)
  }

  def toJson(trip: Trip): Json = {
    val json = JsonBuffer.empty
    json.id = trip.id
    json.serviceid = trip.serviceid
    json.stopTimes = trip.stopTimes.map(StopTime.toJson)
    json.as[Json]
  }

  def toJsonSeq(trips: Seq[Trip]): Json = {
    Json(trips.map(toJson))
  }

  def fromJson(json: Json): Trip = {
    val stopTimes = json.stopTimes.as[List[Json]].map(StopTime.fromJson)
    Trip(json.id.as[String], json.serviceid.as[String], stopTimes)
  }
}
