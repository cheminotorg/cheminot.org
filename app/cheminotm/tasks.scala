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

  val defaultTimeout = Timeout(30 seconds)

  val executionContext = {
    import scala.concurrent.ExecutionContext
    ExecutionContext.fromExecutor(threadPool)
  }

  var _cheminotDbFile: Option[Array[Byte]] = None

  lazy val cheminotDbFile: Array[Byte] = _cheminotDbFile.getOrElse {
    throw new RuntimeException("cheminotDb not initialized!")
  }

  def init(graphPaths: Config.GraphPaths, calendardatesPaths: Config.CalendarDatesPaths, cheminotDbPath: String) { // Blocking
    import scala.collection.JavaConversions._

    _cheminotDbFile = Some(misc.Files.read(cheminotDbPath))
    m.cheminot.plugin.jni.CheminotLib.load(graphPaths.toSeq, calendardatesPaths.toSeq)
  }

  def shutdown(sessionId: String) {
    CheminotcActor.stop(sessionId)
  }

  def activeSessions: Int = CheminotcActor.actors.size
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
    implicit val timeout = Tasks.defaultTimeout
    implicit val executionContext = Tasks.executionContext
    if(Tasks.activeSessions < Config.maxSessions) {
      (ref(sessionId) ? Messages.OpenConnection(sessionId)).mapTo[Future[String]].flatMap(identity).map(meta => Right(meta))
    } else {
      cheminotorg.Mailer.notify("I can't accept more demo sessions", s"Limit is ${Config.maxSessions} and current value is ${Tasks.activeSessions}")
      Future successful Left(Tasks.Full)
    }
  }

  def lookForBestDirectTrip(sessionId: String, vsId: String, veId: String, at: Int, te: Int)(implicit app: Application): Future[Either[Tasks.Status, String]] = {
    implicit val timeout = Tasks.defaultTimeout
    implicit val executionContext = Tasks.executionContext
    fref(sessionId).flatMap(ref => (ref ? Messages.LookForBestDirectTrip(vsId, veId, at, te)).mapTo[Future[Either[Tasks.Status, String]]].flatMap(identity))
  }

  def lookForBestTrip(sessionId: String, vsId: String, veId: String, at: Int, te: Int, max: Int)(implicit app: Application): Future[Either[Tasks.Status, String]] = {
    implicit val timeout = Tasks.defaultTimeout
    implicit val executionContext = Tasks.executionContext
    val taskCounter = lookForBestTripCounter.get()
    if(taskCounter < Config.lookForBestTripLimit) {
      lookForBestTripCounter.incrementAndGet()
      fref(sessionId).flatMap(ref => (ref ? Messages.LookForBestTrip(vsId, veId, at, te, max)).mapTo[Future[Either[Tasks.Status, String]]]).flatMap(identity) andThen {
        case _ => lookForBestTripCounter.decrementAndGet()
      }
    } else {
      cheminotorg.Mailer.notify("Task lookForBestTrip can't accept anymore requests", s"Limit is ${Config.lookForBestTripLimit} and current value is ${taskCounter}")
      Future successful Left(Tasks.Busy)
    }
  }

  def getStop(sessionId: String, stopId: String)(implicit app: Application): Future[Either[Tasks.Status, String]] = {
    implicit val timeout = Tasks.defaultTimeout
    implicit val executionContext = Tasks.executionContext
    fref(sessionId) flatMap (ref => (ref ? Messages.GetStop(stopId)).mapTo[Either[Tasks.Status, String]])
  }

  def stop(sessionId: String) =
    actors.get(sessionId).foreach(_ ! PoisonPill)
}

class CheminotcActor(sessionId: String, app: Application) extends Actor {

  import CheminotcActor.Messages._

  val Logger = Logging(context.system, this)

  val dbPath = CheminotDB.dbPath(sessionId)(app)

  var meta: Option[String] = None;

  var cheminotcMonitor: Option[ActorRef] = None

  context.setReceiveTimeout(Config.sessionDuration(app))

  def idle: Receive = {

    case OpenConnection(sessionId) =>
      val f = Future {
        misc.Files.write(Tasks.cheminotDbFile, dbPath)
        val metadata = m.cheminot.plugin.jni.CheminotLib.openConnection(dbPath)
        meta = Some(metadata)
        context become ready
        metadata
      }(Tasks.executionContext)
      sender ! f

    case ReceiveTimeout =>
        context.stop(self)

    case _ =>
      sender ! Left(Tasks.NotInitialized)
  }

  def ready: Receive = {

    case m: OpenConnection =>
      sender ! (Future successful meta.getOrElse("null"))

    case LookForBestTrip(vsId, veId, at, te, max) =>
      val c = context
      c become busy
      val f = Future {
        val trip = m.cheminot.plugin.jni.CheminotLib.lookForBestTrip(dbPath, vsId, veId, at, te, max)
        c become ready
        Right(trip)
      }(Tasks.executionContext)
      sender !  f

    case LookForBestDirectTrip(vsId, veId, at, te) =>
      val c = context
      c become busy
      val f = Future {
        val trip = m.cheminot.plugin.jni.CheminotLib.lookForBestDirectTrip(dbPath, vsId, veId, at, te)
        c become ready
        Right(trip)
      }(Tasks.executionContext)
      sender ! f

    case GetStop(stopId) =>
      val stop = m.cheminot.plugin.jni.CheminotLib.getStop(stopId)
      sender ! Right(stop)

    case ReceiveTimeout =>
      context.stop(self)
  }

  def busy: Receive = {

    case m: OpenConnection =>
      sender ! (Future successful meta.getOrElse("null"))

    case LookForBestTrip(vsId, veId, at, te, max) =>
      sender ! (Future successful Left(Tasks.Busy))

    case LookForBestDirectTrip(vsId, veId, at, te) =>
      sender ! (Future successful Left(Tasks.Busy))

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
    m.cheminot.plugin.jni.CheminotLib.closeConnection(dbPath)
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
    implicit val timeout = Tasks.defaultTimeout
    ref(sessionId) match {
      case Some(actorref) => (actorref ? Messages.Abort).mapTo[Either[Tasks.Status, Unit]]
      case None => Future successful Left(Tasks.NotInitialized)
    }
  }

  def trace(sessionId: String)(implicit app: Application): Future[Either[Tasks.Status, Enumerator[String]]] = {
    implicit val timeout = Tasks.defaultTimeout
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

class CheminotcMonitorActor(sessionId: String, app: Application) extends Actor {

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

  def idle: Receive = {

    case Init =>
      context become waiting

    case _ =>
      sender ! Left(Tasks.NotInitialized)
  }

  def pulling: Receive = {

    case Init =>

    case Trace =>

    case TracePulling(_) =>

    case Abort =>
      abort(sender)

    case Shutdown =>
      shutdown()
  }

  def waiting: Receive = {

    case Init =>

    case Trace =>
      trace(sender)

    case TracePulling(channel) =>
      val c = context
      c become pulling
      Future {
        val trace = m.cheminot.plugin.jni.CheminotLib.trace(dbPath);
        if(trace != "[]") {
          channel.push(trace)
        }
        c become waiting
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
