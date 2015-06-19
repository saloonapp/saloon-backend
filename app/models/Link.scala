package models

import play.api.libs.json.Json

case class Link(
  label: String,
  url: String)
object Link {
  implicit val format = Json.format[Link]
}
