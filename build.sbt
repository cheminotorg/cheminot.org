name := "cheminotorg"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

doc in Compile <<= target.map(_ / "none")

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "anorm" % "2.4.0-M2",
  "io.prismic" %% "scala-kit" % "1.3.3",
  "commons-io" % "commons-io" % "2.4"
)
