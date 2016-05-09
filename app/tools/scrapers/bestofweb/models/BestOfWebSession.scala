package tools.scrapers.bestofweb.models

import common.models.event.GenericSession
import common.models.values.Source
import org.joda.time.DateTime
import org.jsoup.nodes.Element
import tools.scrapers.bestofweb.BestOfWebScraper
import tools.utils.TextUtils
import scala.collection.JavaConversions._

case class BestOfWebSession(
  name: String,
  description: String,
  descriptionHTML: String,
  hour: String,
  speakers: List[String]) {
  def toGenericSession(sourceName: String, day: DateTime, format: String): GenericSession = {
    val hours = this.hour.split("-")
    val startOpt = hours.headOption.map(h => changeTime(day, h))
    val endOpt = hours.drop(1).headOption.map(h => changeTime(day, h))
    GenericSession(
      source = Source(TextUtils.tokenify(this.name), sourceName, BestOfWebScraper.baseUrl),
      name = this.name,
      description = this.description,
      descriptionHTML = this.descriptionHTML,
      format = format,
      theme = "",
      place = "",
      start = startOpt,
      end = endOpt)
  }
  private def changeTime(d: DateTime, time: String): DateTime = {
    val parts = time.split(":")
    d.withHourOfDay(parts(0).toInt).withMinuteOfHour(parts(1).toInt)
  }
}
object BestOfWebSession {
  def fromMedia(m: Element): BestOfWebSession = BestOfWebSession(
    name = m.select(".media-heading").text(),
    description = m.select("p").text(),
    descriptionHTML = m.select("p").html(),
    hour = m.select(".time-label").text(), // 08:00, 09:00, 09:30... || 10:00-13:00, 14:00-17:00...
    speakers = m.select(".media-left a").map(_.attr("href")).toList)
}
