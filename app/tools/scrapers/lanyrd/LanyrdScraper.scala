package tools.scrapers.lanyrd

import tools.utils.Scraper
import tools.scrapers.lanyrd.models.LanyrdEvent
import scala.collection.JavaConversions._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.Locale
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

/*
 * List page ex :
 *  - http://lanyrd.com/places/7153319/
 * Details page ex :
 *  - http://lanyrd.com/2015/agilefrance/
 */
object LanyrdScraper extends Scraper[LanyrdEvent] {
  val baseUrl = "http://lanyrd.com"

  override def extractLinkList(html: String, baseUrl: String): List[String] = {
    List()
  }

  override def extractDetails(html: String, baseUrl: String, pageUrl: String): LanyrdEvent = {
    LanyrdEvent("")
  }
}
