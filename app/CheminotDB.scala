package cheminotorg

import play.api.Application
import org.apache.commons.io.FileUtils
import java.io.File

object CheminotDB {

  object Id {

    def next: String = java.util.UUID.randomUUID().toString
  }

  def clean()(implicit app: Application) {
    FileUtils.deleteDirectory(new File(Config.bucketPath(app)))
  }

  def del(sessionId: String)(implicit app: Application) = {
    val dir = new File(Config.bucketPath + "/" + sessionId)
    if(dir.exists) {
      FileUtils.deleteDirectory(dir)
    }
  }

  def dbPath(sessionId: String)(implicit app: Application): String =
    Config.bucketPath + "/" + sessionId + "/cheminot.db"
}
