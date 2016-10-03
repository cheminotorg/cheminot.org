package org.cheminot.web.api.models

import org.joda.time.{ DateTime, Minutes }
import org.joda.time.format.{PeriodFormat, PeriodFormatterBuilder}
import rapture.html._, htmlSyntax.{ Option => HOption, _ }
import rapture.json._, jsonBackends.jawn._
import org.cheminot.misc
import org.cheminot.web.storage

case class ApiEntry(ref: String, buildDate: DateTime, subsets: Seq[Subset])

object ApiEntry {

  def apply(m: storage.models.Meta): ApiEntry =
    ApiEntry(m.metaid, m.bundledate, m.subsets.map(Subset.apply))
}

case class Subset(
  id: String,
  timestamp: DateTime
)

object Subset {

  def apply(s: storage.models.MetaSubset): Subset =
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

  object json {

    def reads(json: Json): StopTime = {
      StopTime(
        json.id.as[String],
        json.name.as[String],
        json.lat.as[Double],
        json.lng.as[Double],
        misc.DateTime.parseOrFail(json.arrival.as[String]),
        json.departure.as[Option[String]].map(misc.DateTime.parseOrFail)
      )
    }

    def writes(stopTime: StopTime): Json = {
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
}

case class Calendar(
  monday: Boolean,
  tuesday: Boolean,
  wednesday: Boolean,
  thursday: Boolean,
  friday: Boolean,
  saturday: Boolean,
  sunday: Boolean,
  startdate: DateTime,
  enddate: DateTime
)

object Calendar {

  def apply(calendar: storage.models.Calendar): Calendar =
    Calendar(
      calendar.monday,
      calendar.tuesday,
      calendar.wednesday,
      calendar.thursday,
      calendar.friday,
      calendar.saturday,
      calendar.sunday,
      calendar.startdate,
      calendar.enddate
    )

  object html {

    def writes(calendar: Calendar) = {
      Section(
        Dl(
          Dt("Start date"),
          Dd(misc.DateTime.format(calendar.startdate)),
          Dt("End date"),
          Dd(misc.DateTime.format(calendar.enddate))
        ),
        Table(
          Thead(
            Tr(Td("Lundi"), Td("Mardi"), Td("Mercredi"), Td("Jeudi"), Td("Vendredi"), Td("Samedi"), Td("Dimanche"))
          ),
          Tbody(
            Tr,
            Tr(
              Td(if(calendar.monday) "OK" else "N/A"),
              Td(if(calendar.tuesday) "OK" else "N/A"),
              Td(if(calendar.wednesday) "OK" else "N/A"),
              Td(if(calendar.thursday) "OK" else "N/A"),
              Td(if(calendar.friday) "OK" else "N/A"),
              Td(if(calendar.saturday) "OK" else "N/A"),
              Td(if(calendar.sunday) "OK" else "N/A")
            )
          )
        )
      )
    }
  }

  object json {

    def reads(json: Json): Calendar = {
      Calendar(
        json.monday.as[Boolean],
        json.tuesday.as[Boolean],
        json.wednesday.as[Boolean],
        json.thursday.as[Boolean],
        json.friday.as[Boolean],
        json.saturday.as[Boolean],
        json.sunday.as[Boolean],
        misc.DateTime.parseOrFail(json.startdate.as[String]),
        misc.DateTime.parseOrFail(json.enddate.as[String])
      )
    }

    def writes(calendar: Calendar): Json = {
      val json = JsonBuffer.empty
      json.monday = calendar.monday
      json.tuesday = calendar.tuesday
      json.wednesday = calendar.wednesday
      json.thursday = calendar.thursday
      json.friday = calendar.friday
      json.saturday = calendar.saturday
      json.sunday = calendar.sunday
      json.startdate = misc.DateTime.format(calendar.startdate)
      json.enddate = misc.DateTime.format(calendar.enddate)
      json.as[Json]
    }
  }
}

case class Trip(id: String, serviceid: String, stopTimes: List[StopTime], calendar: Calendar) {

  lazy val departure: Option[DateTime] =
    stopTimes.headOption.flatMap(_.departure)
}

object Trip {

  def apply(trip: storage.models.Trip): Trip = {
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

    Trip(trip.tripid, trip.serviceid, stopTimes, Calendar(trip.calendar))
  }

  object json {

    def writes(trip: Trip): Json = {
      val json = JsonBuffer.empty
      json.id = trip.id
      json.serviceid = trip.serviceid
      json.stopTimes = trip.stopTimes.map(StopTime.json.writes)
      json.calendar = Calendar.json.writes(trip.calendar)
      json.as[Json]
    }

    def writesSeq(trips: Seq[Trip]): Json =
      Json(trips.map(writes))

    def reads(json: Json): Trip = {
      val stopTimes = json.stopTimes.as[List[Json]].map(StopTime.json.reads)
      val calendar = Calendar.json.reads(json.calendar.as[Json])
      Trip(json.id.as[String], json.serviceid.as[String], stopTimes, calendar)
    }
  }

  object html {

    def writesSeq(trips: Seq[Trip]) =
      trips.map(writesHtml)

    def writesHtml(trip: Trip) = {
      Section(
        Hr,
        H2(s"Trajet ${trip.id}"),
        Table(
          Thead(
            Tr(Td("id"), Td("name"), Td("lat"), Td("lng"), Td("arrival"), Td("departure"))
          ),
          Tbody(
            Tr,
            trip.stopTimes.map { stopTime =>
              Tr(
                Td(stopTime.id),
                Td(stopTime.name),
                Td(stopTime.lat.toString),
                Td(stopTime.lng.toString),
                Td(misc.DateTime.format(stopTime.arrival)),
                stopTime.departure.map(misc.DateTime.format).map(Td(_)).getOrElse(Td("N/A"))
              )
            }:_*
          )
        ),
        H3(s"Service ${trip.serviceid}"),
        Calendar.html.writes(trip.calendar)
      )
    }
  }
}

case class DepartureTime(at: Minutes, calendar: Calendar)

object DepartureTime {

  def apply(departureTime: storage.models.DepartureTime): DepartureTime = {
    DepartureTime(departureTime.at, Calendar(departureTime.calendar))
  }

  def formatMinutes(minutes: Minutes): String = {
    val formatter = new PeriodFormatterBuilder()
      .printZeroAlways()
      .appendHours()
      .appendSeparator(":")
      .appendMinutes()
      .toFormatter();
    formatter.print(minutes.toStandardDuration.toPeriod)
  }

  object json {

    def writes(departureTime: DepartureTime): Json = {
      val json = JsonBuffer.empty
      json.at = departureTime.at.getMinutes
      json.calendar = Calendar.json.writes(departureTime.calendar)
      json.as[Json]
    }

    def writesSeq(departureTimes: Seq[DepartureTime]): Json =
      Json(departureTimes.map(writes))
  }
}
