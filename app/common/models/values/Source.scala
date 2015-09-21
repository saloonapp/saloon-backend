package common.models.values

import play.api.data.Forms._
import play.api.libs.json.Json

case class Source(
  ref: String,
  name: String,
  url: String)
object Source {
  implicit val format = Json.format[Source]
  val fields = mapping(
    "ref" -> text,
    "name" -> text,
    "url" -> text)(Source.apply)(Source.unapply)
}
