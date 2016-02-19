package org.cheminot.storage

import org.joda.time.DateTime
import rapture.json._, jsonBackends.jawn._
import org.cheminot.Params
import org.cheminot.misc

object Storage {

  private val FETCH_TRIPS_MAX_LIMIT = 20

  private val FETCH_TRIPS_DEFAULT_LIMIT = 10

  private def fetch[A](statement: Statement)(f: Row => A): List[A] = {
    val response = Cypher.commit(statement)
    for {
      result <- response.results.as[List[Json]]
      data <- result.data.as[List[Json]]
    } yield {
      f(data.row.as[List[Json]])
    }
  }

  def fetchMeta(): Meta = {
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

  private def fetchTrips(params: Params.FetchTrips, filter: DateTime => String, nextAt: (Seq[Trip], DateTime) => DateTime, sortBy: String): List[Trip] = {

    val l = if(params.limit.exists(_ > FETCH_TRIPS_MAX_LIMIT)) {
      FETCH_TRIPS_MAX_LIMIT
    } else {
      params.limit getOrElse FETCH_TRIPS_DEFAULT_LIMIT
    }

    def f(at: DateTime, limit: Int): List[Trip] = {

      val day = misc.DateTime.forPattern("EEEE").print(at).toLowerCase
      val start = at.withTimeAtStartOfDay.getMillis / 1000
      val end = at.withTimeAtStartOfDay.plusDays(1).getMillis / 1000

      val query = s"""
      MATCH path=(trip:Trip)-[:GOES_TO*1..]->(a:Stop { stationid: '${params.vs}' })-[stoptimes:GOES_TO*1..]->(b:Stop { stationid: '${params.ve}' })
      WITH trip, tail(nodes(path)) AS stops, relationships(path) AS allstoptimes, stoptimes
      OPTIONAL MATCH (trip)-[:SCHEDULED_AT*0..]->(c:Calendar { serviceid: trip.serviceid })
      WITH trip, stops, allstoptimes, head(stoptimes) AS vs
      WHERE ${filter(params.at)}
        AND ((c IS NOT NULL AND (c.${day} = true AND c.startdate <= ${start} AND c.enddate > ${end} AND NOT (trip)-[:OFF]->(:CalendarDate { date: ${start} })))
        OR (trip)-[:ON]->(:CalendarDate { date: ${start} }))
      RETURN distinct(trip), stops, allstoptimes, vs
      ORDER BY $sortBy
      LIMIT $l;
      """

      val trips = fetch(Statement(query)) { row =>
        val tripId = row(0).tripid.as[String]
        val serviceId = row(0).serviceid.as[String]
        val stops = row(1).as[List[Stop]]
        val goesTo = row(2).as[List[Json]].map(GoesTo.toJson(_, at))
        (tripId, serviceId, goesTo, stops)
      }

      val stationIds = trips.flatMap {
        case (_, _, _, stops) =>
          stops.map(_.stationid)
      }.distinct

      val stations = fetchStationsById(stationIds).map { station =>
        station.stationid -> station
      }.toMap

      trips.map {
        case (tripId, serviceId, goesTo, stops) =>
          val tripStations = stops.flatMap(s => stations.get(s.stationid).toList)
          val stopTimes = goesTo.zip(tripStations).dropWhile {
            case (_, s) => s.stationid != params.vs
          }
          Trip(tripId, serviceId, stopTimes)
      }
    }

    scalaz.Scalaz.unfold((params.at, l)) {
      case (at, l) =>
        if(l > 0) {
          val trips = f(at, l)
          val distinctTrips = trips.distinct
          val remaining = l - distinctTrips.size
          Option((distinctTrips, (nextAt(trips, at), remaining)))
        } else None
    }.toList.flatten
  }

  def fetchPreviousTrips(params: Params.FetchTrips): List[Trip] = {
    val departure = misc.DateTime.forPattern("HHmm").print(params.at).toInt
    val filter = (t: DateTime) => {
      val departure = misc.DateTime.forPattern("HHmm").print(params.at).toInt
      s"vs.departure <= $departure"
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
    fetchTrips(params, filter = filter, nextAt = nextAt, sortBy = "-vs.departure").reverse
  }

  def fetchNextTrips(params: Params.FetchTrips): List[Trip] = {
    val filter = (t: DateTime) => {
      val departure = misc.DateTime.forPattern("HHmm").print(t).toInt
      s"vs.departure >= $departure"
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
    fetchTrips(params, filter = filter, nextAt = nextAt, sortBy = "vs.departure")
  }

  def fetchStationsById(stationIds: Seq[String]): List[Station] = {
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
