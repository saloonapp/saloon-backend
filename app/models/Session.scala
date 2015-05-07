package models

import infrastructure.repository.common.Repository
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json.Json

case class Session(
  uuid: String,
  eventId: String,
  title: String,
  summary: String,
  format: String,
  category: String,
  place: Place, // room, booth...
  start: Option[DateTime],
  end: Option[DateTime],
  speakerIds: List[String],
  tags: List[String],
  created: DateTime,
  updated: DateTime)
object Session {
  implicit val format = Json.format[Session]
}

// mapping object for Session Form
case class SessionData(
  eventId: String,
  title: String,
  summary: String,
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
    "title" -> nonEmptyText,
    "summary" -> text,
    "format" -> text,
    "category" -> text,
    "place" -> Place.fields,
    "start" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
    "end" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
    "tags" -> text)(SessionData.apply)(SessionData.unapply)

  def toModel(d: SessionData): Session = Session(Repository.generateUuid(), d.eventId, d.title, d.summary, d.format, d.category, d.place, d.start, d.end, List(), toTags(d.tags), new DateTime(), new DateTime())
  def fromModel(m: Session): SessionData = SessionData(m.eventId, m.title, m.summary, m.format, m.category, m.place, m.start, m.end, m.tags.mkString(", "))
  def merge(m: Session, d: SessionData): Session = m.copy(eventId = d.eventId, title = d.title, summary = d.summary, format = d.format, category = d.category, place = d.place, start = d.start, end = d.end, tags = toTags(d.tags), updated = new DateTime())

  private def toTags(str: String): List[String] = str.split(",").toList.map(_.trim())
}
