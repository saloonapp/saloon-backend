package models.user

import play.api.data.Forms._
import play.api.libs.json.Json

case class Push(
  uuid: String,
  platform: String)
object Push {
  implicit val format = Json.format[Push]
  val fields = mapping(
    "uuid" -> text,
    "platform" -> text)(Push.apply)(Push.unapply)
}
