scalaVersion := "2.13.8"
organization := "ru.neoflex"
enablePlugins(AkkaGrpcPlugin)

val http4sVersion = "0.23.18"
val http4sBlaze = "0.23.14"

libraryDependencies ++= Seq(
  "dev.zio" %% "zio" % "2.0.15",
  "dev.zio" %% "zio-config" % "3.0.7",
  "dev.zio" %% "zio-config-typesafe" % "3.0.7",
  "dev.zio" %% "zio-config-magnolia" % "3.0.7",
  "dev.zio" %% "zio-config-refined" % "3.0.7",
  "dev.zio" %% "zio-interop-cats" % "23.0.03",
  "dev.zio" %% "zio-json" % "0.5.0",
  "dev.zio" %% "zio-http" % "3.0.0-RC2",
)