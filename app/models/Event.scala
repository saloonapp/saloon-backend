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
  updated: DateTime)
object Event {
  implicit val format = Json.format[Event]
}

case class EventUI(
  uuid: String,
  name: String,
  description: String,
  logo: Option[String],
  start: Option[DateTime],
  end: Option[DateTime],
  address: Option[Address],
  twitterHashtag: Option[String],
  created: DateTime,
  updated: DateTime,
  sessionCount: Int,
  exponentCount: Int)
object EventUI {
  def toModel(d: EventUI): Event = Event(d.uuid, d.name, d.description, d.logo, d.start, d.end, d.address, d.twitterHashtag, d.created, d.updated)
  def fromModel(m: Event, sessionCount: Int, exponentCount: Int): EventUI = EventUI(m.uuid, m.name, m.description, m.logo, m.start, m.end, m.address, m.twitterHashtag, m.created, m.updated, sessionCount, exponentCount)
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

  def toModel(d: EventData): Event = Event(Repository.generateUuid(), d.name, d.description, d.logo, d.start, d.end, d.address, d.twitterHashtag.map(toHashtag), new DateTime(), new DateTime())
  def fromModel(m: Event): EventData = EventData(m.name, m.description, m.logo, m.start, m.end, m.address, m.twitterHashtag)
  def merge(m: Event, d: EventData): Event = m.copy(name = d.name, description = d.description, logo = d.logo, start = d.start, end = d.end, address = d.address, twitterHashtag = d.twitterHashtag.map(toHashtag), updated = new DateTime())

  private def toHashtag(str: String): String = if (str.startsWith("#")) str.substring(1) else str
}
