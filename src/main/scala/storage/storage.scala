package org.cheminot.storage

import org.joda.time.DateTime
import rapture.json._, jsonBackends.jawn._
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

  private def fetchTrips(ref: String, vs: String, ve: String, at: DateTime, limit: Option[Int], filter: DateTime => String, sortBy: String): List[Trip] = {

    val l = if(limit.exists(_ > FETCH_TRIPS_MAX_LIMIT)) {
      FETCH_TRIPS_MAX_LIMIT
    } else {
      limit getOrElse FETCH_TRIPS_DEFAULT_LIMIT
    }

    def f(at: DateTime, limit: Int): List[Trip] = {

      val day = misc.DateTime.forPattern("EEEE").print(at).toLowerCase
      val start = at.withTimeAtStartOfDay.getMillis / 1000
      val end = at.withTimeAtStartOfDay.plusDays(1).getMillis / 1000

      val query = s"""
      MATCH path=(trip:Trip)-[:GOES_TO*1..]->(a:Stop { stationid: '${vs}' })-[:GOES_TO*1..]->(b:Stop { stationid: '${ve}' })
      WITH trip, tail(nodes(path)) AS stops, tail(relationships(path)) AS stoptimes
      MATCH (trip)-[:SCHEDULED_AT]->(c:Calendar { serviceid: trip.serviceid })-->(cd:CalendarDate)
      WHERE ${filter(at)} AND ((c.${day} = true AND c.startdate <= ${start} AND c.enddate > ${end}) OR (c)-[:ON]->(:CalendarDate { date: ${start} }))
      RETURN distinct(trip), stops, stoptimes
      ORDER BY $sortBy
      LIMIT $limit;
    """
      val trips = fetch(Statement(query)) { row =>
        val tripId = row(0).tripid.as[String]
        val serviceId = row(0).serviceid.as[String]
        val stops = row(1).as[List[Stop]]
        val goesTo = row(2).as[List[GoesTo]]
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
          val stopTimes = ((goesTo.map(Option(_)) :+ None)).zip(tripStations).dropWhile {
            case (_, s) => s.stationid != vs
          }
          Trip(tripId, serviceId, stopTimes)
      }
    }

    scalaz.Scalaz.unfold((at, l)) {
      case (at, l) =>
        if(l > 0) {
          val trips = f(at, l)
          val remaining = l - trips.size
          val nextAt = at.plusDays(1).withTimeAtStartOfDay
          Some((trips, (nextAt, remaining)))
        } else None
    }.toList.flatten
  }

  def fetchPreviousTrips(ref: String, vs: String, ve: String, at: DateTime, limit: Option[Int]): List[Trip] = {
    val departure = misc.DateTime.forPattern("HHmm").print(at).toInt
    val filter = (t: DateTime) => {
      val departure = misc.DateTime.forPattern("HHmm").print(t).toInt
      s"head(stoptimes).departure < $departure"
    }
    fetchTrips(ref, vs, ve, at, limit, filter = filter, sortBy = "-last(stoptimes).departure").reverse
  }

  def fetchNextTrips(ref: String, vs: String, ve: String, at: DateTime, limit: Option[Int]): List[Trip] = {
    val filter = (t: DateTime) => {
      val departure = misc.DateTime.forPattern("HHmm").print(t).toInt
      s"head(stoptimes).departure > $departure"
    }
    fetchTrips(ref, vs, ve, at, limit, filter = filter, sortBy = "last(stoptimes).departure")
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
