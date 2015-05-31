package tools.scrapers.lanyrd.models

import org.joda.time.DateTime
import play.api.libs.json.Json

case class LanyrdEventDetails(
  baseLine: String,
  description: String,
  venue: Option[LanyrdVenue],
  website: Option[LanyrdLink],
  schedule: Option[LanyrdLink],
  twitterAccount: Option[LanyrdLink],
  twitterHashtag: Option[LanyrdLink])
object LanyrdEventDetails {
  implicit val format = Json.format[LanyrdEventDetails]
}

case class LanyrdEvent(
  name: String,
  url: String,
  places: List[LanyrdLink],
  start: Option[DateTime],
  end: Option[DateTime],
  tags: List[LanyrdLink],
  details: Option[LanyrdEventDetails])
object LanyrdEvent {
  implicit val format = Json.format[LanyrdEvent]
}
