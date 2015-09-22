package tools.scrapers.salonreunir

import common.models.event.GenericEvent
import tools.scrapers.salonreunir.models.SalonReunirExponent
import tools.utils.CsvUtils
import tools.utils.Scraper
import tools.utils.ScraperUtils
import scala.collection.JavaConversions._
import play.api.libs.json.Json
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/*
 * List url:
 * 	- http://salon.reunir.com/liste-des-exposants/
 */
object SalonReunirExponentScraper extends Scraper[SalonReunirExponent] {
  val baseUrl = "http://salon.reunir.com"
  override def toCsv(value: SalonReunirExponent): Map[String, String] = CsvUtils.jsonToCsv(Json.toJson(value), 4)
  override def toGenericEvent(value: SalonReunirExponent): List[GenericEvent] = List()

  override def extractLinkList(html: String, baseUrl: String): List[String] = {
    val doc = Jsoup.parse(html)
    doc.select("#menu .menu").toList.map { event =>
      baseUrl + event.select("a").attr("href")
    }.map(fixUrl)
  }

  override def extractLinkPages(html: String): List[String] = {
    val doc = Jsoup.parse(html)
    val pagesElts = doc.select(".pagination li a")
    if (pagesElts.size > 0) {
      val lastPage = pagesElts.last()
      val pageUrl = baseUrl + lastPage.attr("href")
      val maxPages = lastPage.text().toInt
      (2 to maxPages).map { p => pageUrl.replace("?p=" + maxPages, "?p=" + p) }.toList.map(fixUrl)
    } else {
      List()
    }
  }

  val contactRegex = "([^<]+?)<br>([^<]+?)<br>(?:([^<]+)<br>)?".r
  override def extractDetails(html: String, baseUrl: String, pageUrl: String): SalonReunirExponent = {
    val doc = Jsoup.parse(fixEncodage(html))
    val content = doc.select("#content")

    val ref = pageUrl match {
      case urlRegex(base, id, name) => id
      case _ => pageUrl
    }
    val name = content.select("h2").text()
    val place = content.select("h2 + h3").text()
    val infos = content.select("p")
    val address = ScraperUtils.getSafe(infos, 0).map(_.text()).getOrElse("")
    val (contactName, contactPhone, contactEmail) = ScraperUtils.getSafe(infos, 1).map(c => c.html() match {
      case contactRegex(name, phone, email) => (clean(name), clean(phone), clean(email))
      case _ => (c.text(), "", "")
    }).getOrElse(("", "", ""))
    val descriptionHTML = ScraperUtils.getSafe(infos, 2).map(_.html()).getOrElse("")

    SalonReunirExponent(ref, name, place, address, contactName, contactPhone, contactEmail, descriptionHTML, pageUrl)
  }

  private def fixEncodage(str: String): String = new String(str.getBytes("iso-8859-1"), "utf8")
  private val urlRegex = "(http://salon.reunir.com/liste-des-exposants/)([0-9a-f]+)/([^/]+)/".r
  private def fixUrl(url: String): String = url match {
    case urlRegex(base, id, name) => base + id + "/e/"
    case _ => url
  }
  private def clean(str: String): String = if (str == null) "" else str.trim
}