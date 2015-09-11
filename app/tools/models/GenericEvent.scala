package tools.models

import play.api.libs.json.Json
import org.joda.time.DateTime

case class GenericEvent(
  source: Source,
  name: String,
  start: Option[DateTime],
  end: Option[DateTime])
object GenericEvent {
  implicit val format = Json.format[GenericEvent]
}
