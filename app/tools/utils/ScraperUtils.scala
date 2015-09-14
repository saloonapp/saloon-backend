package tools.utils

import scala.collection.JavaConversions._
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
}
