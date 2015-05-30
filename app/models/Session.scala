package models

import common.Utils
import common.infrastructure.repository.Repository
import services.FileImporter
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json.Json

case class Session(
  uuid: String,
  eventId: String,
  name: String,
  description: String,
  format: String,
  category: String,
  place: String, // where to find this exponent
  start: Option[DateTime],
  end: Option[DateTime],
  speakers: List[Person],
  tags: List[String],
  source: Option[DataSource], // where the session were fetched (if applies)
  created: DateTime,
  updated: DateTime) extends EventItem {
  def toMap(): Map[String, String] = Session.toMap(this)
}
object Session {
  implicit val format = Json.format[Session]
  private def parseDate(date: String) = Utils.parseDate(FileImporter.dateFormat)(date)
  def fromMap(eventId: String)(d: Map[String, String]): Option[Session] =
    if (d.get("name").isDefined) {
      Some(Session(
        d.get("uuid").getOrElse(Repository.generateUuid()),
        eventId,
        d.get("name").get,
        d.get("description").getOrElse(""),
        d.get("format").getOrElse(""),
        d.get("category").getOrElse(""),
        d.get("place").getOrElse(""),
        d.get("start").flatMap(d => parseDate(d)),
        d.get("end").flatMap(d => parseDate(d)),
        d.get("speakers").flatMap(json => Json.parse(json.replace("\r", "\\r").replace("\n", "\\n")).asOpt[List[Person]]).getOrElse(List()),
        Utils.toList(d.get("tags").getOrElse("")),
        d.get("source.url").map(url => DataSource(d.get("source.ref").getOrElse(""), url)),
        d.get("created").flatMap(d => parseDate(d)).getOrElse(new DateTime()),
        d.get("updated").flatMap(d => parseDate(d)).getOrElse(new DateTime())))
    } else {
      None
    }
  def toMap(e: Session): Map[String, String] = Map(
    "uuid" -> e.uuid,
    "eventId" -> e.eventId,
    "name" -> e.name,
    "description" -> e.description,
    "format" -> e.format,
    "category" -> e.category,
    "place" -> e.place,
    "start" -> e.start.map(_.toString(FileImporter.dateFormat)).getOrElse(""),
    "end" -> e.end.map(_.toString(FileImporter.dateFormat)).getOrElse(""),
    "speakers" -> Json.stringify(Json.toJson(e.speakers)),
    "tags" -> Utils.fromList(e.tags),
    "source.ref" -> e.source.map(_.ref).getOrElse(""),
    "source.url" -> e.source.map(_.url).getOrElse(""),
    "created" -> e.created.toString(FileImporter.dateFormat),
    "updated" -> e.updated.toString(FileImporter.dateFormat))
}

case class SessionUI(
  uuid: String,
  eventId: String,
  name: String,
  description: String,
  format: String,
  category: String,
  place: String,
  start: Option[DateTime],
  end: Option[DateTime],
  speakers: List[Person],
  tags: List[String],
  created: DateTime,
  updated: DateTime,
  className: String = SessionUI.className)
object SessionUI {
  val className = "sessions"
  implicit val format = Json.format[SessionUI]
  // def toModel(d: SessionUI): Session = Session(d.uuid, d.eventId, d.name, d.description, d.format, d.category, d.place, d.start, d.end, d.speakers, d.tags, d.created, d.updated)
  def fromModel(d: Session): SessionUI = SessionUI(d.uuid, d.eventId, d.name, d.description, d.format, d.category, d.place, d.start, d.end, d.speakers, d.tags, d.created, d.updated)
}

// mapping object for Session Form
case class SessionData(
  eventId: String,
  name: String,
  description: String,
  format: String,
  category: String,
  place: String,
  start: Option[DateTime],
  end: Option[DateTime],
  speakers: List[Person],
  tags: String)
object SessionData {
  implicit val format = Json.format[SessionData]
  val fields = mapping(
    "eventId" -> nonEmptyText,
    "name" -> nonEmptyText,
    "description" -> text,
    "format" -> text,
    "category" -> text,
    "place" -> text,
    "start" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
    "end" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
    "speakers" -> list(Person.fields),
    "tags" -> text)(SessionData.apply)(SessionData.unapply)

  def toModel(d: SessionData): Session = Session(Repository.generateUuid(), d.eventId, d.name, d.description, d.format, d.category, d.place, d.start, d.end, d.speakers.filter(!_.name.isEmpty), Utils.toList(d.tags), None, new DateTime(), new DateTime())
  def fromModel(d: Session): SessionData = SessionData(d.eventId, d.name, d.description, d.format, d.category, d.place, d.start, d.end, d.speakers, Utils.fromList(d.tags))
  def merge(m: Session, d: SessionData): Session = toModel(d).copy(uuid = m.uuid, source = m.source, created = m.created)
}
