package models.values

import play.api.data.Forms._
import play.api.libs.json.Json

case class DataSource(
  ref: String,
  name: String,
  url: String)
object DataSource {
  implicit val format = Json.format[DataSource]
  val fields = mapping(
    "ref" -> text,
    "name" -> text,
    "url" -> text)(DataSource.apply)(DataSource.unapply)
}
