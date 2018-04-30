val Http4sVersion = "0.18.4"
val Specs2Version = "4.0.3"
val LogbackVersion = "1.2.3"

//ensimeIgnoreScalaMismatch in ThisBuild := true
ensimeScalaVersion in ThisBuild := "2.11.8"

lazy val root = (project in file("."))
  .settings(
    organization := "com.example",
    name := "proxy",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.11.8",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"      %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "org.specs2"     %% "specs2-core"          % Specs2Version % "test",
      "ch.qos.logback"  % "logback-classic"     % LogbackVersion,
      "org.scalaz.stream" %% "scalaz-stream" % "0.8.6",
      "com.typesafe" % "config" % "1.3.2",
      "com.github.pureconfig" %% "pureconfig" % "0.9.1",
      "com.github.andr83" %% "scalaconfig" % "0.4"
    )
  )

