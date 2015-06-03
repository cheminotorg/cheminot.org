package models

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import akka.actor.{ Actor, ActorRef, Props }
import akka.event.Logging
import play.api.Application
import play.api.libs.concurrent.Akka
import play.api.libs.json._

object Metrics {

  object cheminotm {

    def threadsCounter(i: Int)(implicit app: Application) {
      MetricsActor.send(Json.obj(
        "cheminotm" -> Json.obj(
          "threadsCounter" -> i
        )
      ))
    }
  }
}

object MetricsActor {

  private var maybeRef: Option[ActorRef] = None

  def prop(implicit app: Application) =
    Props(classOf[MetricsActor], app)

  def ref(implicit app: Application) = maybeRef.getOrElse {
    val r = Akka.system.actorOf(prop(app), "metrics")
    Akka.system.scheduler.schedule(0 milliseconds, models.Config.metricsPeriod, r, Send)
    maybeRef = Some(r)
    r
  }

  def send(value: JsObject)(implicit app: Application) {
    ref ! AddUp(value)
  }

  sealed trait Message
  case object Send extends Message
  case class AddUp(value: JsObject) extends Message
}

class MetricsActor(implicit app: Application) extends Actor {

  import MetricsActor._

  val Logger = Logging(context.system, this)

  var maybeMetrics: Option[JsObject] = None

  def receive = {

    case Send =>
      maybeMetrics.foreach { metrics =>
        misc.Dweet.send(metrics)
      }

    case AddUp(metric) =>
      maybeMetrics.map { metrics =>
        maybeMetrics = Some(metrics.deepMerge(metric))
      } getOrElse {
        maybeMetrics = Some(metric)
      }
  }

  override def preStart() = {
    Logger.info("[MetricsActor] Starting")
  }

  override def postStop(): Unit = {
    Logger.info("[MetricsActor] Shutting down")
  }
}
