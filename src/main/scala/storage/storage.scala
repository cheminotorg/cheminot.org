package org.cheminot.storage

import java.util.Locale
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import rapture.json._, jsonBackends.jawn._

object Storage {

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
      val bundleDate = new DateTime(row(0).bundledate.as[Long] * 1000)
      Meta(id, bundleDate, Seq(subset))
    }.groupBy(_.metaid).headOption.flatMap {
      case (_, meta :: rest) => Option(
        meta.copy(subsets = rest.flatMap(_.subsets) ++: meta.subsets)
      )
      case _ => None
    } getOrElse sys.error("Unable to fetch meta")
  }

  def fetchNextTrips(ref: String, vs: String, ve: String, at: DateTime, limit: Option[Int]): List[Trip] = {
    val pattern = DateTimeFormat.forPattern("EEEE").withLocale(Locale.ENGLISH)
    val day = pattern.print(at).toLowerCase
    val start = at.withTimeAtStartOfDay.getMillis / 1000
    val end = at.withTimeAtStartOfDay.plusDays(1).getMillis / 1000

    val query = s"""MATCH path=(trip:Trip)-[:GOES_TO*1..]->(a:Stop { stationid: '${vs}' })-[ways:GOES_TO*1..]->(b:Stop { stationid: '${ve}' })
          WITH path, tail(nodes(path)) as stops, trip, tail(relationships(path)) as stoptimes, last(ways) AS lastWay
          MATCH (trip)-[:SCHEDULED_AT]->(c:Calendar { serviceid: trip.serviceid })-->(cd:CalendarDate)
          WHERE (c.${day} = false AND c.startdate <= ${start} AND c.enddate > ${end}) OR (c)-[:ON]->(:CalendarDate { date: ${start} })
          RETURN trip, stops, stoptimes
          ORDER BY lastWay.arrival
          LIMIT ${limit.getOrElse(10)};
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
        Trip(tripId, serviceId, (goesTo).zip(tripStations))
    }
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
