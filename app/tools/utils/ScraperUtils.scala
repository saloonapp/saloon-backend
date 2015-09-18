package tools.utils

import scala.util.matching.Regex
import scala.util.Try
import scala.util.Failure
import scala.collection.JavaConversions._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WS
import play.api.libs.ws.WSResponse
import play.api.Play.current
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.util.Locale

object ScraperUtils {
  /*
   * Jsoup helpers
   */
  def firstSafe(elts: Elements): Option[Element] = if (elts != null && elts.length > 0) Some(elts.first()) else None
  def getSafe(elts: Elements, index: Int): Option[Element] = if (elts != null && elts.length > index) Some(elts.get(index)) else None
  def getLink(elt: Element, baseUrl: String): Option[(String, String)] = if (elt == null) None else Some((elt.text(), baseUrl + elt.attr("href")))
  def getLink(elts: Elements, baseUrl: String = ""): Option[(String, String)] = firstSafe(elts).flatMap(elt => getLink(elt, baseUrl))

  /*
   * Regex helpers
   */
  def get(source: String, regex: Regex): Option[String] = source match {
    case regex(value) => Some(value)
    case _ => None
  }
  def get(source: String, regex: Regex, default: String): String = get(source, regex).getOrElse(default)

  /*
   * Date functions
   */
  def parseDate(date: String, pattern: String = "MMM dd, yyyy", locale: Locale = Locale.ENGLISH): Option[DateTime] =
    if (date.isEmpty()) {
      None
    } else {
      Some(DateTime.parse(monthReplace(date), DateTimeFormat.forPattern(pattern).withLocale(locale)))
    }

  private def monthReplace(month: String): String = month
    .replace("Jan.", "January")
    .replace("Feb.", "February")
    .replace("Aug.", "August")
    .replace("Sept.", "September")
    .replace("Oct.", "October")
    .replace("Nov.", "November")
    .replace("Dec.", "December")

  /*
   * WS helpers
   */
  def fetch(url: String): Future[Try[WSResponse]] = {
    WS.url(url).get().map { response =>
      Try(response)
    }.recover {
      // http://www.bimeanalytics.com/engineering-blog/retrying-http-request-in-scala/
      case e => Failure(e)
    }
  }
}
