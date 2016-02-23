lazy val buildSettings = Seq(
  organization := "org.cheminot",
  version := "0.1.0",
  scalaVersion := "2.11.7"
)

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-Xlint",
    "-Ywarn-dead-code",
    "-Ywarn-unused-import"
  ),
  scalacOptions in (Compile, console) ~= _ filterNot (_ == "-Ywarn-unused-import")
)

lazy val cheminotorgSettings = buildSettings ++ commonSettings

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(name := "webapp").
  settings(cheminotorgSettings:_*).
  settings(libraryDependencies += "com.propensive" %% "rapture" % "2.1.0-SNAPSHOT" exclude("com.propensive", "rapture-json-lift_2.11")).
  settings(libraryDependencies += "joda-time" % "joda-time" % "2.9.1")

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.typesafeRepo("releases"),
  "Local Maven Repository" at s"""file://${Path.userHome.absolutePath}/.ivy2/local"""
)
