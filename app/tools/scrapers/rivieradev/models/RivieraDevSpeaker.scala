package tools.scrapers.rivieradev.models

import common.models.event.GenericAttendee
import common.models.values.Source
import org.jsoup.Jsoup
import play.api.libs.json.Json
import tools.scrapers.rivieradev.RivieraDevScraper
import scala.collection.JavaConversions._

case class RivieraDevSpeaker(
  source: Source,
  avatar: String,
  firstName: String,
  lastName: String,
  title: String,
  description: String,
  descriptionHTML: String,
  company: Option[String],
  companyUrl: Option[String],
  twitterUrl: Option[String],
  blogUrl: Option[String],
  sessions: List[String]) {
  def toGeneric(): GenericAttendee = GenericAttendee(
    source = this.source,
    firstName = this.firstName,
    lastName = this.lastName,
    avatar = this.avatar,
    description = this.description,
    descriptionHTML = this.descriptionHTML,
    role = "",
    siteUrl = this.blogUrl,
    twitterUrl = this.twitterUrl,
    company = this.company.getOrElse(""))
}
object RivieraDevSpeaker {
  implicit val format = Json.format[RivieraDevSpeaker]

  def linkList(listPage: String, url: String): List[String] = {
    val baseUrl = url.replace("/orateurs", "")
    Jsoup.parse(listPage).select(".container .lines tbody tr td:nth-child(2) a").map(baseUrl+_.attr("href")).toList
  }

  def fromHTML(detailPage: String, url: String) = {
    val baseUrl = url.split("/orateur").headOption.getOrElse("")
    val doc = Jsoup.parse(detailPage).select(".details")
    val name = doc.select("td:nth-child(1) .photo-label").text()
    val details = doc.select("td:nth-child(2)")
    val detailBlocks = details.html().split("<br>").map(Jsoup.parse).map(block => {
      (block.select(".label").text(), block)
    }).toMap
    RivieraDevSpeaker(
      source = Source(url, RivieraDevScraper.name, url),
      avatar = baseUrl+doc.select("td:nth-child(1) img").attr("src"),
      firstName = name.split(" ").headOption.getOrElse(""),
      lastName = name.split(" ").drop(1).mkString(" "),
      title = detailBlocks.get("Titre:").map(_.text().replace("Titre:", "").trim).getOrElse(""),
      description = details.select("p").map(_.text()).mkString(" "),
      descriptionHTML = details.select("p").map("<p>"+_.html+"</p>").mkString,
      company = detailBlocks.get("Compagnie:").map(_.select("a").text()),
      companyUrl = detailBlocks.get("Compagnie:").map(_.select("a").attr("href")),
      twitterUrl = detailBlocks.get("Twitter:").map(_.select("a").attr("href")),
      blogUrl = detailBlocks.get("Blog:").map(_.select("a").attr("href")),
      sessions = details.select("li a").map(baseUrl+_.attr("href")).toList)
  }
}