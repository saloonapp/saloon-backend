package tools.scrapers.rivieradev.models

import common.models.event.GenericExponent
import common.models.values.Source
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import play.api.libs.json.Json
import tools.scrapers.rivieradev.RivieraDevScraper
import scala.collection.JavaConversions._

case class RivieraDevSponsor(
  source: Source,
  name: String,
  description: String,
  descriptionHTML: String,
  logo: String,
  website: String,
  twitterUrl: Option[String]) {
  def toGeneric(): GenericExponent = GenericExponent(
    source = this.source,
    name = this.name,
    description = this.description,
    descriptionHTML = this.descriptionHTML,
    logo = this.logo,
    website = this.website,
    place = "")
}
object RivieraDevSponsor {
  implicit val format = Json.format[RivieraDevSponsor]

  def fromHTML(html: String, url: String): List[RivieraDevSponsor] = {
    Jsoup.parse(html).select(".container .sponsors tbody tr").map(elt => fromElt(elt, url)).toList
  }

  private def fromElt(elt: Element, url: String): RivieraDevSponsor = {
    val baseUrl = url.replace("/sponsors", "")
    val logoElt = elt.select("td:nth-child(1)")
    val detailsElt = elt.select("td:nth-child(2)")
    val detailsBlocks = detailsElt.html().split("<br>").map(Jsoup.parse).map(block => {
      (block.select(".label").text(), block)
    }).toMap
    val logo = baseUrl+logoElt.select("img").attr("src")
    RivieraDevSponsor(
    source = Source(logo.split("id=").drop(1).headOption.getOrElse("?"), RivieraDevScraper.name, url),
      name = detailsElt.select(".sponsor a").text(),
      description = detailsElt.select("p").map(_.text()).mkString(" "),
      descriptionHTML = detailsElt.select("p").map("<p>"+_.html+"</p>").mkString,
      logo = logo,
      website = logoElt.select("a").attr("href"),
      twitterUrl = detailsBlocks.get("Twitter:").map(_.select("a").attr("href")))
  }
}
