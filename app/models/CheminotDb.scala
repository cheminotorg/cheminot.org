package models

import play.api.Application
import org.apache.commons.io.FileUtils
import java.io.File

object CheminotDb {

  object Id {

    def next: String = java.util.UUID.randomUUID().toString
  }

  def clean()(implicit app: Application) {
    FileUtils.deleteDirectory(new File(models.Config.bucketPath(app)))
  }

  def del(sessionId: String)(implicit app: Application) = {
    val dir = new File(models.Config.bucketPath + "/" + sessionId)
    if(dir.exists) {
      FileUtils.deleteDirectory(dir)
    }
  }

  def dbPath(sessionId: String)(implicit app: Application): String =
    Config.bucketPath + "/" + sessionId + "/cheminot.db"
}
