package cheminotm

import java.io.File
import play.api.Application
import play.api.libs.concurrent.Akka
import akka.actor.{ Actor, ActorRef, Props, Cancellable, PoisonPill, ActorSystem, ReceiveTimeout }
import scala.concurrent.duration._
import play.api.libs.iteratee. { Concurrent, Enumerator, Input }
import akka.pattern.ask
import akka.util.Timeout
import akka.event.Logging
import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import models.Config
import akka.dispatch.UnboundedPriorityMailbox
import akka.dispatch.PriorityGenerator

sealed trait Status
case object NotInitialized extends Status
case object Full extends Status

object Tasks {

  val executionContext = {
    import scala.concurrent.ExecutionContext
    ExecutionContext.fromExecutor(java.util.concurrent.Executors.newFixedThreadPool(10))
  }

  def shutdown(sessionId: String) = {
    CheminotcActor.stop(sessionId)
  }
}

object CheminotcActor {

  val actors = TrieMap.empty[String, ActorRef]

  object Messages {

    sealed trait Event
    case class Init(sessionId: String, graphPath: String, calendardatesPath: String) extends Event
    case class LookForBestTrip(vsId: String, veId: String, at: Int, te: Int, max: Int) extends Event
    case class LookForBestDirectTrip(vsId: String, veId: String, at: Int, te: Int) extends Event
  }

  def ref(sessionId: String)(implicit app: Application) = {
    actors.get(sessionId).getOrElse {
      val r = Akka.system.actorOf(prop(sessionId), s"cheminotc-${sessionId}")
      actors += sessionId -> r
      r
    }
  }

  def prop(sessionId: String)(implicit app: Application) =
    Props(classOf[CheminotcActor], sessionId, app)

  def init(sessionId: String, graphPath: String, calendardatesPath: String)(implicit app: Application): Future[Either[Status, String]] = {
    implicit val timeout = Timeout(30 seconds)
    if(actors.size < Config.maxTasks) {
      (ref(sessionId) ? Messages.Init(sessionId, graphPath, calendardatesPath)).mapTo[String].map { meta =>
        CheminotcMonitorActor.init(sessionId)
        Right(meta)
      }(Tasks.executionContext)
    } else {
      Future successful Left(Full)
    }
  }

  def lookForBestDirectTrip(sessionId: String, vsId: String, veId: String, at: Int, te: Int)(implicit app: Application): Future[Either[Status, String]] = {
    implicit val timeout = Timeout(30 seconds)
    (ref(sessionId) ? Messages.LookForBestDirectTrip(vsId, veId, at, te)).mapTo[Either[Status, String]]
  }

  def lookForBestTrip(sessionId: String, vsId: String, veId: String, at: Int, te: Int, max: Int)(implicit app: Application): Future[Either[Status, String]] = {
    implicit val timeout = Timeout(2 minutes)
    (ref(sessionId) ? Messages.LookForBestTrip(vsId, veId, at, te, max)).mapTo[Either[Status, String]]
  }

  def stop(sessionId: String) =
    actors.get(sessionId).foreach(_ ! PoisonPill)
}


class CheminotcActor(sessionId: String, app: Application) extends Actor {

  import CheminotcActor.Messages._

  val Logger = Logging(context.system, this)

  val dbPath = models.CheminotDb.dbPath(sessionId)(app)

  var meta: Option[String] = None;

  var cheminotcMonitor: Option[ActorRef] = None

  context.setReceiveTimeout(Config.sessionDuration(app))

  def idle: Receive = {

    case Init(sessionId, graphPath, calendardatesPath) =>
      misc.Files.copy(Config.cheminotDbPath(app), dbPath)
      val metadata = m.cheminot.plugin.jni.CheminotLib.init(dbPath, graphPath, calendardatesPath)
      meta = Some(metadata)
      context become busy
      sender ! metadata

    case ReceiveTimeout =>
        context.stop(self)

    case _ =>
      sender ! Left(NotInitialized)
  }

  def busy: Receive = {

    case m: Init =>
      sender ! meta.getOrElse("null")

    case LookForBestTrip(vsId, veId, at, te, max) =>
      val trip = m.cheminot.plugin.jni.CheminotLib.lookForBestTrip(vsId, veId, at, te, max)
      sender ! Right(trip)

    case LookForBestDirectTrip(vsId, veId, at, te) =>
      val trip = m.cheminot.plugin.jni.CheminotLib.lookForBestDirectTrip(vsId, veId, at, te)
      sender ! Right(trip)

    case ReceiveTimeout =>
        context.stop(self)
  }

  def receive = idle


  override def preStart() = {
    Logger.info(s"[CheminotcActor] Starting ${sessionId}")
  }

  override def postStop(): Unit = {
    Logger.info(s"[CheminotcActor] Shutting down ${sessionId}")
    CheminotcActor.actors -= sessionId
    CheminotcMonitorActor.stop(sessionId)
    models.CheminotDb.del(sessionId)(app)
  }
}

object CheminotcMonitorActor {

  object Messages {
    sealed trait Event
    case object Init extends Event
    case object Abort extends Event
    case object Trace extends Event
    case object Shutdown extends Event
    case class TracePulling(channel: Concurrent.Channel[String]) extends Event
  }

  val actors = TrieMap.empty[String, ActorRef]

  def prop(sessionId: String)(implicit app: Application) =
    Props(classOf[CheminotcMonitorActor], sessionId, app)

  def ref(sessionId: String)(implicit app: Application) = {
    actors.get(sessionId).getOrElse {
      val r = Akka.system.actorOf(prop(sessionId).withDispatcher("cheminotcmonitor-dispatcher"), s"cheminotcMonitor-${sessionId}")
      actors += sessionId -> r
      r
    }
  }

  def stop(sessionId: String) =
    actors.get(sessionId).foreach(_ ! Messages.Shutdown)

  def init(sessionId: String)(implicit app: Application) {
    ref(sessionId) ! Messages.Init
  }

  def abort(sessionId: String)(implicit app: Application): Future[Either[Status, Unit]] = {
    implicit val timeout = Timeout(30 seconds)
    (ref(sessionId) ? Messages.Abort).mapTo[Either[Status, Unit]]
  }

  def trace(sessionId: String)(implicit app: Application): Future[Either[Status, Enumerator[String]]] = {
    implicit val timeout = Timeout(30 seconds)
    (ref(sessionId) ? Messages.Trace).mapTo[Either[Status, Enumerator[String]]]
  }

  class CheminotcMonitorMailbox(settings: ActorSystem.Settings, config: com.typesafe.config.Config) extends UnboundedPriorityMailbox(
    PriorityGenerator {

      case Messages.Init | Messages.Abort | Messages.Shutdown => 0

      case Messages.Trace => 3

      case _ => 1
    }
  )
}

class CheminotcMonitorActor(sessionId: String, app: Application) extends Actor {

  import CheminotcMonitorActor.Messages._

  val Logger = Logging(context.system, this)

  val dbPath = models.CheminotDb.dbPath(sessionId)(app)

  var enumerator: Option[Enumerator[String]] = None

  var cancellableScheduler: Option[Cancellable] = None

  def abort(sender: ActorRef) {
    m.cheminot.plugin.jni.CheminotLib.abort()
    sender ! Right(Unit)
  }

  def shutdown() {
    cancellableScheduler foreach (_.cancel)
    context.stop(self)
  }

  def trace(sender: ActorRef) {
    val stream = enumerator.getOrElse {
      val (producer, channel) = Concurrent.broadcast[String]
      enumerator = Some(producer)
      val cancellable = Akka.system(app).scheduler.schedule(0 milliseconds, 0.2 seconds, self, TracePulling(channel))(Tasks.executionContext)
      cancellableScheduler = Some(cancellable)
      producer
    }
    sender ! Right(stream)
  }

  def idle: Receive = {

    case Init =>
      context become waiting

    case _ =>
      sender ! Left(NotInitialized)
  }

  def pulling: Receive = {

    case TracePulling(_) =>

    case Abort =>
      abort(sender)

    case Shutdown =>
      shutdown()
  }

  def waiting: Receive = {

    case Trace =>
      trace(sender)

    case TracePulling(channel) =>
      context become pulling
      val trace = m.cheminot.plugin.jni.CheminotLib.trace();
      if(trace != "[]") {
        channel.push(trace)
      }
      context become waiting

    case Abort =>
      abort(sender)

    case Shutdown =>
      shutdown()

  }

  def receive = idle

  override def preStart() = {
    Logger.info(s"[CheminotcMonitorActor] Starting ${sessionId}")
  }

  override def postStop(): Unit = {
    Logger.info(s"[CheminotcMonitorActor] Shutting down ${sessionId}")
    CheminotcMonitorActor.actors -= sessionId
  }
}
