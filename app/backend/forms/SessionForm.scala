package backend.forms

import common.models.event.Session
import common.models.event.SessionImages
import common.models.event.SessionInfo
import common.models.event.SessionMeta
import common.repositories.Repository
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json._

case class SessionCreateData(
  eventId: String,
  name: String,
  descriptionHTML: String,
  format: String,
  category: String,
  place: String,
  start: Option[DateTime],
  end: Option[DateTime],
  speakers: List[String],
  slides: Option[String],
  video: Option[String])
object SessionCreateData {
  val fields = mapping(
    "eventId" -> nonEmptyText,
    "name" -> nonEmptyText,
    "descriptionHTML" -> text,
    "format" -> text,
    "category" -> text,
    "place" -> text,
    "start" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
    "end" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
    "speakers" -> list(text),
    "slides" -> optional(text),
    "video" -> optional(text))(SessionCreateData.apply)(SessionCreateData.unapply)

  def toMeta(d: SessionCreateData): SessionMeta = SessionMeta(None, new DateTime(), new DateTime())
  def toInfo(d: SessionCreateData): SessionInfo = SessionInfo(d.format, d.category, d.place, d.start, d.end, d.speakers, d.slides, d.video)
  def toImages(d: SessionCreateData): SessionImages = SessionImages("")
  def toModel(d: SessionCreateData): Session = Session(Repository.generateUuid(), d.eventId, d.name, d.descriptionHTML, toImages(d), toInfo(d), toMeta(d))
  def fromModel(d: Session): SessionCreateData = SessionCreateData(d.eventId, d.name, d.description, d.info.format, d.info.category, d.info.place, d.info.start, d.info.end, d.info.speakers, d.info.slides, d.info.video)
  def merge(m: Session, d: SessionCreateData): Session = m.copy(name = d.name, description = d.descriptionHTML, images = toImages(d), info = toInfo(d), meta = m.meta.copy(updated = new DateTime()))
}