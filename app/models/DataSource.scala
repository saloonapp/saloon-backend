package models

import play.api.data.Forms._
import play.api.libs.json.Json

case class DataSource(
  ref: String,
  name: Option[String],
  url: String)
object DataSource {
  implicit val format = Json.format[DataSource]
  val fields = mapping(
    "ref" -> text,
    "name" -> optional(text),
    "url" -> text)(DataSource.apply)(DataSource.unapply)
}
