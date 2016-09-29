package org.cheminot.web.storage

import org.joda.time.{DateTime, Duration}
import org.cheminot.web.Params
import org.cheminot.misc
import org.cheminot.web.Config

object Stations {

  def isParent(stationId: String)(implicit config: Config): Boolean = {
    val query = s"match (s:ParentStation {parentstationid: '${stationId}'}) return s;"
    Storage.fetch(Statement(query))(identity).headOption.isDefined
  }

  def fetchById(stationIds: Seq[String])(implicit config: Config): List[models.Station] = {
    if(!stationIds.isEmpty) {
      val ids = stationIds.map(s => s""""$s"""").mkString(",")
      val query =
        s"""MATCH (station:Station)
          WHERE station.stationid IN [$ids]
          return DISTINCT station;
        """;
      Storage.fetch(Statement(query))(_(0).as[models.Station])
    } else Nil
  }
}
