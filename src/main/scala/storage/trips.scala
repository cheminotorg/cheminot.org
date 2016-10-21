package org.cheminot.web.storage

import org.joda.time.{DateTime, Duration}
import rapture.json._, jsonBackends.jawn._
import org.cheminot.web.Params
import org.cheminot.misc
import org.cheminot.web.Config

object Trips {

  private val SEARCH_TRIPS_MAX_LIMIT = 20
  private val SEARCH_TRIPS_DEFAULT_LIMIT = 10

  private def executeTripsQuery(query: String)(parseGoesTo: (List[Json], models.Calendar) => List[models.GoesTo])(implicit config: Config): List[models.Trip] = {
    val trips = Storage.fetch(Statement(query)) { row =>
      val tripId = row(0).tripid.as[String]
      val serviceId = row(0).serviceid.as[String]
      val stops = row(1).as[List[models.Stop]]
      val calendar = models.Calendar.json.reads(row(4).as[Json])
      val goesTo = parseGoesTo(row(2).as[List[Json]], calendar)
      (tripId, serviceId, goesTo, stops, calendar)
    }

    val stationIds = trips.flatMap {
      case (_, _, _, stops, _) =>
        stops.map(_.stationid)
    }.distinct

    val stations = Stations.fetchById(stationIds).map { station =>
      station.stationid -> station
    }.toMap

    val serviceIds = trips.map { case (_, serviceId, _, _, _) => serviceId }.distinct

    val calendardatesByServiceId = org.cheminot.misc.Debug.measure(CalendarDate.fetchByServiceIds(serviceIds))(x => println(s"byServiceId $x"))

    trips.map {
      case (tripId, serviceId, goesTo, stops, calendar) =>
        val tripStations = stops.flatMap(s => stations.get(s.stationid).toList)
        val stopTimes = goesTo.zip(tripStations)
        val calendarDates = calendardatesByServiceId.get(serviceId) getOrElse Nil
        models.Trip(tripId, serviceId, stopTimes, calendar, calendarDates)
    }
  }

  private def search(params: Params.SearchTrips, filter: DateTime => String, nextAt: (Seq[models.Trip], DateTime) => DateTime, sortBy: String)(implicit config: Config): List[models.Trip] = {
    val limit = if(params.limit.exists(_ > SEARCH_TRIPS_MAX_LIMIT)) {
      SEARCH_TRIPS_MAX_LIMIT
    } else {
      params.limit getOrElse SEARCH_TRIPS_DEFAULT_LIMIT
    }

    def more(at: DateTime, max: Int): List[models.Trip] = {
      val day = models.Calendar.formatDay(at)
      val start = at.withTimeAtStartOfDay.getMillis / 1000
      val end = at.withTimeAtStartOfDay.plusDays(1).getMillis / 1000
      val vsfield = if (Stations.isParent(params.vs)) "parentid" else "stationid"
      val vefield = if (Stations.isParent(params.ve)) "parentid" else "stationid"
      val query = s"""
        MATCH path=(calendar:Calendar)<-[:SCHEDULED_AT*1..]-(trip:Trip)-[:GOES_TO*1..]->(a:Stop { ${vsfield}: '${params.vs}' })-[stoptimes:GOES_TO*1..]->(b:Stop { ${vefield}: '${params.ve}' })
        WITH calendar, trip, tail(tail(nodes(path))) AS stops, tail(relationships(path)) AS allstoptimes, head(stoptimes) AS vs
        WHERE ${filter(at)}
        AND ((calendar.${day} = true AND calendar.startdate <= ${start} AND calendar.enddate > ${end} AND NOT (trip)-[:OFF]->(:CalendarDate { date: ${start} }))
        OR (trip)-[:ON]->(:CalendarDate { date: ${start} }))
        RETURN distinct(trip), stops, allstoptimes, vs, calendar
        ORDER BY $sortBy
        LIMIT ${math.min(max * 2, SEARCH_TRIPS_MAX_LIMIT * 2)};
      """
println(query)
      executeTripsQuery(query) {
        case (goesTo, _) =>
          goesTo.map(models.GoesTo.json.reads(_, at))
      }
    }

    scalaz.Scalaz.unfold((params.at, limit, 3)) {
      case (at, todo, counter) =>
        if(todo <= 0 || counter <= 0) {
          None
        } else {
          val trips = org.cheminot.misc.Debug.measure(more(at, todo))(x => println(s"result $x"))
          val remaining = todo - trips.size
          val retries = if(trips.isEmpty) counter - 1 else counter
          Option((trips, (nextAt(trips, at), remaining, retries)))
        }
    }.toList.flatten.take(limit)
  }

  def searchPrevious(params: Params.SearchTrips)(implicit config: Config): List[models.Trip] = {
    val filter = (t: DateTime) => {
      s"vs.departure < ${t.getMinuteOfDay}"
    }
    val nextAt = (trips: Seq[models.Trip], t: DateTime) => {
      val distinctTrips = trips.distinct
      val e = trips.size - distinctTrips.size
      if(e > 0) {
        val departure = trips.lastOption.flatMap { lastTrip =>
          lastTrip.stopTimes.dropWhile {
            case (_, station) => station.stationid == params.vs
          }.drop(1).headOption.map {
            case (goesTo, _) =>
              goesTo.departure
          }.flatten
        } getOrElse sys.error("Unable to compute nextAt")
        departure.minusMinutes(1)
      } else {
        params.at.minusDays(1).withTime(23, 59, 59, 999)
      }
    }
    search(params, filter = filter, nextAt = nextAt, sortBy = "-vs.departure").reverse
  }

  def searchNext(params: Params.SearchTrips)(implicit config: Config): List[models.Trip] = {
    val filter = (t: DateTime) => {
      s"vs.departure > ${t.getMinuteOfDay}"
    }
    val nextAt = (trips: Seq[models.Trip], t: DateTime) => {
      val distinctTrips = trips.distinct
      val e = trips.size - distinctTrips.size
      if(e > 0) {
        val departure = trips.lastOption.flatMap { lastTrip =>
          lastTrip.stopTimes.dropWhile {
            case (_, station) => station.stationid == params.vs
          }.drop(1).headOption.map {
            case (goesTo, _) =>
              goesTo.departure
          }.flatten
        } getOrElse sys.error("Unable to compute nextAt")
        departure.plusMinutes(1)
      } else {
        params.at.plusDays(1).withTimeAtStartOfDay
      }
    }
    search(params, filter = filter, nextAt = nextAt, sortBy = "vs.departure")
  }

  def fetch(params: Params.FetchTrips)(implicit config: Config): List[models.Trip] = {
    if(!params.departureTimes.isEmpty) {
      val whereClause = params.departureTimes.map { departureTime =>
        val day = models.Calendar.formatDay(departureTime)
        val calendarEq = s"calendar.${day} = true"
        val departureEq = s"vs.departure = ${departureTime.getMinuteOfDay}"
        val periodEq = {
          val start = departureTime.withTimeAtStartOfDay.getMillis / 1000
          val end = departureTime.withTimeAtStartOfDay.plusDays(1).getMillis / 1000
          s"(calendar.startdate <= ${start} AND calendar.enddate > ${end} AND NOT (trip)-[:OFF]->(:CalendarDate { date: ${start} }))"
        }
        s"(${departureEq} AND ${calendarEq} AND ${periodEq})"
      }.mkString(" OR ")

      val vsfield = if (Stations.isParent(params.vs)) "parentid" else "stationid"
      val vefield = if (Stations.isParent(params.ve)) "parentid" else "stationid"

      val query = s"""
        MATCH path=(calendar:Calendar)<-[:SCHEDULED_AT*1..]-(trip:Trip)-[:GOES_TO*1..]->(a:Stop { ${vsfield}: '${params.vs}' })-[stoptimes:GOES_TO*1..]->(b:Stop { ${vefield}: '${params.ve}' })
        WITH calendar, trip, tail(tail(nodes(path))) AS stops, tail(relationships(path)) AS allstoptimes, head(stoptimes) AS vs
        WHERE ${whereClause}
        RETURN distinct(trip), stops, allstoptimes, vs, calendar
        LIMIT ${params.departureTimes.size};
      """

      val trips = executeTripsQuery(query) {
        case (goesToSeq, calendar) =>
          val departureTimes = params.departureTimes.map(_.getMinuteOfDay)
          goesToSeq.lift(1).toList.flatMap { goesTo =>
            models.GoesTo.json.readsAsTuple(goesTo) match {
              case (_, Some(minuteOfDay)) =>
                params.departureTimes.find { departureTime =>
                  departureTime.getMinuteOfDay == minuteOfDay && calendar.isRunningOn(departureTime)
                } match {
                  case Some(departureTime) =>
                    goesToSeq.map(models.GoesTo.json.reads(_, departureTime))
                  case None =>
                    sys.error(s"""Unable to match departure ${goesTo} with ${departureTimes.mkString(" ")}""")
                }
              case _ =>
                sys.error(s"""Unable to match departure ${goesTo} with ${departureTimes.mkString(" ")}""")
            }
          }
      }

      params.departureTimes.flatMap { departureTime =>
        trips.find { trip =>
          trip.stopTimes.lift(1).flatMap(_._1.departure).exists(_.isEqual(departureTime))
        }
      }
    } else Nil
  }
}
