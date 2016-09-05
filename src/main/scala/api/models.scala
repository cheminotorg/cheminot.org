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
  timestamp: DateTime
)

object Subset {

  def apply(s: storage.MetaSubset): Subset =
    Subset(s.metasubsetid, s.timestamp)

  def toJson(subset: Subset): Json = {
    val json = JsonBuffer.empty
    json.id = subset.id
    json.timestamp = misc.DateTime.format(subset.timestamp)
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

case class Calendar(
  monday: Boolean,
  tuesday: Boolean,
  wednesday: Boolean,
  thursday: Boolean,
  friday: Boolean,
  saturday: Boolean,
  sunday: Boolean
)

object Calendar {

  def toJson(calendar: Calendar): Json = {
    val json = JsonBuffer.empty
    json.monday = calendar.monday
    json.tuesday = calendar.tuesday
    json.wednesday = calendar.wednesday
    json.thursday = calendar.thursday
    json.friday = calendar.friday
    json.saturday = calendar.saturday
    json.sunday = calendar.sunday
    json.as[Json]
  }
}

case class Trip(id: String, serviceid: String, stopTimes: List[StopTime], calendar: Option[Calendar]) {

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

    val maybeCalendar = trip.calendar.map { calendar =>
      Calendar(
        calendar.monday,
        calendar.tuesday,
        calendar.wednesday,
        calendar.thursday,
        calendar.friday,
        calendar.saturday,
        calendar.sunday
      )
    }

    Trip(trip.tripid, trip.serviceid, stopTimes, maybeCalendar)
  }

  def toJson(trip: Trip): Json = {
    val json = JsonBuffer.empty
    json.id = trip.id
    json.serviceid = trip.serviceid
    json.stopTimes = trip.stopTimes.map(StopTime.toJson)
    trip.calendar.foreach { calendar =>
      json.calendar = Calendar.toJson(calendar)
    }
    json.as[Json]
  }

  def toJsonSeq(trips: Seq[Trip]): Json = {
    Json(trips.map(toJson))
  }
}
