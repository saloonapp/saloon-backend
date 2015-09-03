package backend.forms

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
import org.jsoup.Jsoup

case class SessionCreateData(
  eventId: EventId,
  name: String,
  descriptionHTML: String,
  format: String,
  theme: String,
  place: String,
  start: Option[DateTime],
  end: Option[DateTime],
  speakers: List[AttendeeId],
  slides: Option[String],
  video: Option[String])
object SessionCreateData {
  val fields = mapping(
    "eventId" -> of[EventId],
    "name" -> nonEmptyText,
    "descriptionHTML" -> text,
    "format" -> text,
    "theme" -> text,
    "place" -> text,
    "start" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
    "end" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
    "speakers" -> list(of[AttendeeId]),
    "slides" -> optional(text),
    "video" -> optional(text))(SessionCreateData.apply)(SessionCreateData.unapply)

  def toMeta(d: SessionCreateData): SessionMeta = SessionMeta(None, new DateTime(), new DateTime())
  def toInfo(d: SessionCreateData): SessionInfo = SessionInfo(d.format, d.theme, d.place, d.start, d.end, d.speakers, d.slides, d.video)
  def toImages(d: SessionCreateData): SessionImages = SessionImages("")
  def toModel(d: SessionCreateData): Session = Session(SessionId.generate(), d.eventId, d.name, Jsoup.parse(d.descriptionHTML).text(), d.descriptionHTML, toImages(d), toInfo(d), toMeta(d))
  def fromModel(d: Session): SessionCreateData = SessionCreateData(d.eventId, d.name, d.description, d.info.format, d.info.theme, d.info.place, d.info.start, d.info.end, d.info.speakers, d.info.slides, d.info.video)
  def merge(m: Session, d: SessionCreateData): Session = m.copy(name = d.name, description = Jsoup.parse(d.descriptionHTML).text(), descriptionHTML = d.descriptionHTML, images = toImages(d), info = toInfo(d), meta = m.meta.copy(updated = new DateTime()))
}