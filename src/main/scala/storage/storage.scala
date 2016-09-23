package org.cheminot.web.storage

import org.joda.time.{DateTime, Duration}
import rapture.json._, jsonBackends.jawn._
import org.cheminot.web.Params
import org.cheminot.misc
import org.cheminot.web.Config

object Storage {

  private val SEARCH_TRIPS_MAX_LIMIT = 20

  private val SEARCH_TRIPS_DEFAULT_LIMIT = 10

  private def fetch[A](statement: Statement)(f: Row => A)(implicit config: Config): List[A] = {
    val response = Cypher.commit(statement)
    for {
      result <- response.results.as[List[Json]]
      data <- result.data.as[List[Json]]
    } yield {
      f(data.row.as[List[Json]])
    }
  }

  def fetchMeta()(implicit config: Config): Meta = {
    val query = "match p=(s:Meta)-[:HAS]->(m:MetaSubset) return s as Meta, m as MetaSubset;"
    fetch(Statement(query)) { row =>
      val subset = MetaSubset.fromJson(row(1))
      val id = row(0).metaid.as[String]
      val bundleDate = misc.DateTime.fromSecs(row(0).bundledate.as[Long])
      Meta(id, bundleDate, Seq(subset))
    }.groupBy(_.metaid).headOption.flatMap {
      case (_, meta :: rest) => Option(
        meta.copy(subsets = rest.flatMap(_.subsets) ++: meta.subsets)
      )
      case _ => None
    } getOrElse sys.error("Unable to fetch meta")
  }

  private def isParentStation(stationId: String)(implicit config: Config): Boolean = {
    val query = s"match (s:ParentStation {parentstationid: '${stationId}'}) return s;"
    fetch(Statement(query))(identity).headOption.isDefined
  }

  private def searchTrips(params: Params.SearchTrips, filter: DateTime => String, nextAt: (Seq[Trip], DateTime) => DateTime, sortBy: String)(implicit config: Config): List[Trip] = {

    val l = if(params.limit.exists(_ > SEARCH_TRIPS_MAX_LIMIT)) {
      SEARCH_TRIPS_MAX_LIMIT
    } else {
      params.limit getOrElse SEARCH_TRIPS_DEFAULT_LIMIT
    }

    def f(at: DateTime, limit: Int): List[Trip] = {
      val day = misc.DateTime.forPattern("EEEE").print(at).toLowerCase
      val start = at.withTimeAtStartOfDay.getMillis / 1000
      val end = at.withTimeAtStartOfDay.plusDays(1).getMillis / 1000
      val vsfield = if (isParentStation(params.vs)) "parentid" else "stationid"
      val vefield = if (isParentStation(params.ve)) "parentid" else "stationid"
      val query = s"""
      MATCH path=(calendar:Calendar)<-[:SCHEDULED_AT*1..]-(trip:Trip)-[:GOES_TO*1..]->(a:Stop { ${vsfield}: '${params.vs}' })-[stoptimes:GOES_TO*1..]->(b:Stop { ${vefield}: '${params.ve}' })
      WITH calendar, trip, tail(tail(nodes(path))) AS stops, tail(relationships(path)) AS allstoptimes, head(stoptimes) AS vs
      WHERE ${filter(at)}
        AND ((calendar.${day} = true AND calendar.startdate <= ${start} AND calendar.enddate > ${end} AND NOT (trip)-[:OFF]->(:CalendarDate { date: ${start} }))
        OR (trip)-[:ON]->(:CalendarDate { date: ${start} }))
      RETURN distinct(trip), stops, allstoptimes, vs, calendar
      ORDER BY $sortBy
      LIMIT ${l * 2};
      """

      val trips = fetch(Statement(query)) { row =>
        val tripId = row(0).tripid.as[String]
        val serviceId = row(0).serviceid.as[String]
        val stops = row(1).as[List[Stop]]
        val goesTo = row(2).as[List[Json]].map(GoesTo.fromJson(_, at))
        val calendar = Calendar.fromJson(row(4).as[Json])
        (tripId, serviceId, goesTo, stops, calendar)
      }

      val stationIds = trips.flatMap {
        case (_, _, _, stops, _) =>
          stops.map(_.stationid)
      }.distinct

      val stations = fetchStationsById(stationIds).map { station =>
        station.stationid -> station
      }.toMap

      trips.map {
        case (tripId, serviceId, goesTo, stops, calendar) =>
          val tripStations = stops.flatMap(s => stations.get(s.stationid).toList)
          val stopTimes = goesTo.zip(tripStations).dropWhile {
            case (_, s) => s.stationid != params.vs
          }
          Trip(tripId, serviceId, stopTimes, calendar)
      }
    }

    scalaz.Scalaz.unfold((params.at, l, 3)) {
      case (at, todo, counter) =>
        if(todo <= 0 || counter <= 0) {
          None
        } else {
          val trips = f(at, todo)
          val distinctTrips = trips.distinct
          val remaining = todo - distinctTrips.size
          val retries = if(trips.isEmpty) counter - 1 else counter
          Option((distinctTrips, (nextAt(trips, at), remaining, retries)))
        }
    }.toList.flatten.take(l)
  }

  private def formatTime(time: DateTime): String =
    org.cheminot.misc.DateTime.minutesOfDay(time).toString

  def searchPreviousTrips(params: Params.SearchTrips)(implicit config: Config): List[Trip] = {
    val filter = (t: DateTime) => {
      s"vs.departure < ${formatTime(t)}"
    }
    val nextAt = (trips: Seq[Trip], t: DateTime) => {
      val distinctTrips = trips.distinct
      val e = trips.size - distinctTrips.size
      if(e > 0) {
        (for {
          lastTrip <- trips.lastOption
          departure <- lastTrip.stopTimes.lift(1).flatMap(_._1.departure)
        } yield {
          departure.minusMinutes(1)
        }) getOrElse sys.error("Unable to compute nextAt")
      } else {
        params.at.minusDays(1).withTime(23, 59, 59, 999)
      }
    }
    searchTrips(params, filter = filter, nextAt = nextAt, sortBy = "-vs.departure").reverse
  }

  def searchNextTrips(params: Params.SearchTrips)(implicit config: Config): List[Trip] = {
    val filter = (t: DateTime) => {
      s"vs.departure > ${formatTime(t)}"
    }
    val nextAt = (trips: Seq[Trip], t: DateTime) => {
      val distinctTrips = trips.distinct
      val e = trips.size - distinctTrips.size
      if(e > 0) {
        (for {
          lastTrip <- trips.lastOption
          departure <- lastTrip.stopTimes.lift(1).flatMap(_._1.departure)
        } yield {
          departure.plusMinutes(1)
        }) getOrElse sys.error("Unable to compute nextAt")
      } else {
        params.at.plusDays(1).withTimeAtStartOfDay
      }
    }
    searchTrips(params, filter = filter, nextAt = nextAt, sortBy = "vs.departure")
  }

  def searchDepartureTimes(params: Params.SearchDepartureTimes)(implicit config: Config): List[DepartureTime] = {
    val filters = Map(
      "monday" -> params.monday,
      "tuesday" -> params.tuesday,
      "thursday" -> params.thursday,
      "wednesday" -> params.wednesday,
      "thursday" -> params.thursday,
      "friday" -> params.friday,
      "saturday" -> params.saturday,
      "sunday" -> params.sunday
    ).collect {
      case (day, Some(value)) if value =>
        s"calendar.${day}=${value}"
    }

    val filtersStr = if(filters.isEmpty) "" else {
      s"""WHERE ${filters.mkString(" OR ")}"""
    }

    val query = s"""
      MATCH path=(calendar:Calendar)<-[:SCHEDULED_AT*1..]-(trip:Trip)-[:GOES_TO*1..]->(:Stop { stationid: '${params.vs}' })-[stoptimes:GOES_TO*1..]->(:Stop { stationid: '${params.ve}' })
      ${filtersStr}
      WITH head(stoptimes) AS stoptimeA, calendar
      RETURN distinct(stoptimeA.departure), calendar
      ORDER BY stoptimeA.departure
    """

    fetch(Statement(query)) { row =>
      val minutes = Duration.standardMinutes(row(0).as[Long]).toStandardMinutes
      val calendar = Calendar.fromJson(row(1).as[Json])
      DepartureTime(minutes, calendar)
    }.groupBy(_.at).toList.map {
      case (minutes, departureTimes) =>
        DepartureTime(minutes, departureTimes.map(_.calendar).reduce(_ merge _))
    }.sortBy(_.at.getMinutes)
  }

  def fetchStationsById(stationIds: Seq[String])(implicit config: Config): List[Station] = {
    if(!stationIds.isEmpty) {
      val ids = stationIds.map(s => s""""$s"""").mkString(",")
      val query =
        s"""MATCH (station:Station)
          WHERE station.stationid IN [$ids]
          return DISTINCT station;
        """;
      fetch(Statement(query))(_(0).as[Station])
    } else Nil
  }
}
