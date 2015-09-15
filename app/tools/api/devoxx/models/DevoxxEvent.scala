package tools.api.devoxx.models

import play.api.libs.json.Json

case class DevoxxEvent(
  eventCode: String,
  label: String,
  sourceUrl: Option[String])
object DevoxxEvent {
  implicit val format = Json.format[DevoxxEvent]
}
