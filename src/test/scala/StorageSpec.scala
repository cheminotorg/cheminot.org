package org.cheminot.web.tests

import org.jsoup._
import org.joda.time.DateTime
import scala.collection.JavaConverters._
import org.scalatest._
import rapture.json._, jsonBackends.jawn._
import rapture.io._
import rapture.fs._
import rapture.uri._
import rapture.codec._, encodings.`UTF-8`._
import org.cheminot.misc
import org.cheminot.web.{storage, api, Params, Config}
import org.cheminot.web.storage.Storage

abstract class CheminotSpec extends FlatSpec

object Stations {
  lazy val chartres = "8739400"
  lazy val parisMont = "8739100"
  lazy val paris = "PARISXX"
}

class StorageSpec extends CheminotSpec {

  implicit val config = Config.default

  behavior of "fetchNextTrips"

  it should "find next 20 trips from Chartres to Paris Montparnasse" in {
    val at = misc.DateTime.parseOrFail("2016-03-02T04:00:00.000+01:00")
    val params = Params.FetchTrips(
      vs = Stations.chartres,
      ve = Stations.paris,
      at = at,
      limit = Option(20),
      previous = false
    )
    val trips = Storage.fetchNextTrips(params).map(api.Trip.apply(_, at))
    val testTrips = Trips.fromData("test1.json")
    assert(trips.size === testTrips.size)
    trips.zip(testTrips).foreach {
      case (a, b) =>
        assert(a === b)
    }
  }
}

object Trips {

  lazy val dir =
    org.cheminot.misc.File.currentDir / "data" / "test"

  def fromData(name: String): List[api.Trip] = {
    val file = dir / name
    val json = Json.parse(file.slurp[Char])
    json.results.as[List[Json]].map(api.Trip.fromJson)
  }
}

object CaptainTrain {

  private def fetchTrips(vs: String, ve: String, date: DateTime): Seq[Seq[(String, DateTime)]] = {
    val datestr = misc.DateTime.forPattern("yyyy-MM-dd").print(date)
    val url = s"https://horaires.captaintrain.com/trains/${vs}/${ve}?date=${datestr}"
    val doc = Jsoup.connect(url).get()
    val itineraries = doc.getElementById("itineraries").getElementsByClass("itinerary")
    itineraries.asScala.map { itinerary =>
      itinerary.getElementsByTag("tr").asScala.toList.map { tr =>
        tr.getElementsByTag("td").asScala.map(_.text).toList match {
          case time :: station :: _ =>
            val t = misc.DateTime.forPattern("HH:mm").parseDateTime(time)
            station -> date.withTime(t.toLocalTime)
          case x =>
            sys.error(s"Unable to read stop time $x")
        }
      }
    }
  }

  def matches(vs: String, ve: String, date: DateTime)(ttt: List[storage.Trip]): Boolean = {
    val trips = ttt.map(api.Trip.apply(_, date))
    val tripsFromCapitaineTrain = {
      val upperBound = date.withTime(23, 59, 59, 999)
      val lowerBound = date.withTimeAtStartOfDay
      val boundTrips = if(trips.exists(trip => trip.stopTimes.lastOption.exists(_.arrival.isAfter(upperBound)))) {
        fetchTrips(vs, ve, date.plusDays(1))
      } else if(trips.exists(trip => trip.stopTimes.headOption.exists(_.arrival.isBefore(lowerBound)))) {
        fetchTrips(vs, ve, date.minusDays(1))
      } else Seq.empty
      (boundTrips ++: fetchTrips(vs, ve, date)).sortBy { stopTimes =>
        stopTimes.headOption.map(_._2.getMillis) getOrElse sys.error("Unable to sort trip")
      }
    }
    trips.forall { ta =>
      tripsFromCapitaineTrain.exists { tb =>
        (for {
          departureA <- ta.stopTimes.headOption.flatMap(_.departure)
          departureB <- tb.headOption.map(_._2)
          arrivalA <- ta.stopTimes.lastOption.map(_.arrival)
          arrivalB <- tb.lastOption.map(_._2)
          if ta.stopTimes.size == tb.size
        } yield {
          (departureA == departureB) && (arrivalA == arrivalB)
        }) getOrElse false
      }
    }
  }
}
