package org.cheminot.web.storage

import org.joda.time.{DateTime, Duration}
import org.cheminot.web.Params
import org.cheminot.misc
import org.cheminot.web.Config

object CalendarDate {

  def fetchByServiceIds(serviceIds: Seq[String])(implicit config: Config): Map[String, List[models.CalendarDate]] = {
    if(!serviceIds.isEmpty) {
      val ids = serviceIds.map(s => s""""$s"""").mkString(",")
      val query = s"""
        MATCH (cd:CalendarDate)
        WHERE cd.serviceid IN [${ids}]
        RETURN cd;
      """;
      println(query)
      Storage.fetch(Statement(query)) { row =>
        models.CalendarDate.json.reads(row(0))
      }.groupBy(_.serviceid)
    } else Map.empty
  }
}
