package org.cheminot.web.tests

import org.jsoup._
import org.joda.time.DateTime
import scala.collection.JavaConverters._
import org.scalatest._
import org.cheminot.web.storage
import org.cheminot.web.api
import org.cheminot.web.misc
import org.cheminot.web.Params
import org.cheminot.web.storage.Storage

abstract class CheminotSpec extends FlatSpec
// with Matchers with OptionValues with Inside with Inspectors

object Stations {
  lazy val chartres = "8739400"
  lazy val parisMont = "8739100"
  lazy val paris = "PARISXX"
}

object Trip {

  private def fetchTrips(vs: String, ve: String, date: DateTime): Seq[Seq[(String, DateTime)]] = {
    val datestr = misc.DateTime.forPattern("yyyy-MM-dd").print(date)
    val url = s"https://horaires.captaintrain.com/trains/${vs}/${ve}?date=${datestr}"
    println(url)
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

class StorageSpec extends CheminotSpec {

  behavior of "fetchNextTrips"

  it should "find next 10 trips from Chartres to Paris Montparnasse" in {
    val params = Params.FetchTrips(
      vs = Stations.chartres,
      ve = Stations.paris,
      at = misc.DateTime.parseOrFail("2016-02-24T22:33:00.000+01:00"),
      limit = Option(10),
      previous = false
    )
    val trips = Storage.fetchNextTrips(params)
    assert(Trip.matches("chartres", "paris", params.at)(trips))
  }
}