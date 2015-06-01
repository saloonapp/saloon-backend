package tools.scrapers.foiresetsalons.pages

import tools.scrapers.foiresetsalons.models.FESOrga
import tools.scrapers.foiresetsalons.models.FESStats
import tools.scrapers.foiresetsalons.models.FESEvent
import scala.collection.JavaConversions._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object FESDetailsPage {
  // (.*) => normal group
  // (.*?) => smallest group possible
  // (?:.*) => non-capturing group

  def extract(html: String, url: String): FESEvent = {
    val doc = Jsoup.parse(html)
    val data = doc.select("#centre p")
    val namedValues = data.toList.map { elt => parseElt(elt.html) }.flatten.toMap
    val (start, end) = parseDates(namedValues.get("Date").getOrElse(""))

    FESEvent(
      url,
      doc.select("#hautp h2").text(),
      namedValues.get("Adresse de tenue").getOrElse("").replace("<br>", " ").trim,
      namedValues.get("Catégorie").getOrElse(""),
      namedValues.get("Condition d'accès").getOrElse("").split("<br>").toList.map(_.trim).filter(_ != ""),
      start,
      end,
      namedValues.get("Secteur d'activité").getOrElse("").split(" - ").toList,
      namedValues.get("Produits et services présentés (\"nomenclature\")").getOrElse("").split(", ").toList,
      parseStats(data.html),
      FESOrga(
        namedValues.get("Raison sociale ou nom et prénom").getOrElse("").split("<br>")(0).trim,
        parseSigle(namedValues.get("Raison sociale ou nom et prénom").getOrElse("")),
        namedValues.get("Adresse").getOrElse("").replace("<br>", " ").trim,
        namedValues.get("Téléphone").getOrElse("").split("<br>")(0),
        fixUrl(namedValues.get("Adresse du site internet").getOrElse(""))))
  }

  val eltParser1 = "<b>([^<]+?) ?:? ?</b> ?:? ?(.+)".r
  val eltParser2 = "([^<]+?) ?: ?([^<]+)".r
  private def parseElt(html: String): Option[(String, String)] = html match {
    case eltParser1(key, value) => Some((key, value.replace("&nbsp;", " ").trim))
    case eltParser2(key, value) => Some((key, value.replace("&nbsp;", " ").trim))
    case err => None
  }

  val datesParser = "du ([0-9]+/[0-9]+/[0-9]+) au ([0-9]+/[0-9]+/[0-9]+)".r
  private def parseDates(str: String): (String, String) = str match {
    case datesParser(start, end) => (start, end)
    case err => (err, err)
  }

  val statsParser = "(?is)(?:.*)\nSurface nette: ([0-9]+) m² <br> Nombre de visites: ([0-9]+)<br> Nombre d'exposants : ([0-9]+)<br> Nombre de visiteurs : ([0-9]+)<br>(?: <br>Dénomination de l'organisme de certification : ([^\n]+))?\n(?:.*)".r
  private def parseStats(html: String): FESStats = html match {
    case statsParser(area, venues, exponents, visitors, certified) => FESStats(area.toInt, exponents.toInt, visitors.toInt, venues.toInt, certified)
    case err => FESStats(0, 0, 0, 0, err)
  }

  val sigleParser = "(?:.*?)Sigle : (.*)".r
  private def parseSigle(str: String): String = str match {
    case sigleParser(res) => res
    case err => ""
  }

  private def fixUrl(url: String): String = if (url.startsWith("http") || url.isEmpty()) url else "http://" + url
}
