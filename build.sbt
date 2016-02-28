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
  scalacOptions in (Compile, console) ~= (_ filterNot (_ == "-Ywarn-unused-import"))
)

lazy val cheminotorgSettings = buildSettings ++ commonSettings

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(name := "web").
  settings(cheminotorgSettings:_*).
  settings(libraryDependencies += "com.propensive" %% "rapture" % "2.0.0-M5").
  settings(libraryDependencies += "com.propensive" %% "rapture-http-jetty" % "2.0.0-M5").
  settings(libraryDependencies += "joda-time" % "joda-time" % "2.9.1").
  settings(libraryDependencies += "org.joda" % "joda-convert" % "1.8").
  settings(libraryDependencies += "org.jsoup" % "jsoup" % "1.8.3").
  settings(com.github.retronym.SbtOneJar.oneJarSettings: _*)

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.typesafeRepo("releases")
)
