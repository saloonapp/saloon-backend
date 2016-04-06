package tools.scrapers.mixit.models

import org.joda.time.DateTime
import play.api.libs.json.Json
import tools.scrapers.mixit.MixitScraper

case class SessionSpeaker(
  idMember: Int,
  firstname: String,
  lastname: String,
  hash: String,
  links: Seq[String])
case class Session(
  idSession: Int,
  format: String,
  title: String,
  summary: String,
  description: String,
  ideaForNow: String,
  lang: String,
  room: Option[String],
  start: Option[String],
  end: Option[String],
  year: String,
  interests: Seq[String],
  speakers: Seq[SessionSpeaker],
  votes: Int,
  positiveVotes: Int,
  nbConsults: Int,
  links: Seq[Link])
object Session {
  implicit val formatSessionSpeaker = Json.format[SessionSpeaker]
  implicit val format = Json.format[Session]

  def allUrl(year: Int): String = s"${MixitScraper.baseUrl}/api/session?year=$year"
  def oneUrl(id: Int): String = s"${MixitScraper.baseUrl}/api/session/$id"
}
