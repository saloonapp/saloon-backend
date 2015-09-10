package tools.scrapers.eventseye.models

import tools.utils.CsvElt
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
case class EventsEyeOrganizer(name: String,
  address: EventsEyeAddress,
  phone: String,
  site: String,
  email: String)
object EventsEyeOrganizer {
  implicit val format = Json.format[EventsEyeOrganizer]
}
case class EventsEyeEvent(
  logo: String,
  name: String,
  decription: String,
  audience: String,
  cycle: String,
  nextDate: String,
  otherDates: List[String],
  venue: String,
  orgas: List[EventsEyeOrganizer],
  website: String,
  email: String,
  url: String) extends CsvElt {
  def toCsv(): Map[String, String] = Map(
    "logo" -> this.logo,
    "name" -> this.name,
    "decription" -> this.decription,
    "audience" -> this.audience,
    "cycle" -> this.cycle,
    "nextDate" -> this.nextDate,
    "otherDates" -> this.otherDates.mkString(", "),
    "venue" -> this.venue,
    "orgas" -> Json.stringify(Json.toJson(this.orgas)),
    "website" -> this.website,
    "email" -> this.email,
    "url" -> this.url)
}
object EventsEyeEvent {
  implicit val format = Json.format[EventsEyeEvent]
}