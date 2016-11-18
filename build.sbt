enablePlugins(GitVersioning, GitBranchPrompt)

lazy val buildSettings = Seq(
  organization := "org.cheminot",
  scalaVersion := "2.11.8",
  crossPaths := false
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
  settings(libraryDependencies += "com.propensive" %% "rapture" % "2.0.0-M7" exclude ("javax.servlet", "servlet-api")).
  settings(libraryDependencies += "com.propensive" %% "rapture-http-jetty" % "2.0.0-M7").
  settings(libraryDependencies += "joda-time" % "joda-time" % "2.9.1").
  settings(libraryDependencies += "org.joda" % "joda-convert" % "1.8").
  settings(libraryDependencies += "org.jsoup" % "jsoup" % "1.8.3").
  settings(libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.4").
  settings(libraryDependencies += "org.cheminot" % "misc" % "0.1-SNAPSHOT").
  settings(com.github.retronym.SbtOneJar.oneJarSettings: _*)

// versioning
git.baseVersion := "0.1.0"
git.useGitDescribe := true
git.formattedShaVersion := git.gitHeadCommit.value map { sha => s"$sha".take(7) }

resolvers ++= Seq(
  Resolver.typesafeRepo("releases"),
  Resolver.url("rapture", new URL("https://raw.githubusercontent.com/srenault/central/master"))(Resolver.ivyStylePatterns)
)

sourceGenerators in Compile <+= (version, sourceManaged in Compile).map {
  case (gitVersion, sources) =>
    val file = sources / "Settings.scala"
    val code = """package org.cheminot.web { trait Settings { val GIT_TAG = "%s" } }""".format(gitVersion)
    scala.util.Try(IO read file).map { current =>
      Some(current)
    }.recover {
      case _: java.io.FileNotFoundException =>
        None
    }.foreach { maybeCurrent =>
      if(maybeCurrent.isEmpty || maybeCurrent.exists(_ != code)) {
        IO.write(file, code)
      }
    }
    Seq(file)
}
