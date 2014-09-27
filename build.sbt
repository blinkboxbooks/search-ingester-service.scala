val buildSettings = Seq(
  organization := "com.blinkbox.books.agora",
  name := "search-ingester-service",
  version := scala.util.Try(scala.io.Source.fromFile("VERSION").mkString.trim).getOrElse("0.0.0"),
  scalaVersion  := "2.10.4",
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8", "-target:jvm-1.7",
    "-Xfatal-warnings", "-Xlint", "-Yno-adapted-args", "-Xfuture")
)

val dependencySettings = Seq(
  libraryDependencies ++= {
    val akkaV = "2.3.3"
    val sprayV = "1.3.1"
    Seq(
    "com.blinkbox.books"        %%         "common-config"          % "1.4.1",
    "com.blinkbox.books"        %%         "common-messaging"       % "1.1.3",
    "com.blinkbox.books"        %%         "common-scala-test"      % "0.3.0"   % Test,
    "com.blinkbox.books"        %%         "common-spray"           % "0.14.1",
    "com.blinkbox.books.hermes" %%         "rabbitmq-ha"            % "6.0.5",
    "io.spray"                   %         "spray-client"           % sprayV,
    "io.spray"                  %%         "spray-json"             % "1.2.6",
    "org.json4s"                %%         "json4s-jackson"         % "3.2.10",
    "com.typesafe.akka"         %%         "akka-testkit"           % akkaV     % Test
    )
  }
)

parallelExecution in Test := false

rpmPrepSettings

val root = (project in file(".")).
  settings(rpmPrepSettings: _*).
  settings(buildSettings: _*).
  settings(dependencySettings: _*)
