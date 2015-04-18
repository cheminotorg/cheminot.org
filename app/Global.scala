import play.api._

object Global extends GlobalSettings {

  override def beforeStart(app: Application) {

    System.load(models.Config.cheminotcPath(app))
  }
}
