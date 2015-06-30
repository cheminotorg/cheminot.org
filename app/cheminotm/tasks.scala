package cheminotorg
package cheminotm

import java.util.concurrent.{ Executors, ThreadPoolExecutor }
import java.io.File
import play.api.{ Play, Application }
import play.api.libs.concurrent.Akka
import akka.actor.{ Actor, ActorRef, Props, Cancellable, PoisonPill, ActorSystem, ReceiveTimeout }
import scala.concurrent.duration._
import play.api.libs.iteratee. { Concurrent, Enumerator, Input }
import akka.pattern.ask
import akka.util.Timeout
import akka.event.Logging
import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import akka.dispatch.UnboundedPriorityMailbox
import akka.dispatch.PriorityGenerator

object Tasks {

  sealed trait Status
  case object NotInitialized extends Status
  case object Busy extends Status
  case object Full extends Status

  val threadPool = Executors.newFixedThreadPool(Config.threadPoolSize(Play.current)).asInstanceOf[ThreadPoolExecutor]

  val executionContext = {
    import scala.concurrent.ExecutionContext
    ExecutionContext.fromExecutor(threadPool)
  }

  var _cheminotDb: Option[Array[Byte]] = None

  lazy val cheminotDb: Array[Byte] = _cheminotDb.getOrElse {
    throw new RuntimeException("cheminotDb not initialized!")
  }

  def init(graphPath: String, calendardatesPath: String, cheminotDbPath: String) { // Blocking
    _cheminotDb = Some(misc.Files.read(cheminotDbPath))
    m.cheminot.plugin.jni.CheminotLib.load(graphPath, calendardatesPath)
  }

  def shutdown(sessionId: String) {
    CheminotcActor.stop(sessionId)
  }

  def activeSessions: Int = CheminotcActor.actors.size
}

trait HandlingFailure {
  self: Actor =>

  def WithFailure(f: PartialFunction[Any, Unit]): PartialFunction[Any, Unit] = {
    case x =>
      try { f(x) } catch {
        case e: Exception =>
          e.printStackTrace
          sender ! akka.actor.Status.Failure(e)
      }
  }
}

object CheminotcActor {

  val actors = TrieMap.empty[String, ActorRef]

  val lookForBestTripCounter = new java.util.concurrent.atomic.AtomicInteger(0)

  object Messages {
    sealed trait Event
    case class OpenConnection(sessionId: String) extends Event
    case class LookForBestTrip(vsId: String, veId: String, at: Int, te: Int, max: Int) extends Event
    case class LookForBestDirectTrip(vsId: String, veId: String, at: Int, te: Int) extends Event
    case class GetStop(stopId: String) extends Event
  }

  def hasSession(sessionId: String): Boolean =
    actors.get(sessionId).isDefined

  def ref(sessionId: String)(implicit app: Application): ActorRef = {
    actors.get(sessionId).getOrElse {
      val r = Akka.system.actorOf(prop(sessionId), s"cheminotc-${sessionId}")
      actors += sessionId -> r
      CheminotcMonitorActor.init(sessionId)
      r
    }
  }

  def fref(sessionId: String)(implicit app: Application): Future[ActorRef] = {
    implicit val executionContext = Tasks.executionContext
    actors.get(sessionId).map(Future.successful).getOrElse {
      val r = Akka.system.actorOf(prop(sessionId), s"cheminotc-${sessionId}")
      actors += sessionId -> r
      CheminotcMonitorActor.init(sessionId)
      openConnection(sessionId).map(_ => r)
    }
  }

  def prop(sessionId: String)(implicit app: Application) =
    Props(classOf[CheminotcActor], sessionId, app)

  def openConnection(sessionId: String)(implicit app: Application): Future[Either[Tasks.Status, String]] = {
    implicit val timeout = Timeout(30 seconds)
    if(Tasks.activeSessions < Config.maxSessions) {
      (ref(sessionId) ? Messages.OpenConnection(sessionId)).mapTo[String].map(meta => Right(meta))(Tasks.executionContext)
    } else {
      Future successful Left(Tasks.Full)
    }
  }

  def lookForBestDirectTrip(sessionId: String, vsId: String, veId: String, at: Int, te: Int)(implicit app: Application): Future[Either[Tasks.Status, String]] = {
    implicit val timeout = Timeout(30 seconds)
    implicit val executionContext = Tasks.executionContext
    fref(sessionId) flatMap (ref => (ref ? Messages.LookForBestDirectTrip(vsId, veId, at, te)).mapTo[Either[Tasks.Status, String]])
  }

  def lookForBestTrip(sessionId: String, vsId: String, veId: String, at: Int, te: Int, max: Int)(implicit app: Application): Future[Either[Tasks.Status, String]] = {
    implicit val timeout = Timeout(Config.lookForBestTripTimeout)
    implicit val executionContext = Tasks.executionContext
    if(lookForBestTripCounter.get() < Config.lookForBestTripLimit) {
      lookForBestTripCounter.incrementAndGet()
      fref(sessionId) flatMap (ref => (ref ? Messages.LookForBestTrip(vsId, veId, at, te, max)).mapTo[Either[Tasks.Status, String]]) andThen {
        case _ => lookForBestTripCounter.decrementAndGet()
      }
    } else {
      Future successful Left(Tasks.Busy)
    }
  }

  def getStop(sessionId: String, stopId: String)(implicit app: Application): Future[Either[Tasks.Status, String]] = {
    implicit val timeout = Timeout(30 seconds)
    implicit val executionContext = Tasks.executionContext
    fref(sessionId) flatMap (ref => (ref ? Messages.GetStop(stopId)).mapTo[Either[Tasks.Status, String]])
  }

  def stop(sessionId: String) =
    actors.get(sessionId).foreach(_ ! PoisonPill)
}

class CheminotcActor(sessionId: String, app: Application) extends Actor with HandlingFailure {

  import CheminotcActor.Messages._

  val Logger = Logging(context.system, this)

  val dbPath = CheminotDB.dbPath(sessionId)(app)

  var meta: Option[String] = None;

  var cheminotcMonitor: Option[ActorRef] = None

  context.setReceiveTimeout(Config.sessionDuration(app))

  def idle: Receive = WithFailure {

    case OpenConnection(sessionId) =>
      misc.Files.write(Tasks.cheminotDb, dbPath)
      val metadata = m.cheminot.plugin.jni.CheminotLib.openConnection(dbPath)
      meta = Some(metadata)
      context become ready
      sender ! metadata

    case ReceiveTimeout =>
        context.stop(self)

    case _ =>
      sender ! Left(Tasks.NotInitialized)
  }

  def ready: Receive = WithFailure {

    case m: OpenConnection =>
      sender ! meta.getOrElse("null")

    case LookForBestTrip(vsId, veId, at, te, max) =>
      val s = sender
      context become busy
      Future {
        val trip = m.cheminot.plugin.jni.CheminotLib.lookForBestTrip(dbPath, vsId, veId, at, te, max)
        context become ready
        s ! Right(trip)
      }(Tasks.executionContext)

    case LookForBestDirectTrip(vsId, veId, at, te) =>
      val s = sender
      context become busy
      Future {
        val trip = m.cheminot.plugin.jni.CheminotLib.lookForBestDirectTrip(dbPath, vsId, veId, at, te)
        context become ready
        s ! Right(trip)
      }(Tasks.executionContext)

    case GetStop(stopId) =>
      val stop = m.cheminot.plugin.jni.CheminotLib.getStop(stopId)
      sender ! Right(stop)

    case ReceiveTimeout =>
      context.stop(self)
  }

  def busy: Receive = WithFailure {

    case m: OpenConnection =>
      sender ! meta.getOrElse("null")

    case LookForBestTrip(vsId, veId, at, te, max) =>
      sender ! Left(Tasks.Busy)

    case LookForBestDirectTrip(vsId, veId, at, te) =>
      sender ! Left(Tasks.Busy)

    case GetStop(stopId) =>
      sender ! Left(Tasks.Busy)

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
    CheminotDB.del(sessionId)(app)
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

  def ref(sessionId: String)(implicit app: Application): Option[ActorRef] = {
    if(CheminotcActor.hasSession(sessionId)) {
      actors.get(sessionId).orElse {
        val r = Akka.system.actorOf(prop(sessionId).withDispatcher("cheminotcmonitor-dispatcher"), s"cheminotcMonitor-${sessionId}")
        actors += sessionId -> r
        Some(r)
      }
    } else None
  }

  def stop(sessionId: String) =
    actors.get(sessionId).foreach(_ ! Messages.Shutdown)

  def init(sessionId: String)(implicit app: Application) {
    ref(sessionId).foreach(_ ! Messages.Init)
  }

  def abort(sessionId: String)(implicit app: Application): Future[Either[Tasks.Status, Unit]] = {
    implicit val timeout = Timeout(30 seconds)
    ref(sessionId) match {
      case Some(actorref) => (actorref ? Messages.Abort).mapTo[Either[Tasks.Status, Unit]]
      case None => Future successful Left(Tasks.NotInitialized)
    }
  }

  def trace(sessionId: String)(implicit app: Application): Future[Either[Tasks.Status, Enumerator[String]]] = {
    implicit val timeout = Timeout(30 seconds)
    ref(sessionId) match {
      case Some(actorref) => (actorref ? Messages.Trace).mapTo[Either[Tasks.Status, Enumerator[String]]]
      case None => Future successful Left(Tasks.NotInitialized)
    }
  }

  class CheminotcMonitorMailbox(settings: ActorSystem.Settings, config: com.typesafe.config.Config) extends UnboundedPriorityMailbox(
    PriorityGenerator {

      case Messages.Init | Messages.Abort | Messages.Shutdown => 0

      case Messages.Trace => 3

      case _ => 1
    }
  )
}

class CheminotcMonitorActor(sessionId: String, app: Application) extends Actor with HandlingFailure {

  import CheminotcMonitorActor.Messages._

  val Logger = Logging(context.system, this)

  val dbPath = CheminotDB.dbPath(sessionId)(app)

  var enumerator: Option[Enumerator[String]] = None

  var cancellableScheduler: Option[Cancellable] = None

  def abort(sender: ActorRef) {
    m.cheminot.plugin.jni.CheminotLib.abort(dbPath)
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
      val cancellable = Akka.system(app).scheduler.schedule(0 milliseconds, Config.tracePullingPeriod(app), self, TracePulling(channel))(Tasks.executionContext)
      cancellableScheduler = Some(cancellable)
      producer
    }
    sender ! Right(stream)
  }

  def idle: Receive = WithFailure {

    case Init =>
      context become waiting

    case _ =>
      sender ! Left(Tasks.NotInitialized)
  }

  def pulling: Receive = WithFailure {

    case Init =>

    case Trace =>

    case TracePulling(_) =>

    case Abort =>
      abort(sender)

    case Shutdown =>
      shutdown()
  }

  def waiting: Receive = WithFailure {

    case Init =>

    case Trace =>
      trace(sender)

    case TracePulling(channel) =>
      context become pulling
      Future {
        val trace = m.cheminot.plugin.jni.CheminotLib.trace(dbPath);
        if(trace != "[]") {
          channel.push(trace)
        }
        context become waiting
      }(Tasks.executionContext)

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
