package common.models.event

import common.models.values.Source
import play.api.libs.json.Json
import org.joda.time.DateTime

case class GenericEventAddress(
  name: String,
  complement: String,
  street: String,
  zipCode: String,
  city: String,
  country: String)
object GenericEventAddress {
  implicit val format = Json.format[GenericEventAddress]
}
case class GenericEventVenue(
  logo: String,
  name: String,
  address: GenericEventAddress,
  website: Option[String],
  email: Option[String],
  phone: Option[String])
object GenericEventVenue {
  implicit val format = Json.format[GenericEventVenue]
}
case class GenericEventOrganizer(
  logo: String,
  name: String,
  address: GenericEventAddress,
  website: Option[String],
  email: Option[String],
  phone: Option[String])
object GenericEventOrganizer {
  implicit val format = Json.format[GenericEventOrganizer]
}
case class GenericEventInfo(
  logo: String,
  start: Option[DateTime],
  end: Option[DateTime],
  description: String,
  descriptionHTML: String,
  venue: Option[GenericEventVenue],
  organizers: List[GenericEventOrganizer],
  website: Option[String],
  email: Option[String],
  phone: Option[String])
object GenericEventInfo {
  implicit val format = Json.format[GenericEventInfo]
}
case class GenericEventStats(
  year: Option[Int],
  area: Option[Int],
  exponents: Option[Int],
  registration: Option[Int],
  visitors: Option[Int])
object GenericEventStats {
  implicit val format = Json.format[GenericEventStats]
}
case class GenericEvent(
  sources: List[Source],
  uuid: String,
  name: String,
  info: GenericEventInfo,
  tags: List[String],
  socialUrls: Map[String, String],
  stats: GenericEventStats,
  status: String, // draft, publishing, published

  attendees: List[GenericAttendee],
  exponents: List[GenericExponent],
  sessions: List[GenericSession],
  exponentTeam: Map[String, List[String]],
  sessionSpeakers: Map[String, List[String]])
object GenericEvent {
  implicit val format = Json.format[GenericEvent]
  object Social {
    val twitterAccount = "twitterAccount"
    val twitterHashtag = "twitterHashtag"
  }
  object Status {
    val draft = "draft"
    val publishing = "publishing"
    val published = "published"
  }
}
