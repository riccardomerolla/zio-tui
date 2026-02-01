ThisBuild / scalaVersion := "3.5.2"
ThisBuild / organization := "io.github.riccardomerolla"
ThisBuild / organizationName := "Riccardo Merolla"
ThisBuild / organizationHomepage := Some(url("https://github.com/riccardomerolla"))

addCommandAlias("fmt", " ; scalafixAll ; scalafmtAll")
addCommandAlias("check", "; scalafixAll --check; scalafmtCheckAll")

inThisBuild(List(
  organization := "io.github.riccardomerolla",
  homepage := Some(url("https://github.com/riccardomerolla/zio-tui")),
  licenses := Seq(
    "MIT" -> url("https://opensource.org/license/mit")
  ),
  developers := List(
    Developer(
      id = "riccardomerolla",
      name = "Riccardo Merolla",
      email = "riccardo.merolla@gmail.com",
      url = url("https://github.com/riccardomerolla")
    )
  ),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/riccardomerolla/zio-tui"),
      "scm:git@github.com:riccardomerolla/zio-tui.git"
    )
  ),
  versionScheme := Some("early-semver"),
  scalacOptions ++= Seq(
    "-language:existentials",
    "-explain",
    "-Wunused:all",
  ),
  semanticdbEnabled                := true,
))

lazy val root = (project in file("."))
  .settings(
    name := "zio-tui",
    description := "A ZIO 2.x wrapper for layoutz, bringing effect-typed architecture to terminal UI development",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.1.24",
      "dev.zio" %% "zio-streams" % "2.1.24",
      "xyz.matthieucourt" %% "layoutz" % "0.6.0",
      "org.jline" % "jline" % "3.25.1",
      "dev.zio" %% "zio-test" % "2.1.24" % Test,
      "dev.zio" %% "zio-test-sbt" % "2.1.24" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    coverageExcludedPackages := ".*\\.example\\..*",
  )
