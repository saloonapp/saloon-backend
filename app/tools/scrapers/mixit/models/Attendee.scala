package tools.scrapers.mixit.models

import play.api.libs.json.Json
import tools.scrapers.mixit.MixitScraper

case class Attendee(
                    idMember: Int,
                    login: String,
                    firstname: String,
                    lastname: String,
                    company: Option[String],
                    logo: Option[String],
                    hash: String,
                    sessionType: Option[String],
                    shortDescription: Option[String],
                    longDescription: Option[String],
                    userLinks: Seq[KeyValue],
                    interests: Seq[String],
                    sessions: Seq[Int],
                    level: Seq[KeyValue],
                    links: Seq[Link]
                  )
object Attendee {
  implicit val format = Json.format[Attendee]

  def allSpeakerUrl(year: Int): String = s"${MixitScraper.baseUrl}/api/member/speaker?year=$year"
  def allStaffUrl(year: Int): String = s"${MixitScraper.baseUrl}/api/member/staff?year=$year"
  def oneUrl(id: Int): String = s"${MixitScraper.baseUrl}/api/member/$id"
}
