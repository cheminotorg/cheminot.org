package monitor

import java.lang.management.ManagementFactory
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import akka.actor.{ Actor, ActorRef, Props }
import akka.event.Logging
import play.api.Application
import play.api.libs.concurrent.Akka
import play.api.libs.json._

object Tasks {

  def init(implicit app: Application) {
    MetricsActor.ref
  }
}

object MetricsActor {

  private var maybeRef: Option[ActorRef] = None

  def prop(implicit app: Application) =
    Props(classOf[MetricsActor], app)

  def ref(implicit app: Application) = maybeRef.getOrElse {
    val r = Akka.system.actorOf(prop(app), "metrics")
    Akka.system.scheduler.schedule(0 milliseconds, models.Config.metricsPeriod, r, Update)
    maybeRef = Some(r)
    r
  }

  sealed trait Message
  case object Update extends Message
}

class MetricsActor(implicit app: Application) extends Actor {

  import MetricsActor._

  val Logger = Logging(context.system, this)

  val osStats = ManagementFactory.getOperatingSystemMXBean
  val threadStats = ManagementFactory.getThreadMXBean
  val memoryStats = ManagementFactory.getMemoryMXBean
  val cpuStats = new CPU

  def ready: Receive = {

    case Update =>
      context become busy
      val cpuUsage = cpuStats.getCpuUsage()
      misc.Dweet.send("cheminotorg", Json.obj(
        "general" -> Json.obj(
          "memory" -> memoryStats.getHeapMemoryUsage.getUsed / 1024 / 1024,
          "loadAvg" -> osStats.getSystemLoadAverage.toFloat,
          "nbThreads" -> threadStats.getThreadCount,
          "cpu" -> ((cpuUsage * 1000).round / 10.0).toInt
        ),
        "cheminotm" -> Json.obj(
          "nbTreads" -> cheminotm.Tasks.threadPool.getActiveCount,
          "sessions" -> cheminotm.Tasks.activeSessions
        )
      )).onComplete {
        case _ => context become ready
      }
  }

  def busy: Receive = {
    case _ => Logger.warning("[MetricsActor] Receive message while sending metrics.")
  }

  def receive = ready

  override def preStart() = {
    Logger.info("[MetricsActor] Starting")
  }

  override def postStop(): Unit = {
    Logger.info("[MetricsActor] Shutting down")
  }
}
