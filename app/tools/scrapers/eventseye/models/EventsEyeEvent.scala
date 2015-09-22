package tools.scrapers.eventseye.models

import common.models.values.Source
import common.models.event.GenericEvent
import common.models.event.GenericEventVenue
import common.models.event.GenericEventOrganizer
import common.models.event.GenericEventAddress
import common.models.event.GenericEventStats
import scala.util.Try
import play.api.libs.json.Json
import org.joda.time.DateTime

case class EventsEyePeriod(
  start: Option[DateTime],
  end: Option[DateTime])
object EventsEyePeriod {
  implicit val format = Json.format[EventsEyePeriod]
}
case class EventsEyeAddress(
  name: String,
  complement: String,
  street: String,
  city: String,
  country: String) {
  def toGeneric: GenericEventAddress = GenericEventAddress(this.name, this.complement, this.street, "", this.city, this.country)
}
object EventsEyeAddress {
  implicit val format = Json.format[EventsEyeAddress]
}
case class EventsEyeVenue(
  logo: String,
  name: String,
  address: EventsEyeAddress,
  phone: String,
  website: String,
  email: String) {
  def toGeneric: GenericEventVenue = GenericEventVenue(this.logo, this.name, this.address.toGeneric, this.website, this.email, this.phone)
}
object EventsEyeVenue {
  implicit val format = Json.format[EventsEyeVenue]
}
case class EventsEyeOrganizer(
  logo: String,
  name: String,
  address: EventsEyeAddress,
  phone: String,
  website: String,
  email: String) {
  def toGeneric: GenericEventOrganizer = GenericEventOrganizer(this.logo, this.name, this.address.toGeneric, this.website, this.email, this.phone)
}
object EventsEyeOrganizer {
  implicit val format = Json.format[EventsEyeOrganizer]
}
case class EventsEyeAttendance(
  year: String,
  exponents: String,
  visitors: String,
  exhibitionSpace: String) {
  def toGeneric: GenericEventStats = GenericEventStats(
    Try(this.year.toInt).toOption,
    Try(this.exhibitionSpace.toInt).toOption,
    Try(this.exponents.toInt).toOption,
    Try(this.visitors.toInt).toOption,
    Try(this.visitors.toInt).toOption)
}
object EventsEyeAttendance {
  implicit val format = Json.format[EventsEyeAttendance]
}
case class EventsEyeEvent(
  logo: String,
  name: String,
  industries: List[String],
  decription: String,
  audience: String,
  cycle: String,
  start: Option[DateTime],
  end: Option[DateTime],
  otherDates: List[EventsEyePeriod],
  venue: EventsEyeVenue,
  orgas: List[EventsEyeOrganizer],
  attendance: Option[EventsEyeAttendance],
  website: String,
  email: String,
  phone: String,
  url: String)
object EventsEyeEvent {
  implicit val format = Json.format[EventsEyeEvent]

  val sourceName = "EventsEyeScraper"
  def toGenericEvent(event: EventsEyeEvent): GenericEvent = {
    GenericEvent(
      List(Source(getRef(event.url), sourceName, event.url)),
      event.logo,
      event.name,
      event.start,
      event.end,
      event.decription,
      event.decription,
      Some(event.venue.toGeneric),
      event.orgas.map(_.toGeneric),
      event.website,
      event.email,
      event.phone,
      event.industries, // tags
      Map(), // socialUrls
      event.attendance.map(_.toGeneric).getOrElse(GenericEventStats(None, None, None, None, None)),
      GenericEvent.Status.draft,
      List(), // attendees
      List(), // exponents
      List(), // sessions
      Map(), // exponentTeam
      Map()) // sessionSpeakers
  }
  private val refRegex = "http://www.eventseye.com/fairs/(.*?).html".r
  private def getRef(url: String): String = url match {
    case refRegex(ref) => ref
    case _ => url
  }
}
