package models.values

import play.api.data.Forms._
import play.api.libs.json.Json

case class Link(
  label: String,
  url: String)
object Link {
  implicit val format = Json.format[Link]
  val fields = mapping(
    "label" -> text,
    "url" -> text)(Link.apply)(Link.unapply)
}
