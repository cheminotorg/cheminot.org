name := "cheminotorg"

version := {
  val GitShortVersion = """^.*([0-9abcdef]{7})$""".r
  Option("git describe --long --always" !!).map(_.trim.toLowerCase).map {
    case GitShortVersion(gitVersion) => gitVersion
    case other                       => other
  }.filterNot(_.isEmpty).getOrElse("x-SNAPSHOT")
}

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

doc in Compile <<= target.map(_ / "none")

libraryDependencies ++= Seq(
  ws,
  "io.prismic" %% "scala-kit" % "1.3.3",
  "commons-io" % "commons-io" % "2.4"
)

sourceGenerators in Compile <+= (version, sourceManaged in Compile).map {
  case (gitVersion, sources) =>
    val file = sources / "Settings.scala"
    val code = """package cheminotorg { trait Settings { val GIT_TAG = "%s" } }""".format(gitVersion)
    val current = try {
      IO read file
    } catch {
      case _: java.io.FileNotFoundException => ""
    }
    if (current != code) IO.write(file, code)
    Seq(file)
}
