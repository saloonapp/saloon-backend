package tools.scrapers.rivieradev.models

import java.util.Locale

import common.models.event.GenericSession
import common.models.values.Source
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import play.api.libs.json.Json
import tools.scrapers.rivieradev.RivieraDevScraper
import scala.collection.JavaConversions._

case class RivieraDevSession(
  source: Source,
  name: String,
  description: String,
  descriptionHTML: String,
  start: Option[DateTime],
  end: Option[DateTime],
  speakers: List[String]) {
  def toGeneric(): GenericSession = GenericSession(
    source = this.source,
    name = this.name,
    description = this.description,
    descriptionHTML = this.descriptionHTML,
    format = "",
    theme = "",
    place = "",
    start = this.start,
    end = this.end)
}
object RivieraDevSession {
  implicit val format = Json.format[RivieraDevSession]

  def fromHTML(html: String, url: String): List[RivieraDevSession] = {
    Jsoup.parse(html).select(".container .lines tbody tr").map(elt => fromElt(elt, url)).toList
  }

  private val scheduleRegex = """Horaire: ([\d]+ [\w]+ [\d]+ [\d]+:[\d]+) - ([\d]+:[\d]+)""".r
  private def fromElt(elt: Element, url: String): RivieraDevSession = {
    val baseUrl = url.replace("/sessions", "")
    val title = elt.select("td > a")
    val schedule = elt.select("td:nth-child(3)").text()
    val (start, end) = schedule match {
      case scheduleRegex(start, end) => {
        val startDate = DateTime.parse(start, DateTimeFormat.forPattern("dd MMM yyyy HH:mm").withLocale(Locale.FRENCH))
        val endTime = DateTime.parse(end, DateTimeFormat.forPattern("HH:mm"))
        val endDate = startDate.withHourOfDay(endTime.getHourOfDay).withMinuteOfHour(endTime.getMinuteOfHour)
        (Some(startDate), Some(endDate))
      }
      case _ => (None, None)
    }
    RivieraDevSession(
      source = Source(baseUrl+title.attr("href"), RivieraDevScraper.name, baseUrl+title.attr("href")),
      name = title.text(),
      description = elt.select("td p").map(_.text()).mkString(" "),
      descriptionHTML = elt.select("td p").map("<p>"+_.html+"</p>").mkString,
      start = start,
      end = end,
      speakers = elt.select("li a").map(baseUrl+_.attr("href")).toList)
  }
}
