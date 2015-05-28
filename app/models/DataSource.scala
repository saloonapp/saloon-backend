package models

import play.api.libs.json.Json

case class DataSource(
  ref: String,
  url: String)
object DataSource {
  implicit val format = Json.format[DataSource]
}