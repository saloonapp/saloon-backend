package models

import org.joda.time.DateTime
import play.api.libs.json.Json

case class CrashError(
  `type`: String,
  name: Option[String],
  message: String,
  stack: Option[String],
  fileName: Option[String],
  lineNumber: Option[Int],
  columnNumber: Option[Int])
object CrashError {
  implicit val format = Json.format[CrashError]
}

case class CrashDevice(
  uuid: String,
  platform: String,
  version: String,
  manufacturer: String,
  model: String,
  cordova: String,
  available: Boolean)
object CrashDevice {
  implicit val format = Json.format[CrashDevice]
}

case class CrashApplication(
  appVersion: String)
object CrashApplication {
  implicit val format = Json.format[CrashApplication]
}

case class CrashNavigator(
  platform: String,
  vendor: String,
  appName: String,
  appCodeName: String,
  product: String,
  appVersion: String,
  userAgent: String)
object CrashNavigator {
  implicit val format = Json.format[CrashNavigator]
}

case class Crash(
  uuid: String,
  clientId: String,
  previousClientId: Option[String],
  userId: Option[String],
  error: CrashError,
  device: Option[CrashDevice],
  application: Option[CrashApplication],
  navigator: Option[CrashNavigator],
  url: String,
  solved: Option[Boolean],
  time: DateTime,
  created: DateTime)
object Crash {
  implicit val format = Json.format[Crash]
}
    