import play.api._

object Global extends GlobalSettings {

  override def beforeStart(app: Application) {

    models.Config.print(app)

    System.load(models.Config.cheminotcPath(app))

    models.CheminotDb.clean()(app)
  }
}
