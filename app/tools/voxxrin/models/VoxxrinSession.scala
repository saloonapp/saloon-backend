package tools.voxxrin.models

import common.Utils
import models.Session
import infrastructure.repository.common.Repository
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
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
      "",
      this.title,
      this.summary.map(html => Utils.htmlToText(html)).getOrElse(""),
      this.`type`,
      this.kind,
      this.room.toPlace(),
      VoxxrinSession.parseDate(this.fromTime),
      VoxxrinSession.parseDate(this.toTime),
      this.tags.getOrElse(List()),
      new DateTime(),
      new DateTime())
  }
}
object VoxxrinSession {
  implicit val format = Json.format[VoxxrinSession]
  def parseDate(date: String): Option[DateTime] = if (date.isEmpty) None else Some(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS").parseDateTime(date))
}
