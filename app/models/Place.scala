package models

import play.api.data.Forms._
import play.api.libs.json.Json

case class Place(
  ref: String,
  name: String)
object Place {
  implicit val format = Json.format[Place]
  val fields = mapping(
    "ref" -> text,
    "name" -> text)(Place.apply)(Place.unapply)
}
