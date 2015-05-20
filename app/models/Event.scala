package models

import infrastructure.repository.common.Repository
import services.FileImporter
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json.Json

case class Event(
  uuid: String,
  image: String,
  name: String,
  description: String,
  start: Option[DateTime],
  end: Option[DateTime],
  address: Option[Address],
  twitterHashtag: Option[String],
  published: Boolean,
  created: DateTime,
  updated: DateTime) {
  def toMap(): Map[String, String] = Event.toMap(this)
}
object Event {
  implicit val format = Json.format[Event]
  def fromMap(d: Map[String, String]): Option[Event] =
    if (d.get("name").isDefined) {
      Some(Event(
        Repository.generateUuid(),
        d.get("image").getOrElse(""),
        d.get("name").get,
        d.get("description").getOrElse(""),
        d.get("start").map(d => DateTime.parse(d, FileImporter.dateFormat)),
        d.get("end").map(d => DateTime.parse(d, FileImporter.dateFormat)),
        d.get("address.name").map(name => Address(name)),
        d.get("twitterHashtag"),
        d.get("published").map(_.toBoolean).getOrElse(false),
        d.get("created").map(d => DateTime.parse(d, FileImporter.dateFormat)).getOrElse(new DateTime()),
        d.get("updated").map(d => DateTime.parse(d, FileImporter.dateFormat)).getOrElse(new DateTime())))
    } else {
      None
    }
  def toMap(e: Event): Map[String, String] = Map(
    "image" -> e.image,
    "name" -> e.name,
    "description" -> e.description,
    "start" -> e.start.map(_.toString(FileImporter.dateFormat)).getOrElse(""),
    "end" -> e.end.map(_.toString(FileImporter.dateFormat)).getOrElse(""),
    "address.name" -> e.address.map(_.name).getOrElse(""),
    "twitterHashtag" -> e.twitterHashtag.getOrElse(""),
    "published" -> e.published.toString,
    "created" -> e.created.toString(FileImporter.dateFormat),
    "updated" -> e.updated.toString(FileImporter.dateFormat))
}

case class EventUI(
  uuid: String,
  image: String,
  name: String,
  description: String,
  start: Option[DateTime],
  end: Option[DateTime],
  address: Option[Address],
  twitterHashtag: Option[String],
  published: Boolean,
  created: DateTime,
  updated: DateTime,
  sessionCount: Int,
  exponentCount: Int,
  className: String = "events")
object EventUI {
  implicit val format = Json.format[EventUI]
  def toModel(d: EventUI): Event = Event(d.uuid, d.image, d.name, d.description, d.start, d.end, d.address, d.twitterHashtag, d.published, d.created, d.updated)
  def fromModel(m: Event, sessionCount: Int, exponentCount: Int): EventUI = EventUI(m.uuid, m.image, m.name, m.description, m.start, m.end, m.address, m.twitterHashtag, m.published, m.created, m.updated, sessionCount, exponentCount)
}

// mapping object for Event Form
case class EventData(
  name: String,
  description: String,
  image: String,
  start: Option[DateTime],
  end: Option[DateTime],
  address: Option[Address],
  twitterHashtag: Option[String],
  published: Boolean)
object EventData {
  implicit val format = Json.format[EventData]
  val fields = mapping(
    "name" -> nonEmptyText,
    "description" -> text,
    "image" -> text,
    "start" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
    "end" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
    "address" -> optional(Address.fields),
    "twitterHashtag" -> optional(text),
    "published" -> boolean)(EventData.apply)(EventData.unapply)

  def toModel(d: EventData): Event = Event(Repository.generateUuid(), d.image, d.name, d.description, d.start, d.end, d.address, d.twitterHashtag.map(toHashtag), d.published, new DateTime(), new DateTime())
  def fromModel(m: Event): EventData = EventData(m.name, m.description, m.image, m.start, m.end, m.address, m.twitterHashtag, m.published)
  def merge(m: Event, d: EventData): Event = m.copy(name = d.name, description = d.description, image = d.image, start = d.start, end = d.end, address = d.address, twitterHashtag = d.twitterHashtag.map(toHashtag), published = d.published, updated = new DateTime())

  private def toHashtag(str: String): String = if (str.startsWith("#")) str.substring(1) else str
}
