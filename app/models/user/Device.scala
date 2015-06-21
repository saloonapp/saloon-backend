package models.user

import play.api.data.Forms._
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
  val fields = mapping(
    "uuid" -> nonEmptyText,
    "platform" -> text,
    "manufacturer" -> text,
    "model" -> text,
    "version" -> text,
    "cordova" -> text)(Device.apply)(Device.unapply)
}
