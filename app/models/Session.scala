package models

import common.Utils
import common.infrastructure.repository.Repository
import services.FileImporter
import org.joda.time.DateTime
import scala.util.Try
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
  slides: Option[String],
  video: Option[String],
  source: Option[DataSource], // where the session were fetched (if applies)
  created: DateTime,
  updated: DateTime) extends EventItem {
  def toMap(): Map[String, String] = Session.toMap(this)
  def merge(s: Session): Session = Session(
    this.uuid,
    this.eventId,
    merge(this.name, s.name),
    merge(this.description, s.description),
    merge(this.format, s.format),
    merge(this.category, s.category),
    merge(this.place, s.place),
    merge(this.start, s.start),
    merge(this.end, s.end),
    merge(this.speakers, s.speakers),
    merge(this.tags, s.tags),
    merge(this.slides, s.slides),
    merge(this.video, s.video),
    merge(this.source, s.source),
    this.created,
    s.updated)
  private def merge(s1: String, s2: String): String = if (s2.isEmpty) s1 else s2
  private def merge(b1: Boolean, b2: Boolean): Boolean = b1 || b2
  private def merge[A](d1: Option[A], d2: Option[A]): Option[A] = if (d2.isEmpty) d1 else d2
  private def merge[A](l1: List[A], l2: List[A]): List[A] = if (l2.isEmpty) l1 else l2
}
object Session {
  val className = "sessions"
  implicit val format = Json.format[Session]
  private def parseDate(date: String) = Utils.parseDate(FileImporter.dateFormat)(date)
  def fromMap(eventId: String)(d: Map[String, String]): Try[Session] =
    Try(Session(
      d.get("uuid").flatMap(u => if (u.isEmpty) None else Some(u)).getOrElse(Repository.generateUuid()),
      eventId,
      d.get("name").get,
      d.get("description").getOrElse(""),
      d.get("format").getOrElse(""),
      d.get("category").getOrElse(""),
      d.get("place").getOrElse(""),
      d.get("start").flatMap(d => parseDate(d)),
      d.get("end").flatMap(d => parseDate(d)),
      d.get("speakers").flatMap(json => if (json.isEmpty) None else Json.parse(json.replace("\r", "\\r").replace("\n", "\\n")).asOpt[List[Person]]).getOrElse(List()),
      Utils.toList(d.get("tags").getOrElse("")),
      d.get("slides"),
      d.get("video"),
      d.get("source.ref").map { ref => DataSource(ref, d.get("source.name"), d.get("source.url").getOrElse("")) },
      d.get("created").flatMap(d => parseDate(d)).getOrElse(new DateTime()),
      d.get("updated").flatMap(d => parseDate(d)).getOrElse(new DateTime())))

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
    "slides" -> e.slides.getOrElse(""),
    "video" -> e.video.getOrElse(""),
    "source.ref" -> e.source.map(_.ref).getOrElse(""),
    "source.name" -> e.source.flatMap(_.name).getOrElse(""),
    "source.url" -> e.source.map(_.url).getOrElse(""),
    "created" -> e.created.toString(FileImporter.dateFormat),
    "updated" -> e.updated.toString(FileImporter.dateFormat))
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
  tags: String,
  slides: Option[String],
  video: Option[String])
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
    "tags" -> text,
    "slides" -> optional(text),
    "video" -> optional(text))(SessionData.apply)(SessionData.unapply)

  def toModel(d: SessionData): Session = Session(Repository.generateUuid(), d.eventId, d.name, d.description, d.format, d.category, d.place, d.start, d.end, d.speakers.filter(!_.name.isEmpty), Utils.toList(d.tags), d.slides, d.video, None, new DateTime(), new DateTime())
  def fromModel(d: Session): SessionData = SessionData(d.eventId, d.name, d.description, d.format, d.category, d.place, d.start, d.end, d.speakers, Utils.fromList(d.tags), d.slides, d.video)
  def merge(m: Session, d: SessionData): Session = toModel(d).copy(uuid = m.uuid, source = m.source, created = m.created)
}
