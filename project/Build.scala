import sbt._, Keys._

object CheminotOrg extends Build {

  lazy val bot = Project(
    id = "cheminotorg",
    base = file("."),
    settings = Defaults.defaultSettings ++ Seq(
      version := "0.1",
      scalaVersion := "2.11.7",
      resolvers += "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/",
      libraryDependencies ++= Seq(
        "com.propensive" %% "rapture" % "2.0.0-M3" exclude("com.propensive", "rapture-json-lift_2.11"),
        "joda-time" % "joda-time" % "2.9.1"
      ),
      scalacOptions ++= Seq("-encoding", "UTF-8", "-feature", "-Xlint", "-Ywarn-unused-import", "-Ywarn-dead-code"),
      unmanagedResourceDirectories in Compile += { baseDirectory.value / "assets" }
    )
  ).settings(com.github.retronym.SbtOneJar.oneJarSettings:_*)
}
