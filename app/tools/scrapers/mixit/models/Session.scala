package tools.scrapers.mixit.models

import org.joda.time.DateTime
import play.api.libs.json.Json
import tools.scrapers.mixit.MixitScraper


case class Session(
                    idSession: Int,
                    format: String,
                    title: String,
                    summary: String,
                    description: String,
                    ideaForNow: String,
                    lang: String,
                    room: Option[String],
                    start: Option[DateTime],
                    end: Option[DateTime],
                    year: String,
                    interests: Seq[String],
                    votes: Int,
                    positiveVotes: Int,
                    nbConsults: Int,
                    links: Seq[Link]
                  )
object Session {
  implicit val format = Json.format[Session]

  def allUrl(year: Int): String = s"${MixitScraper.baseUrl}/api/session?year=$year"
  def oneUrl(id: Int): String = s"${MixitScraper.baseUrl}/api/session/$id"
}
