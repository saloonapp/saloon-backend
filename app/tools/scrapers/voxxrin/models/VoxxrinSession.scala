package tools.scrapers.voxxrin.models

import common.Utils
import common.infrastructure.repository.Repository
import models.values.DataSource
import models.event.Session
import models.event.SessionImages
import models.event.SessionInfo
import models.event.SessionMeta
import tools.scrapers.voxxrin.VoxxrinApi
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTimeZone
import play.api.libs.json.Json

case class VoxxrinSession(
  id: String,
  dayId: String,
  title: String,
  summary: Option[String],
  `type`: String,
  kind: String,
  track: Option[String],
  slot: String,
  fromTime: String,
  toTime: String,
  room: VoxxrinRoom,
  speakers: Option[List[VoxxrinSpeaker]],
  tags: Option[List[String]],
  experience: Option[Int],
  nextId: Option[String],
  previousId: Option[String],
  uri: String) {
  def toSession(eventId: String, sessionId: String = Repository.generateUuid()): Session = {
    Session(
      sessionId,
      eventId,
      this.title,
      this.summary.map(html => Utils.htmlToText(html)).getOrElse(""),
      SessionImages(""),
      SessionInfo(
        this.`type`,
        this.kind,
        this.room.toPlace(),
        VoxxrinSession.parseDate(this.fromTime),
        VoxxrinSession.parseDate(this.toTime),
        this.speakers.map(_.map(_.toSpeaker())).getOrElse(List()),
        Some(List()),
        None,
        None),
      SessionMeta(
        Some(DataSource(this.id, "Voxxrin API", VoxxrinApi.baseUrl + this.uri)),
        new DateTime(),
        new DateTime()))
  }
}
object VoxxrinSession {
  implicit val format = Json.format[VoxxrinSession]
  private val datePattern = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")
  def parseDate(date: String): Option[DateTime] = if (date.isEmpty) None else Some(datePattern.parseDateTime(date))
}
