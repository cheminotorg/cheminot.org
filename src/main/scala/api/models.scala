package org.cheminot.api

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.DateTime
import rapture.json._, jsonBackends.jawn._
import org.cheminot.storage
import org.cheminot.misc

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

  private def withDate(at: DateTime, time: Int): DateTime = {
    val (hours, minutes) = {
      val str = time.toString
      str.splitAt(if (str.length > 3) 2 else 1)
    }
    val d = at.withHourOfDay(hours.toInt).withMinuteOfHour(minutes.toInt)
    if(d.isAfter(at)) d else d.plusDays(1)
  }

  def apply(trip: storage.Trip, at: DateTime): Trip = {
    val (goesTo, _) = trip.stopTimes.unzip
    val stopTimes = trip.stopTimes.zipWithIndex.map {
      case ((to, stop), index) =>
        val arrival = goesTo.lift(index - 1).flatten.map(_.arrival) getOrElse {
          to.map(_.departure) getOrElse sys.error(s"Unable to get arrival for $stop")
        }
        StopTime(
          stop.stationid,
          stop.name,
          stop.lat,
          stop.lng,
          arrival = withDate(at, arrival),
          departure = to.map(_.departure).map(withDate(at, _))
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
}

case class Query(ref: String, vs: String, ve: String, limit: Option[Int], previous: Boolean)
