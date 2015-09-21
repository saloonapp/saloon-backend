package common.models.event

import common.models.values.Source
import play.api.libs.json.Json
import org.joda.time.DateTime

case class GenericEvent(
  source: Source,
  name: String,
  start: Option[DateTime],
  end: Option[DateTime],

  attendees: List[GenericAttendee],
  exponents: List[GenericExponent],
  sessions: List[GenericSession],
  exponentTeam: Map[String, List[String]],
  sessionSpeakers: Map[String, List[String]])
object GenericEvent {
  implicit val format = Json.format[GenericEvent]
}
