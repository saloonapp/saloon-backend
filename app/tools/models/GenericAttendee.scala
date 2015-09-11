package tools.models

import play.api.libs.json.Json

case class GenericAttendee(
  source: Source,
  firstName: String,
  lastName: String,
  avatar: String,
  description: String,
  descriptionHTML: String,
  blogUrl: Option[String],
  twitterUrl: Option[String],
  company: String)
object GenericAttendee {
  implicit val format = Json.format[GenericAttendee]
}