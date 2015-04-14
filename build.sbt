name := "cheminotorg"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "org.xerial" % "sqlite-jdbc" % "3.8.6",
  "com.typesafe.play" %% "anorm" % "2.4.0-M2",
  "io.prismic" %% "scala-kit" % "1.3.3"
)
