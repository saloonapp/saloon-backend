package tools.scrapers.blendwebmix.models

import org.jsoup.Jsoup
import scala.collection.JavaConversions._
import play.api.libs.json.Json

case class BlendWebMixSpeaker(
                               firstName: String,
                               lastName: String,
                               avatar: String,
                               job: Option[String],
                               company: Option[String],
                               description: String,
                               descriptionHTML: String,
                               siteUrl: Option[String],
                               twitterUrl: Option[String],
                               linkedinUrl: Option[String],
                               url: String
                             )
object BlendWebMixSpeaker {
  def extractLinks(html: String): List[(String, String)] = {
    val doc = Jsoup.parse(html)
    doc.select(".team-title a").toList.map(a => (a.attr("href"), a.select("span").text))
  }
  def extract(url: String, job: Option[String], html: String): BlendWebMixSpeaker = {
    val doc = Jsoup.parse(html)
    val content = doc.select(".portfolio-single-content")
    val name = content.select("h2").text()
    val descriptionHTML = content.select("> *").filterNot { elt =>
      elt.attr("class").contains("fancy-title") || elt.attr("class").contains("line") || elt.attr("class").contains("portfolio-meta")
    }.map("<p>"+_.html()+"</p>").mkString("")
    val meta = doc.select(".portfolio-meta li").map { elt =>
      (elt.select("i").attr("class"), elt.select("a").attr("href"))
    }
    BlendWebMixSpeaker(
      firstName = name.split(" ").headOption.getOrElse(""),
      lastName = name.split(" ").drop(1).mkString(" "),
      avatar = doc.select(".portfolio-single-image img").attr("src"),
      job = job.map(_.split(",").dropRight(1).mkString(",").trim),
      company = job.map(_.split(",").takeRight(1).mkString(",").trim),
      description = Jsoup.parse(descriptionHTML).text(),
      descriptionHTML = descriptionHTML,
      siteUrl = meta.find(_._1 == "icon-link").map(_._2),
      twitterUrl = meta.find(_._1 == "icon-twitter").map(_._2),
      linkedinUrl = meta.find(_._1 == "icon-linkedin").map(_._2),
      url = url
    )
  }
  implicit val format = Json.format[BlendWebMixSpeaker]
}
