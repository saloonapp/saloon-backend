package models

import play.api.libs.json.Json

case class Device(
  uuid: String,
  platform: String,
  manufacturer: String,
  model: String,
  version: String,
  cordova: String)
object Device {
  implicit val format = Json.format[Device]
}
