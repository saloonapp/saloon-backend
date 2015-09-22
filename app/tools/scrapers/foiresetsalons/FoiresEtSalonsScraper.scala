package tools.scrapers.foiresetsalons

import common.models.event.GenericEvent
import tools.utils.CsvUtils
import tools.utils.Scraper
import tools.utils.ScraperUtils
import tools.scrapers.foiresetsalons.models.FoiresEtSalonsEvent
import tools.scrapers.foiresetsalons.models.FoiresEtSalonsAddress
import tools.scrapers.foiresetsalons.models.FoiresEtSalonsStats
import tools.scrapers.foiresetsalons.models.FoiresEtSalonsOrga
import scala.collection.JavaConversions._
import play.api.libs.json.Json
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.joda.time.DateTime

/*
 * List page ex :
 *  - https://www.foiresetsalons.entreprises.gouv.fr/salon.php
 *  - https://www.foiresetsalons.entreprises.gouv.fr/catalogue.php
 * Details page ex :
 *  - https://www.foiresetsalons.entreprises.gouv.fr/fichemanif.php?manif=16438&decl=71
 */
object FoiresEtSalonsScraper extends Scraper[FoiresEtSalonsEvent] {
  val baseUrl = "https://www.foiresetsalons.entreprises.gouv.fr"
  override def toCsv(value: FoiresEtSalonsEvent): Map[String, String] = CsvUtils.jsonToCsv(Json.toJson(value), 4)
  override def toGenericEvent(value: FoiresEtSalonsEvent): List[GenericEvent] = List(value).map(e => FoiresEtSalonsEvent.toGenericEvent(e))

  override def extractLinkList(html: String, baseUrl: String): List[String] = {
    val doc = Jsoup.parse(fixEncodage(html))
    doc.select("div#centre > form > table[border] tr:not([align])").map { row =>
      baseUrl + "/" + row.select("a").attr("href")
    }.toList
  }

  val dateRegex = "du ([0-9]{2}/[0-9]{2}/[0-9]{4}) au ([0-9]{2}/[0-9]{2}/[0-9]{4})".r
  val statsRegex = "Surface nette: ([0-9]*) ?m² <br> Nombre de visites: ([0-9]*)<br> Nombre d'exposants : ([0-9]*)<br> Nombre de visiteurs : ([0-9]*)<br>(?: <br>Dénomination de l'organisme de certification : ([^\n]+))?".r.unanchored
  val orgaNameRegex = "(.*?)<br>Sigle :(.*?)".r
  val websiteRegex = ".*?: (.*)".r
  override def extractDetails(html: String, baseUrl: String, pageUrl: String): FoiresEtSalonsEvent = {
    val doc = Jsoup.parse(fixEncodage(html))
    val sections = doc.select("#centre p")
    val sectionIndexes = sections.toList.zipWithIndex.map { case (section, i) => (ScraperUtils.firstSafe(section.select("b")).map(_.text()).getOrElse(""), i) }.toMap

    val name = doc.select("#hautp h2").text()
    val address = FoiresEtSalonsAddress.build(getItem(sections, sectionIndexes, "Adresse de tenue :").map(_.split("<br>").map(_.trim()).filter(!_.isEmpty()).toList).getOrElse(List()))
    val category = getItem(sections, sectionIndexes, "Catégorie").getOrElse("")
    val access = getItem(sections, sectionIndexes, "Condition d'accès").getOrElse("").split("<br>").toList.map(_.trim).filter(_ != "")
    val (start, end) = getItem(sections, sectionIndexes, "Date").getOrElse("") match {
      case dateRegex(start, end) => (ScraperUtils.parseDate(start, "dd/MM/yyyy"), ScraperUtils.parseDate(end, "dd/MM/yyyy"))
      case _ => (None, None)
    }
    val sectors = getItem(sections, sectionIndexes, "Secteur d'activité :").getOrElse("").split(" - ").toList
    val products = getItem(sections, sectionIndexes, "Produits et services présentés (\"nomenclature\")").getOrElse("").split(", ").toList.map(_.trim).filter(_ != "")
    val stats = sections.html() match {
      case statsRegex(area, venues, exponents, visitors, certified) => FoiresEtSalonsStats(toIntSafe(area), toIntSafe(venues), toIntSafe(exponents), toIntSafe(visitors), notNull(certified))
      case _ => FoiresEtSalonsStats(0, 0, 0, 0, "")
    }
    val orgaNameStr = getItem(sections, sectionIndexes, "Raison sociale ou nom et prénom :").getOrElse("")
    val (orgaName, orgaSigle) = orgaNameStr match {
      case orgaNameRegex(name, sigle) => (name.trim, sigle.trim)
      case _ => (orgaNameStr, "")
    }
    val websiteIndexOpt = sectionIndexes.get("Téléphone").orElse(sectionIndexes.get("Adresse :"))
    val orga = FoiresEtSalonsOrga(
      orgaName,
      orgaSigle,
      FoiresEtSalonsAddress.build(getItem(sections, sectionIndexes, "Adresse :").map(_.split("<br>").map(_.trim()).filter(!_.isEmpty()).toList).getOrElse(List())),
      getItem(sections, sectionIndexes, "Téléphone").getOrElse("").split("<br>").toList.headOption.getOrElse(""),
      websiteIndexOpt.flatMap { index =>
        if (index < sections.length - 1) {
          sections(index + 1).html() match {
            case websiteRegex(website) => Some(fixUrl(website))
            case _ => None
          }
        } else { None }
      }.getOrElse(""))

    FoiresEtSalonsEvent(name, address, category, access, start, end, sectors, products, stats, orga, pageUrl)
  }

  val itemRegex = "<b>(.*?) ? ?:? ?</b> ?:? ?(.*)".r
  private def getItem(sections: Elements, sectionIndexes: Map[String, Int], name: String): Option[String] = {
    sectionIndexes.get(name).map { index =>
      getItem(sections, index)
    }
  }
  private def getItem(sections: Elements, index: Int): String = {
    sections(index).html() match {
      case itemRegex(name, content) => content
      case _ => sections(index).html()
    }
  }

  private def fixEncodage(str: String): String = new String(str.getBytes("iso-8859-1"), "utf8")
  private def notNull(str: String): String = if (str == null) "" else str
  private def toIntSafe(str: String): Int = if (str.isEmpty()) 0 else str.toInt
  private def fixUrl(url: String): String = if (url.startsWith("http") || url.isEmpty()) url else "http://" + url
}
