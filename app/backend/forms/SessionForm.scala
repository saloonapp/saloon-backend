package backend.forms

import common.models.utils.tStringConstraints._
import common.models.values.typed._
import common.models.event.EventId
import common.models.event.AttendeeId
import common.models.event.Session
import common.models.event.SessionId
import common.models.event.SessionImages
import common.models.event.SessionInfo
import common.models.event.SessionMeta
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json._

case class SessionCreateData(
  eventId: EventId,
  name: FullName,
  descriptionHTML: TextHTML,
  format: String,
  theme: String,
  place: EventLocation,
  start: Option[DateTime],
  end: Option[DateTime],
  speakers: List[AttendeeId],
  slides: Option[WebsiteUrl],
  video: Option[WebsiteUrl])
object SessionCreateData {
  val fields = mapping(
    "eventId" -> of[EventId].verifying(nonEmpty),
    "name" -> of[FullName].verifying(nonEmpty),
    "descriptionHTML" -> of[TextHTML],
    "format" -> text,
    "theme" -> text,
    "place" -> of[EventLocation],
    "start" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
    "end" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
    "speakers" -> list(of[AttendeeId]),
    "slides" -> optional(of[WebsiteUrl]),
    "video" -> optional(of[WebsiteUrl]))(SessionCreateData.apply)(SessionCreateData.unapply)

  def toMeta(d: SessionCreateData): SessionMeta = SessionMeta(None, new DateTime(), new DateTime())
  def toInfo(d: SessionCreateData): SessionInfo = SessionInfo(d.format, d.theme, d.place, d.start, d.end, d.speakers, d.slides, d.video)
  def toImages(d: SessionCreateData): SessionImages = SessionImages(ImageUrl(""))
  def toModel(d: SessionCreateData): Session = Session(SessionId.generate(), d.eventId, d.name, d.descriptionHTML.toPlainText, d.descriptionHTML, toImages(d), toInfo(d), toMeta(d))
  def fromModel(d: Session): SessionCreateData = SessionCreateData(d.eventId, d.name, d.descriptionHTML, d.info.format, d.info.theme, d.info.place, d.info.start, d.info.end, d.info.speakers, d.info.slides, d.info.video)
  def merge(m: Session, d: SessionCreateData): Session = m.copy(name = d.name, description = d.descriptionHTML.toPlainText, descriptionHTML = d.descriptionHTML, images = toImages(d), info = toInfo(d), meta = m.meta.copy(updated = new DateTime()))
}