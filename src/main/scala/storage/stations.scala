package org.cheminot.web.storage

import org.cheminot.web.Config

object Stations {

  private var PARENT_STATIONS: Option[Set[String]] = None

  def initCache()(implicit config: Config): Unit =
    if(PARENT_STATIONS.isEmpty) {
      val stationIds = Storage.fetch(Statement("MATCH (s:ParentStation) RETURN s")) { row =>
        row(0).parentstationid.as[String]
      }
      PARENT_STATIONS = Some(stationIds.toSet)
    } else {
      sys.error("You can only init cache one time")
    }

  def isParent(stationId: String)(implicit config: Config): Boolean = {
    PARENT_STATIONS.map(_.contains(stationId)) getOrElse {
      sys.error("Please init parent stations cache")
    }
  }

  def fetchById(stationIds: Seq[String])(implicit config: Config): List[models.Station] = {
    if(!stationIds.isEmpty) {
      val ids = stationIds.map(s => s""""$s"""").mkString(",")
      val query = s"""
        MATCH (station:Station)
        WHERE station.stationid IN [$ids]
        RETURN DISTINCT station;
      """;
      Storage.fetch(Statement(query))(_(0).as[models.Station])
    } else Nil
  }
}
