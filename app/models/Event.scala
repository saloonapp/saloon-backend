package models

import infrastructure.repository.common.Repository
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json.Json

case class Event(
  uuid: String,
  name: String,
  description: String,
  logo: Option[String],
  start: Option[DateTime],
  end: Option[DateTime],
  address: Option[Address],
  twitterHashtag: Option[String],
  created: DateTime,
  updated: DateTime) {
  def withData(d: EventData) = this.copy(name = d.name, description = d.description, logo = d.logo, start = d.start, end = d.end, address = d.address, twitterHashtag = d.twitterHashtag, updated = new DateTime())
}
object Event {
  implicit val format = Json.format[Event]
}

// mapping object for Event Form
case class EventData(
  name: String,
  description: String,
  logo: Option[String],
  start: Option[DateTime],
  end: Option[DateTime],
  address: Option[Address],
  twitterHashtag: Option[String])
object EventData {
  implicit val format = Json.format[EventData]
  val fields = mapping(
    "name" -> nonEmptyText,
    "description" -> text,
    "logo" -> optional(text),
    "start" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
    "end" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
    "address" -> optional(Address.fields),
    "twitterHashtag" -> optional(text))(EventData.apply)(EventData.unapply)

  def toModel(d: EventData): Event = Event(Repository.generateUuid(), d.name, d.description, d.logo, d.start, d.end, d.address, d.twitterHashtag, new DateTime(), new DateTime())
  def fromModel(m: Event): EventData = EventData(m.name, m.description, m.logo, m.start, m.end, m.address, m.twitterHashtag)
}