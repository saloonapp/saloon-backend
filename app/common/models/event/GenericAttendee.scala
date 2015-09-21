package common.models.event

import common.models.values.Source
import play.api.libs.json.Json

case class GenericAttendee(
  source: Source,
  firstName: String,
  lastName: String,
  avatar: String,
  description: String,
  descriptionHTML: String,
  role: String,
  siteUrl: Option[String],
  twitterUrl: Option[String],
  company: String)
object GenericAttendee {
  implicit val format = Json.format[GenericAttendee]
}