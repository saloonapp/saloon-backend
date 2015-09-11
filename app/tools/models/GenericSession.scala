package tools.models

import play.api.libs.json.Json
import org.joda.time.DateTime

case class GenericSession(
  source: Source,
  name: String,
  description: String,
  descriptionHTML: String,
  format: String,
  theme: String,
  place: String,
  start: Option[DateTime],
  end: Option[DateTime])
object GenericSession {
  implicit val format = Json.format[GenericSession]
}