package org.cheminot.site.storage

import rapture.json._, jsonBackends.jawn._

object Storage {

  def fetchNextTrips(): List[Trip] = {
    val query =
      """MATCH path=(trip:Trip)-[:GOES_TO*1..]->(a:Stop { stationid: '8739100' })-[ways:GOES_TO*1..]->(b:Stop { stationid: '8739400' })
         WITH path, tail(nodes(path)) as stops, trip, tail(relationships(path)) as stoptimes, last(ways) AS lastWay
         MATCH (trip)-[:SCHEDULED_AT]->(c:Calendar { serviceid: trip.serviceid })-->(cd:CalendarDate)
         WHERE (c.sunday = false AND c.startdate < 1443311000 AND c.enddate > 1443311000) OR (c)-[:ON]->(:CalendarDate { date: 1442793600 })
         RETURN trip, stops, stoptimes
         ORDER BY lastWay.arrival
      """

    val response = Cypher.commit(Statement(query))

    for {
      result <- response.results.as[List[Json]]
      data <- result.data.as[List[Json]]
    } yield {
      val row = data.row.as[List[Json]]
      val tripId = row(0).tripid.as[String]
      val serviceId = row(0).serviceid.as[String]
      val stops = row(1).as[List[Stop]]
      val goesTo = row(2).as[List[GoesTo]]
      Trip(tripId, serviceId, goesTo.zip(stops))
    }
  }
}
