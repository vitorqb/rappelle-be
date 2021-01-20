name := """rappelle"""
organization := "github.com.vitorqb"
version := "1.0-SNAPSHOT"
scalaVersion := "2.13.3"

//Versioning has a special handling to we are able to inject it during build time.
//We read versioning from `AppVersioning.scala`, which in turns read it from
//Play's configuration `application.conf`. When building the application, you
//(or the CI system) can just find the right version using git and echo append
//it to the very end of `application.conf`, and it will endup here and be readable
//by the code.
version := AppVersioning.appVersion

//A configuration key for functional tests, with helper functions to identify the test type
lazy val FunTest = config("fun") extend(Test)
lazy val AllTests = config("alltests") extend(Test)
def funTestFilter(name: String): Boolean = name endsWith "FunSpec"
def unitTestFilter(name: String): Boolean = (name endsWith "Spec") && !funTestFilter(name)

//The project definition
lazy val root = (project in file("."))
  .configs(FunTest)
  .configs(AllTests)
  .settings(
    //Settings for unit test
    Test / testOptions := Seq(Tests.Filter(unitTestFilter)),
    Test / javaOptions += "-Dconfig.resource=application.local.conf",

    //Settings for functional tests
    inConfig(FunTest)(Defaults.testTasks),
    FunTest / testOptions := Seq(Tests.Filter(funTestFilter)),
    FunTest / javaOptions += "-Dconfig.resource=application.local.conf",

    //Settings for all tests
    inConfig(AllTests)(Defaults.testTasks),
    AllTests / testOptions := Seq(),
    AllTests / javaOptions += "-Dconfig.resource=application.local.conf",
  )
  .enablePlugins(PlayScala)

//Dependencies and test dependencies
libraryDependencies += guice
libraryDependencies += ws
libraryDependencies ++= Seq(evolutions, jdbc, "org.playframework.anorm" %% "anorm" % "2.6.4")
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.18"
libraryDependencies += "joda-time" % "joda-time" % "2.10.8"
libraryDependencies += "com.typesafe.play" %% "play-json-joda" % "2.9.1"
libraryDependencies += "com.google.crypto.tink" % "tink" % "1.5.0"

libraryDependencies += jdbc % Test
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test
libraryDependencies += "org.mockito" %% "mockito-scala" % "1.16.3" % Test
libraryDependencies += "org.mockito" %% "mockito-scala-scalatest" % "1.16.3" % Test

//Custom scala compiler
scalacOptions += "-Ywarn-unused:imports"
scalacOptions += "-language:postfixOps"
