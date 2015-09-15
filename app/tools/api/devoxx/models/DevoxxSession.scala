package tools.api.devoxx.models

import play.api.libs.json.Json
import org.joda.time.DateTime

case class DevoxxRoom(
  id: String,
  name: String,
  capacity: Int,
  setup: String,
  recorded: Boolean)
case class DevoxxSessionBreak(
  id: String,
  nameFR: String,
  nameEN: String,
  room: DevoxxRoom)
case class DevoxxSessionTalkSpeaker(
  name: String,
  link: Link)
case class DevoxxSessionTalk(
  id: String,
  title: String,
  talkType: String,
  track: String,
  summary: String,
  summaryAsHtml: String,
  lang: String,
  speakers: List[DevoxxSessionTalkSpeaker],
  demoLevel: String,
  audienceLevel: String,
  livecoding: Boolean)
case class DevoxxSession(
  slotId: String,
  day: String,
  roomId: String,
  roomName: String,
  roomCapacity: Int,
  roomSetup: String,
  fromTimeMillis: DateTime,
  toTimeMillis: DateTime,
  fromTime: String,
  toTime: String,
  notAllocated: Boolean,
  break: Option[DevoxxSessionBreak],
  talk: Option[DevoxxSessionTalk],
  sourceUrl: Option[String])
object DevoxxSession {
  implicit val formatDevoxxRoom = Json.format[DevoxxRoom]
  implicit val formatDevoxxSessionBreak = Json.format[DevoxxSessionBreak]
  implicit val formatDevoxxSessionTalkSpeaker = Json.format[DevoxxSessionTalkSpeaker]
  implicit val formatDevoxxSessionTalk = Json.format[DevoxxSessionTalk]
  implicit val format = Json.format[DevoxxSession]
}