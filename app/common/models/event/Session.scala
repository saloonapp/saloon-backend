package common.models.event

import common.Utils
import common.repositories.Repository
import common.models.values.DataSource
import common.services.FileImporter
import org.joda.time.DateTime
import scala.util.Try
import play.api.data.Forms._
import play.api.libs.json.Json
import org.jsoup.Jsoup

case class SessionImages(
  landing: String) // landscape img (~ 400x150)
case class SessionInfo(
  format: String,
  theme: String,
  place: String, // where to find this session
  start: Option[DateTime],
  end: Option[DateTime],
  speakers: List[String],
  slides: Option[String],
  video: Option[String])
case class SessionMeta(
  source: Option[DataSource], // where the session were fetched (if applies)
  created: DateTime,
  updated: DateTime)
case class Session(
  uuid: String,
  eventId: String,
  name: String,
  description: String,
  descriptionHTML: String,
  images: SessionImages,
  info: SessionInfo,
  meta: SessionMeta) extends EventItem {
  def merge(e: Session): Session = Session.merge(this, e)
  def toBackendExport(): Map[String, String] = Session.toBackendExport(this)
  //def toMap(): Map[String, String] = Session.toMap(this)
}
object Session {
  val className = "sessions"
  implicit val formatSessionImages = Json.format[SessionImages]
  implicit val formatSessionInfo = Json.format[SessionInfo]
  implicit val formatSessionMeta = Json.format[SessionMeta]
  implicit val format = Json.format[Session]

  def toBackendExport(e: Session): Map[String, String] = Map(
    "uuid" -> e.uuid,
    "name" -> e.name,
    "description" -> e.description,
    "format" -> e.info.format,
    "theme" -> e.info.theme,
    "place" -> e.info.place,
    "start" -> e.info.start.map(_.toString(FileImporter.dateFormat)).getOrElse(""),
    "end" -> e.info.end.map(_.toString(FileImporter.dateFormat)).getOrElse(""),
    "speakerUuids" -> Utils.fromList(e.info.speakers),
    "slides" -> e.info.slides.getOrElse(""),
    "video" -> e.info.video.getOrElse(""),
    "created" -> e.meta.created.toString(FileImporter.dateFormat),
    "updated" -> e.meta.updated.toString(FileImporter.dateFormat))

  def merge(e1: Session, e2: Session): Session = Session(
    e1.uuid,
    e1.eventId,
    merge(e1.name, e2.name),
    merge(e1.description, e2.description),
    merge(e1.descriptionHTML, e2.descriptionHTML),
    merge(e1.images, e2.images),
    merge(e1.info, e2.info),
    merge(e1.meta, e2.meta))
  private def merge(e1: SessionImages, e2: SessionImages): SessionImages = SessionImages(
    merge(e1.landing, e2.landing))
  private def merge(e1: SessionInfo, e2: SessionInfo): SessionInfo = SessionInfo(
    merge(e1.format, e2.format),
    merge(e1.theme, e2.theme),
    merge(e1.place, e2.place),
    merge(e1.start, e2.start),
    merge(e1.end, e2.end),
    merge(e1.speakers, e2.speakers),
    merge(e1.slides, e2.slides),
    merge(e1.video, e2.video))
  private def merge(e1: SessionMeta, e2: SessionMeta): SessionMeta = SessionMeta(
    merge(e1.source, e2.source),
    e1.created,
    e2.updated)
  private def merge(e1: String, e2: String): String = if (e2.isEmpty) e1 else e2
  private def merge[A](e1: Option[A], e2: Option[A]): Option[A] = if (e2.isEmpty) e1 else e2
  private def merge[A](e1: List[A], e2: List[A]): List[A] = if (e2.isEmpty) e1 else e2

  /*def fromMap(eventId: String)(d: Map[String, String]): Try[Session] =
    Try(Session(
      d.get("uuid").flatMap(u => if (u.isEmpty) None else Some(u)).getOrElse(Repository.generateUuid()),
      eventId,
      d.get("name").get,
      d.get("description").getOrElse(""),
      SessionImages(
        d.get("images.landing").getOrElse("")),
      SessionInfo(
        d.get("info.format").getOrElse(""),
        d.get("info.category").getOrElse(""),
        d.get("info.place").getOrElse(""),
        d.get("info.start").flatMap(d => parseDate(d)),
        d.get("info.end").flatMap(d => parseDate(d)),
        Utils.toList(d.get("info.speakers").getOrElse("")),
        d.get("info.slides"),
        d.get("info.video")),
      SessionMeta(
        d.get("meta.source.ref").map { ref => DataSource(ref, d.get("meta.source.name").getOrElse(""), d.get("meta.source.url").getOrElse("")) },
        d.get("meta.created").flatMap(d => parseDate(d)).getOrElse(new DateTime()),
        d.get("meta.updated").flatMap(d => parseDate(d)).getOrElse(new DateTime()))))

  def toMap(e: Session): Map[String, String] = Map(
    "uuid" -> e.uuid,
    "eventId" -> e.eventId,
    "name" -> e.name,
    "description" -> e.description,
    "images.landing" -> e.images.landing,
    "info.format" -> e.info.format,
    "info.category" -> e.info.category,
    "info.place" -> e.info.place,
    "info.start" -> e.info.start.map(_.toString(FileImporter.dateFormat)).getOrElse(""),
    "info.end" -> e.info.end.map(_.toString(FileImporter.dateFormat)).getOrElse(""),
    "info.speakers" -> Utils.fromList(e.info.speakers),
    "info.slides" -> e.info.slides.getOrElse(""),
    "info.video" -> e.info.video.getOrElse(""),
    "meta.source.ref" -> e.meta.source.map(_.ref).getOrElse(""),
    "meta.source.name" -> e.meta.source.map(_.name).getOrElse(""),
    "meta.source.url" -> e.meta.source.map(_.url).getOrElse(""),
    "meta.created" -> e.meta.created.toString(FileImporter.dateFormat),
    "meta.updated" -> e.meta.updated.toString(FileImporter.dateFormat))
  private def parseDate(date: String) = Utils.parseDate(FileImporter.dateFormat)(date)*/
}

// mapping object for Session Form
case class SessionMetaData(
  source: Option[DataSource])
case class SessionData(
  eventId: String,
  name: String,
  description: String,
  descriptionHTML: String,
  images: SessionImages,
  info: SessionInfo,
  meta: SessionMetaData)
object SessionData {
  val fields = mapping(
    "eventId" -> nonEmptyText,
    "name" -> nonEmptyText,
    "description" -> text,
    "descriptionHTML" -> text,
    "images" -> mapping(
      "landing" -> text)(SessionImages.apply)(SessionImages.unapply),
    "info" -> mapping(
      "format" -> text,
      "theme" -> text,
      "place" -> text,
      "start" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
      "end" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
      "speakers" -> list(text),
      "slides" -> optional(text),
      "video" -> optional(text))(SessionInfo.apply)(SessionInfo.unapply),
    "meta" -> mapping(
      "source" -> optional(DataSource.fields))(SessionMetaData.apply)(SessionMetaData.unapply))(SessionData.apply)(SessionData.unapply)

  def toModel(d: SessionMetaData): SessionMeta = SessionMeta(d.source, new DateTime(), new DateTime())
  def toModel(d: SessionData): Session = Session(Repository.generateUuid(), d.eventId, d.name, d.description, d.descriptionHTML, d.images, d.info, toModel(d.meta))
  def fromModel(d: SessionMeta): SessionMetaData = SessionMetaData(d.source)
  def fromModel(d: Session): SessionData = SessionData(d.eventId, d.name, d.description, d.descriptionHTML, d.images, d.info, fromModel(d.meta))
  def merge(m: SessionMeta, d: SessionMetaData): SessionMeta = toModel(d).copy(source = m.source, created = m.created)
  def merge(m: Session, d: SessionData): Session = toModel(d).copy(uuid = m.uuid, meta = merge(m.meta, d.meta))
}
