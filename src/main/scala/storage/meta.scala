package org.cheminot.web.storage

import org.joda.time.{DateTime, Duration}
import org.cheminot.web.Params
import org.cheminot.misc
import org.cheminot.web.Config

object Meta {

  def fetch()(implicit config: Config): models.Meta = {
    val query = "match p=(s:Meta)-[:HAS]->(m:MetaSubset) return s as Meta, m as MetaSubset;"
    Storage.fetch(Statement(query)) { row =>
      val subset = models.MetaSubset.fromJson(row(1))
      val id = row(0).metaid.as[String]
      val bundleDate = misc.DateTime.fromSecs(row(0).bundledate.as[Long])
      models.Meta(id, bundleDate, Seq(subset))
    }.groupBy(_.metaid).headOption.flatMap {
      case (_, meta :: rest) => Option(
        meta.copy(subsets = rest.flatMap(_.subsets) ++: meta.subsets)
      )
      case _ => None
    } getOrElse sys.error("Unable to fetch meta")
  }
}
