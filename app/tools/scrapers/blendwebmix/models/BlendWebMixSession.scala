package tools.scrapers.blendwebmix.models

import java.util.Locale

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json

import scala.collection.JavaConversions._
import org.jsoup.Jsoup

import scala.util.Try

case class BlendWebMixSession(
                               name: String,
                               description: String,
                               descriptionHTML: String,
                               level: String,
                               theme: String,
                               place: String,
                               start: Option[DateTime],
                               end: Option[DateTime],
                               speakers: List[String],
                               url: String
                             )
object BlendWebMixSession {
  def extractLinks(html: String): List[(String, String)] = {
    val doc = Jsoup.parse(html)
    doc.select("a.link_title").toList.map(a => (a.attr("href"), a.text))
  }

  def extract(url: String, name: String, html: String): BlendWebMixSession = {
    val doc = Jsoup.parse(html)
    val metas = doc.select(".events-meta li").map { elt =>
      (elt.select("i").attr("class"), elt.text(), elt.select("a").map(_.attr("href")))
    }.toList
    val day = metas.find(_._1 == "icon-calendar3").map(_._2).getOrElse("")
    val date = Try(DateTime.parse(day, DateTimeFormat.forPattern("dd MMMM yyyy").withLocale(Locale.FRANCE))).toOption
    val hours = metas.find(_._1 == "icon-time").map(_._2).getOrElse("")
    val hourRegex = "Horaire: ([0-9]{2})h([0-9]{2}) / ([0-9]{2})h([0-9]{2})".r
    val (start, end) = hours match {
      case hourRegex(startHour, startMin, endHour, endMin) => (
        date.map(_.withHourOfDay(startHour.toInt).withMinuteOfHour(startMin.toInt)),
        date.map(_.withHourOfDay(endHour.toInt).withMinuteOfHour(endMin.toInt))
        )
      case _ => (None, None)
    }
    BlendWebMixSession(
      name = name,
      description = doc.select(".postcontent p").map(_.text()).mkString("\n\r"),
      descriptionHTML = doc.select(".postcontent p").map("<p>"+_.html()+"</p>").mkString(""),
      level = metas.find(_._1 == "icon-study").map(_._2).getOrElse(""),
      theme = doc.select(".cat-icon").text(),
      place = metas.find(_._1 == "icon-map-marker2").map(_._2).getOrElse(""),
      start = start,
      end = end,
      speakers = metas.filter(_._1 == "icon-user").flatMap(_._3),
      url = url
    )
  }
  implicit val format = Json.format[BlendWebMixSession]
}
