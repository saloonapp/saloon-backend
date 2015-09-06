name := """backend"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

play.PlayImport.PlayKeys.routesImport ++= Seq("common.models.user._", "common.models.event._", "common.models.values.GenericId")

includeFilter in (Assets, LessKeys.less) := "website.sample.less" | "website.local.less" | "website.dev.less" | "website.prod.less" | "backend.local.less" | "backend.dev.less" | "backend.prod.less" | "admin.local.less" | "admin.dev.less" | "admin.prod.less"

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23",
  "com.mohiva" %% "play-silhouette" % "1.0",
  "com.github.tototoshi" %% "scala-csv" % "1.2.1",
  "org.jsoup" % "jsoup" % "1.8.2"
)
