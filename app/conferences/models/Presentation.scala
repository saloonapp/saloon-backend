package conferences.models

import common.Config
import common.models.utils._
import common.models.values.UUID
import common.services.{EmbedSrv, TwitterCard}
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json.Json

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

case class PresentationId(id: String) extends AnyVal with tString with UUID {
  def unwrap: String = this.id
}
object PresentationId extends tStringHelper[PresentationId] {
  def generate(): PresentationId = PresentationId(UUID.generate())
  def build(str: String): Either[String, PresentationId] = UUID.toUUID(str).right.map(id => PresentationId(id)).left.map(_ + " for PresentationId")
}

case class Presentation(
  conferenceId: ConferenceId,
  id: PresentationId,
  title: String,
  description: Option[String],
  slidesUrl: Option[String],
  slidesEmbedCode: Option[String],
  videoUrl: Option[String],
  videoEmbedCode: Option[String],
  speakers: List[PersonId],
  start: Option[DateTime],
  end: Option[DateTime],
  room: Option[String],
  tags: List[String],
  created: DateTime,
  createdBy: Option[User]) {
  def toTwitterCard(): TwitterCard = TwitterCard( // TODO : improve presentation twitter card
    "summary",
    "@conferencelist_",
    title,
    description.getOrElse(""),
    "https://avatars2.githubusercontent.com/u/11368266?v=3&s=200")
  def toTwitt(): String = "" // TODO : prefilled text to twitt about this presentation
}
object Presentation {
  implicit val format = Json.format[Presentation]
}

case class PresentationData(
  conferenceId: ConferenceId,
  id: Option[PresentationId],
  title: String,
  description: Option[String],
  slidesUrl: Option[String],
  videoUrl: Option[String],
  speakers: List[PersonId],
  start: Option[DateTime],
  duration: Option[Int],
  room: Option[String],
  tags: List[String],
  createdBy: User)
object PresentationData {
  val fields = mapping(
    "conferenceId" -> of[ConferenceId],
    "id" -> optional(of[PresentationId]),
    "title" -> nonEmptyText,
    "description" -> optional(nonEmptyText),
    "slidesUrl" -> optional(nonEmptyText),
    "videoUrl" -> optional(nonEmptyText),
    "speakers" -> list(of[PersonId]),
    "start" -> optional(jodaDate(pattern = Config.Application.datetimeFormat)),
    "duration" -> optional(number),
    "room" -> optional(nonEmptyText),
    "tags" -> list(nonEmptyText),
    "createdBy" -> User.fields
  )(PresentationData.apply)(PresentationData.unapply)

  def empty(conference: Conference) = PresentationData(
    conferenceId = conference.id,
    id = None,
    title = "",
    description = None,
    slidesUrl = None,
    videoUrl = None,
    speakers = List(),
    start = Some(conference.start.toDateTimeAtStartOfDay),
    duration = None,
    room = None,
    tags = List(),
    createdBy = User.empty)

  def toModel(d: PresentationData): Future[Presentation] = {
    for {
      slidesEmbedOpt <- EmbedSrv.embedCode(d.slidesUrl.getOrElse(""))
      videoEmbedOpt <- EmbedSrv.embedCode(d.videoUrl.getOrElse(""))
    } yield {
      Presentation(
        d.conferenceId,
        d.id.getOrElse(PresentationId.generate()),
        d.title,
        d.description,
        d.slidesUrl,
        slidesEmbedOpt.map(_.embedCode),
        d.videoUrl,
        videoEmbedOpt.map(_.embedCode),
        d.speakers,
        d.start,
        d.start.flatMap(s => d.duration.map(duration => s.plusMinutes(duration))),
        d.room,
        d.tags,
        new DateTime(),
        Some(d.createdBy.trim()))
    }
  }

  def fromModel(m: Presentation): PresentationData = PresentationData(
    m.conferenceId,
    Some(m.id),
    m.title,
    m.description,
    m.slidesUrl,
    m.videoUrl,
    m.speakers,
    m.start,
    m.end.flatMap(e => m.start.map(s => ((e.getMillis - s.getMillis) / (1000*60)).toInt)),
    m.room,
    m.tags,
    User.empty)
}