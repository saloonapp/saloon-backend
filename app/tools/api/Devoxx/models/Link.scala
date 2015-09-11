package tools.api.Devoxx.models

import play.api.libs.json.Json

case class Link(
  href: String,
  rel: String,
  title: String)
object Link {
  implicit val format = Json.format[Link]
}
