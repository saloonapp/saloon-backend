package tools.scrapers.eventseye.models

import tools.utils.CsvElt
import tools.utils.CsvUtils
import play.api.libs.json.Json

case class EventsEyeAddress(
  name: String,
  complement: String,
  street: String,
  city: String,
  country: String)
object EventsEyeAddress {
  implicit val format = Json.format[EventsEyeAddress]
}
case class EventsEyeVenue(
  logo: String,
  name: String,
  address: EventsEyeAddress,
  phone: String,
  site: String,
  email: String)
object EventsEyeVenue {
  implicit val format = Json.format[EventsEyeVenue]
}
case class EventsEyeOrganizer(
  logo: String,
  name: String,
  address: EventsEyeAddress,
  phone: String,
  site: String,
  email: String)
object EventsEyeOrganizer {
  implicit val format = Json.format[EventsEyeOrganizer]
}
case class EventsEyeAttendance(
  year: String,
  exponents: String,
  visitors: String,
  exhibitionSpace: String)
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
  nextDate: String,
  otherDates: List[String],
  venue: EventsEyeVenue,
  orgas: List[EventsEyeOrganizer],
  attendance: Option[EventsEyeAttendance],
  website: String,
  email: String,
  phone: String,
  url: String) extends CsvElt {
  def toCsv(): Map[String, String] = EventsEyeEvent.toCsv(this)
}
object EventsEyeEvent {
  implicit val format = Json.format[EventsEyeEvent]
  def toCsv(e: EventsEyeEvent): Map[String, String] = CsvUtils.jsonToCsv(Json.toJson(e), 4)
}
