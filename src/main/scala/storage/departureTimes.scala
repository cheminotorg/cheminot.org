package org.cheminot.web.storage

import org.joda.time.{DateTime, Duration}
import rapture.json._, jsonBackends.jawn._
import org.cheminot.web.Params
import org.cheminot.misc
import org.cheminot.web.Config

object DepartureTimes {

  def search(params: Params.SearchDepartureTimes)(implicit config: Config): List[models.DepartureTime] = {
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

    Storage.fetch(Statement(query)) { row =>
      val minutes = Duration.standardMinutes(row(0).as[Long]).toStandardMinutes
      val calendar = models.Calendar.fromJson(row(1).as[Json])
      models.DepartureTime(minutes, calendar)
    }.groupBy(_.at).toList.map {
      case (minutes, departureTimes) =>
        models.DepartureTime(minutes, departureTimes.map(_.calendar).reduce(_ merge _))
    }.sortBy(_.at.getMinutes)
  }
}
