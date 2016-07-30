name := """backend"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

play.PlayImport.PlayKeys.routesImport ++= Seq("common.models.user._", "common.models.event._", "conferences.models.ConferenceId", "conferences.models.PresentationId", "common.models.values.typed.ItemType", "common.models.values.typed.GenericId")

includeFilter in (Assets, LessKeys.less) :=
  "website.sample.less" | "website.local.less" | "website.dev.less" | "website.prod.less" |
    "backend.local.less" | "backend.dev.less" | "backend.prod.less" |
    "admin.local.less" | "admin.dev.less" | "admin.prod.less" |
    "conferences.local.less" | "conferences.dev.less" | "conferences.prod.less"

scalaVersion := "2.11.5"

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.7.play23",
  "com.mohiva" %% "play-silhouette" % "1.0",
  "com.github.tototoshi" %% "scala-csv" % "1.2.1",
  "org.jsoup" % "jsoup" % "1.8.2",
  "com.danielasfregola" %% "twitter4s" % "0.2"
)
