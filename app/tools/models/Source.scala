package tools.models

import play.api.libs.json.Json

case class Source(
  ref: String,
  name: String,
  url: String)
object Source {
  implicit val format = Json.format[Source]
}
