package models

import infrastructure.repository.common.Repository
import services.FileImporter
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json.Json

case class Session(
  uuid: String,
  eventId: String,
  image: String,
  name: String,
  description: String,
  format: String,
  category: String,
  place: Place, // room, booth...
  start: Option[DateTime],
  end: Option[DateTime],
  tags: List[String],
  created: DateTime,
  updated: DateTime) {
  def toMap(): Map[String, String] = Session.toMap(this)
}
object Session {
  implicit val format = Json.format[Session]
  def fromMap(eventId: String)(d: Map[String, String]): Option[Session] =
    if (d.get("name").isDefined) {
      Some(Session(
        Repository.generateUuid(),
        eventId,
        d.get("image").getOrElse(""),
        d.get("name").get,
        d.get("description").getOrElse(""),
        d.get("format").getOrElse(""),
        d.get("category").getOrElse(""),
        Place(
          d.get("place.ref").getOrElse(""),
          d.get("place.name").getOrElse("")),
        d.get("start").map(d => DateTime.parse(d, FileImporter.dateFormat)),
        d.get("end").map(d => DateTime.parse(d, FileImporter.dateFormat)),
        ExponentData.toArray(d.get("tags").getOrElse("")),
        d.get("created").map(d => DateTime.parse(d, FileImporter.dateFormat)).getOrElse(new DateTime()),
        d.get("updated").map(d => DateTime.parse(d, FileImporter.dateFormat)).getOrElse(new DateTime())))
    } else {
      None
    }
  def toMap(e: Session): Map[String, String] = Map(
    "uuid" -> e.uuid,
    "eventId" -> e.eventId,
    "image" -> e.image,
    "name" -> e.name,
    "description" -> e.description,
    "format" -> e.format,
    "category" -> e.category,
    "place.ref" -> e.place.ref,
    "place.name" -> e.place.name,
    "start" -> e.start.map(_.toString(FileImporter.dateFormat)).getOrElse(""),
    "end" -> e.end.map(_.toString(FileImporter.dateFormat)).getOrElse(""),
    "tags" -> e.tags.mkString(", "),
    "created" -> e.created.toString(FileImporter.dateFormat),
    "updated" -> e.updated.toString(FileImporter.dateFormat))
}

case class SessionUI(
  uuid: String,
  eventId: String,
  image: String,
  name: String,
  description: String,
  format: String,
  category: String,
  place: Place,
  start: Option[DateTime],
  end: Option[DateTime],
  tags: List[String],
  created: DateTime,
  updated: DateTime,
  className: String = "sessions")
object SessionUI {
  implicit val format = Json.format[SessionUI]
  def toModel(d: SessionUI): Session = Session(d.uuid, d.eventId, d.image, d.name, d.description, d.format, d.category, d.place, d.start, d.end, d.tags, d.created, d.updated)
  def fromModel(d: Session): SessionUI = SessionUI(d.uuid, d.eventId, d.image, d.name, d.description, d.format, d.category, d.place, d.start, d.end, d.tags, d.created, d.updated)
}

// mapping object for Session Form
case class SessionData(
  eventId: String,
  image: String,
  name: String,
  description: String,
  format: String,
  category: String,
  place: Place,
  start: Option[DateTime],
  end: Option[DateTime],
  tags: String)
object SessionData {
  implicit val format = Json.format[SessionData]
  val fields = mapping(
    "eventId" -> nonEmptyText,
    "image" -> text,
    "name" -> nonEmptyText,
    "description" -> text,
    "format" -> text,
    "category" -> text,
    "place" -> Place.fields,
    "start" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
    "end" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
    "tags" -> text)(SessionData.apply)(SessionData.unapply)

  def toModel(d: SessionData): Session = Session(Repository.generateUuid(), d.eventId, d.image, d.name, d.description, d.format, d.category, d.place, d.start, d.end, toTags(d.tags), new DateTime(), new DateTime())
  def fromModel(m: Session): SessionData = SessionData(m.eventId, m.image, m.name, m.description, m.format, m.category, m.place, m.start, m.end, m.tags.mkString(", "))
  def merge(m: Session, d: SessionData): Session = m.copy(eventId = d.eventId, image = d.image, name = d.name, description = d.description, format = d.format, category = d.category, place = d.place, start = d.start, end = d.end, tags = toTags(d.tags), updated = new DateTime())

  private def toTags(str: String): List[String] = str.split(",").toList.map(_.trim())
}
