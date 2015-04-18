package models

import play.api.{ Play, Application }

object Config {

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
}
