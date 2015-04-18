package cheminotm

import play.api.Application
import play.api.libs.concurrent.Akka
import akka.actor.{ Actor, ActorRef, Props, Cancellable }
import scala.concurrent.duration._
import play.api.libs.iteratee. { Concurrent, Enumerator, Input }
import akka.pattern.ask
import akka.util.Timeout
import scala.collection.concurrent.TrieMap
import scala.concurrent.Future

object CheminotcActor {

  val executionContext = {
    import scala.concurrent.ExecutionContext
    ExecutionContext.fromExecutor(java.util.concurrent.Executors.newFixedThreadPool(10))
  }

  val actors = TrieMap.empty[String, ActorRef]

  object Messages {
    sealed trait Event
    case class Init(dbPath: String, graphPath: String, calendardatesPath: String) extends Event
    case class LookForBestTrip(vsId: String, veId: String, at: Int, te: Int, max: Int) extends Event
    case class LookForBestDirectTrip(vsId: String, veId: String, at: Int, te: Int) extends Event
    case object Abort extends Event
    case object Trace extends Event
    case class TracePulling(channel: Concurrent.Channel[String]) extends Event
  }

  def ref(dbPath: String)(implicit app: Application) = {
    actors.get(dbPath).getOrElse {
      val r = Akka.system.actorOf(prop(dbPath))
      actors += dbPath -> r
      r
    }
  }

  def prop(dbPath: String)(implicit app: Application) =
    Props(classOf[CheminotcActor], dbPath, app)

  def init(dbPath: String, graphPath: String, calendardatesPath: String)(implicit app: Application): Future[String] = {
    implicit val timeout = Timeout(30 seconds)
    (ref(dbPath) ? Messages.Init(dbPath, graphPath, calendardatesPath)).mapTo[String]
  }

  def lookForBestDirectTrip(dbPath: String, vsId: String, veId: String, at: Int, te: Int)(implicit app: Application): Future[String] = {
    implicit val timeout = Timeout(30 seconds)
    (ref(dbPath) ? Messages.LookForBestDirectTrip(vsId, veId, at, te)).mapTo[String]
  }

  def lookForBestTrip(dbPath: String, vsId: String, veId: String, at: Int, te: Int, max: Int)(implicit app: Application): Future[String] = {
    implicit val timeout = Timeout(30 seconds)
    (ref(dbPath) ? Messages.LookForBestTrip(vsId, veId, at, te, max)).mapTo[String]
  }

  def abort(dbPath: String)(implicit app: Application): Future[Unit] = {
    implicit val timeout = Timeout(30 seconds)
    (ref(dbPath) ? Messages.Abort).mapTo[Unit]
  }

  def trace(dbPath: String)(implicit app: Application): Enumerator[String] = {
    implicit val timeout = Timeout(30 seconds)
    Enumerator.flatten((ref(dbPath) ? Messages.Trace).mapTo[Enumerator[String]])
  }
}


class CheminotcActor(dbPath: String, app: Application) extends Actor {

  import CheminotcActor.Messages._

  var enumerator: Enumerator[String] = null
  var cancellableScheduller: Option[Cancellable] = None
  var meta: String = null;

  def idle: Receive = {

    case Init(dbPath, graphPath, calendardatesPath) =>
      meta = m.cheminot.plugin.jni.CheminotLib.init(dbPath, graphPath, calendardatesPath)
      context become busy
      sender ! meta
  }

  def busy: Receive = {

    case Init(dbPath, graphPath, calendardatesPath) =>
      sender ! meta

    case LookForBestTrip(vsId, veId, at, te, max) =>
      val trip = m.cheminot.plugin.jni.CheminotLib.lookForBestTrip(vsId, veId, at, te, max)
      cancellableScheduller.foreach(_.cancel)
      sender ! trip

    case LookForBestDirectTrip(vsId, veId, at, te) =>
      val trip = m.cheminot.plugin.jni.CheminotLib.lookForBestDirectTrip(vsId, veId, at, te)
      sender ! trip

    case Abort =>
      m.cheminot.plugin.jni.CheminotLib.abort()
      cancellableScheduller.foreach(_.cancel)
      sender ! Unit

    case TracePulling(channel) =>
      val trace = m.cheminot.plugin.jni.CheminotLib.trace();
      channel.push(trace)

    case Trace => {
      val onStart = (channel: Concurrent.Channel[String]) => {
        val cancellable = Akka.system(app).scheduler.schedule(0 milliseconds, 1 seconds, self, TracePulling(channel))(CheminotcActor.executionContext)
        cancellableScheduller = Some(cancellable)
        ()
      }

      val onComplete: Unit = {
        println("oncomplete")
      }

      val onError = (message: String, input: Input[String]) => {
        println("onerror")
      }

      enumerator = Concurrent.unicast[String](onStart, onComplete, onError)(CheminotcActor.executionContext)

      sender ! enumerator
    }
  }

  def receive = idle
}
