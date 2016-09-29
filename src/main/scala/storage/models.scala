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

case class GoesTo(arrival: DateTime, departure: Option[DateTime])

case class DepartureTime(at: Minutes, calendar: Calendar)

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
    Calendar.isRunningOn(datetime, toMap) 

  def merge(calendarB: Calendar) =
    Calendar.merge(this, calendarB)
}

object Calendar {

  def merge(calendarA: Calendar, calendarB: Calendar): Calendar = {
    val startdate = if(calendarB.startdate.isAfter(calendarA.startdate)) {
      calendarA.startdate
    } else calendarB.startdate

    val enddate = if(calendarB.enddate.isBefore(calendarA.enddate)) {
      calendarA.enddate
    } else calendarB.enddate

    Calendar(
      calendarA.monday || calendarB.monday,
      calendarA.tuesday || calendarB.tuesday,
      calendarA.wednesday || calendarB.wednesday,
      calendarA.thursday || calendarB.thursday,
      calendarA.friday || calendarB.friday,
      calendarA.saturday || calendarB.saturday,
      calendarA.sunday || calendarB.sunday,
      startdate,
      enddate
    )
  }

  def isRunningOn(datetime: DateTime, calendar: Map[String, Boolean]): Boolean = {
    val day = misc.DateTime.forPattern("EEEE").print(datetime).toLowerCase
    calendar.get(day).getOrElse {
      sys.error(s"Unexpected value ${day}")
    }
  }

  def formatDay(datetime: DateTime) =
    misc.DateTime.forPattern("EEEE").print(datetime).toLowerCase

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
        new DateTime(json.startdate.as[Long]),
        new DateTime(json.enddate.as[Long])
      )
    }
  }
}

case class Trip(
  tripid: String,
  serviceid: String,
  stopTimes: List[(GoesTo, Station)],
  calendar: Calendar
) {
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
