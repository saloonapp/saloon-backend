package tools.scrapers.foiresetsalons

import tools.utils.Scraper
import tools.utils.ScraperUtils
import tools.scrapers.foiresetsalons.models.FoiresEtSalonsEvent
import scala.collection.JavaConversions._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.util.Locale

/*
 * List page ex :
 *  - https://www.foiresetsalons.entreprises.gouv.fr/salon.php
 *  - https://www.foiresetsalons.entreprises.gouv.fr/catalogue.php
 * Details page ex :
 *  - https://www.foiresetsalons.entreprises.gouv.fr/fichemanif.php?manif=16438&decl=71
 */
object FoiresEtSalonsScraper extends Scraper[FoiresEtSalonsEvent] {
  val baseUrl = "https://www.foiresetsalons.entreprises.gouv.fr"

  override def extractLinkList(html: String, baseUrl: String): List[String] = {
    val doc = Jsoup.parse(fixEncodage(html))
    doc.select("div#centre > form > table[border] tr:not([align])").map { row =>
      baseUrl + "/" + row.select("a").attr("href")
    }.toList
  }

  override def extractDetails(html: String, baseUrl: String, pageUrl: String): FoiresEtSalonsEvent = {
    val doc = Jsoup.parse(fixEncodage(html))
    // TODO
    FoiresEtSalonsEvent("")
  }

  private def fixEncodage(str: String): String = new String(str.getBytes("iso-8859-1"), "utf8")
}
