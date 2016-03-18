package tools.scrapers.mixit.models

import play.api.libs.json.Json

case class KeyValue(
                     key: String,
                     value: Option[String]
                   )
object KeyValue {
  implicit val format = Json.format[KeyValue]
}
