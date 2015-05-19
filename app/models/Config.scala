package models

import play.api.{ Play, Application }
import scala.concurrent.duration._
import play.api.Logger

object Config {

  def domain(implicit app: Application): String =
    Play.configuration(app).getString("domain").getOrElse {
      throw new RuntimeException("Please specify `domain` configuration.")
    }

  def cheminotcPath(implicit app: Application): String =
    Play.configuration(app).getString("cheminotc.path").getOrElse {
      app.getFile("/data/libcheminot.so").getAbsolutePath
    }

  def calendardatesPath(implicit app: Application): String =
    Play.configuration(app).getString("calendardates.path").getOrElse {
      app.getFile("/data/calendardates").getAbsolutePath
    }

  def graphPath(implicit app: Application): String =
    Play.configuration(app).getString("graph.path").getOrElse {
      app.getFile("/data/graph").getAbsolutePath
    }

  def cheminotDbPath(implicit app: Application): String =
    Play.configuration(app).getString("cheminotdb.path").getOrElse {
      app.getFile("/data/cheminot.db").getAbsolutePath
    }

  def bucketPath(implicit app: Application): String =
    Play.configuration(app).getString("bucket.path").getOrElse {
      app.getFile("/data/bucket").getAbsolutePath
    }

  def sessionDuration(implicit app: Application): FiniteDuration = {
    val duration = Play.configuration(app).getLong("session.duration").getOrElse {
      (3600 / 2).toLong
    }
    duration seconds
  }

  def maxTasks(implicit app: Application): Int =
    Play.configuration(app).getInt("tasks.max") getOrElse 10

  def cheminotmPath(implicit app: Application): String =
    Play.configuration(app).getString("cheminotm.path").getOrElse {
      app.getFile("/data/app").getAbsolutePath
    }

  def print(implicit app: Application) {
    Logger.info("---------------------------")
    Logger.info("[CONFIGURATION]")
    Logger.info("cheminotc.path: " + cheminotcPath)
    Logger.info("calendardates.path: " + calendardatesPath)
    Logger.info("graph.path: " + graphPath)
    Logger.info("cheminotdb.path: " + cheminotDbPath)
    Logger.info("bucket.path: " + bucketPath)
    Logger.info("session.duration: " + sessionDuration)
    Logger.info("tasks.max: " + maxTasks)
    Logger.info("cheminotm.path: " + cheminotmPath)
    Logger.info("---------------------------")
  }
}
