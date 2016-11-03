package org.cheminot.web.storage.models

import org.joda.time.{DateTime, Minutes}
import rapture.core._
import rapture.json._
import org.cheminot.misc

case class Meta(metaid: String, bundledate: DateTime, subsets: Seq[MetaSubset])

object MetaSubset {

  object json {

    def reads(json: Json): MetaSubset =
      MetaSubset(
        json.metasubsetid.as[String],
        misc.DateTime.fromSecs(json.timestamp.as[Long])
      )
  }
}

case class MetaSubset(metasubsetid: String, timestamp: DateTime)

case class Station(stationid: String, name: String, lat: Double, lng: Double)

case class Stop(stopid: String, stationid: String, parentid: Option[String])

case class GoesTo(
  arrival: DateTime,
  departure: Option[DateTime]
)

object GoesTo {

  private def withTime(at: DateTime, time: Int): DateTime = {
    at.withTimeAtStartOfDay.plusMinutes(time)
  }

  object json {

    def readsAsTuple(json: Json): (Int, Option[Int]) =
      (json.arrival.as[Int], json.departure.as[Option[Int]])

    def reads(json: Json, date: DateTime): GoesTo = {
      readsAsTuple(json) match {
        case (arrival, departure) =>
          GoesTo(withTime(date, arrival), departure.map(withTime(date, _)))
      }
    }
  }
}

case class CalendarDate(
  calendardateid: String,
  `type`: Int,
  date: DateTime,
  serviceid: String
)

object CalendarDate {

  object json {

    def reads(json: Json): CalendarDate =
      CalendarDate(
        json.calendardateid.as[String],
        json.`type`.as[Int],
        new DateTime(json.date.as[Long] * 1000),
        json.`serviceid`.as[String]
      )
  }
}

case class Calendar(
  serviceid: String,
  monday: Boolean,
  tuesday: Boolean,
  wednesday: Boolean,
  thursday: Boolean,
  friday: Boolean,
  saturday: Boolean,
  sunday: Boolean,
  startdate: DateTime,
  enddate: DateTime
) {

  lazy val toMap: Map[String, Boolean] =
    Map(
      "monday" -> monday,
      "tuesday" -> tuesday,
      "wednesday" -> wednesday,
      "thursday" -> thursday,
      "friday" -> friday,
      "saturday" -> saturday,
      "sunday" -> sunday
    )

  def isRunningOn(datetime: DateTime): Boolean =
    Calendar.isRunningOn(datetime, toMap, startdate, enddate)
}

object Calendar {

  def isRunningOn(datetime: DateTime, calendar: Map[String, Boolean], startdate: DateTime, enddate: DateTime): Boolean = {
    if((startdate.isBefore(datetime) || startdate.isEqual(datetime)) && (enddate.isAfter(datetime) || enddate.isEqual(datetime))) {
      val day = misc.DateTime.forPattern("EEEE").print(datetime).toLowerCase
      calendar.get(day).getOrElse {
        sys.error(s"Unexpected value ${day}")
      }
    } else false
  }

  def formatDay(datetime: DateTime) =
    misc.DateTime.forPattern("EEEE").print(datetime).toLowerCase

  object json {

    def reads(json: Json): Calendar = {
      Calendar(
        json.serviceid.as[String],
        json.monday.as[Boolean],
        json.tuesday.as[Boolean],
        json.wednesday.as[Boolean],
        json.thursday.as[Boolean],
        json.friday.as[Boolean],
        json.saturday.as[Boolean],
        json.sunday.as[Boolean],
        new DateTime(json.startdate.as[Long] * 1000),
        new DateTime(json.enddate.as[Long] * 1000)
      )
    }
  }
}

case class Trip(
  tripid: String,
  serviceid: String,
  stopTimes: List[(GoesTo, Station)],
  calendar: Calendar,
  calendarDates: List[CalendarDate]
)
