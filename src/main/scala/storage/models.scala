package org.cheminot.storage

import org.joda.time.DateTime
import rapture.core._
import rapture.json._

case class Meta(metaid: String, bundledate: DateTime, subsets: Seq[MetaSubset])

object MetaSubset {

  def asDateTimeOpt(json: Json): Option[DateTime] =
    json.as[Option[Long]].map(t => new DateTime(t * 1000))

  def fromJson(json: Json): MetaSubset =
    MetaSubset(
      json.metasubsetid.as[String],
      json.metasubsetname.as[String],
      asDateTimeOpt(json.updateddate),
      asDateTimeOpt(json.startdate),
      asDateTimeOpt(json.enddate)
    )
}

case class MetaSubset(metasubsetid: String, metasubsetname: String, updateddate: Option[DateTime], startdate: Option[DateTime], enddate: Option[DateTime])

case class Station(stationid: String, name: String, lat: Double, lng: Double)

case class Stop(stopid: String, stationid: String, parentid: Option[String])

case class GoesTo(arrival: Int, departure: Option[Int])

case class Trip(tripid: String, serviceid: String, stopTimes: List[(GoesTo, Station)])
