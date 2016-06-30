package tools.scrapers.twitter.models

import org.jsoup.Jsoup
import play.api.libs.json.Json
import scala.util.Try

case class TwitterProfil(
  name: String,
  account: String,
  avatar: String,
  backgroundImage: String,
  bio: String,
  location: String,
  url: String,
  tweets: Int,
  following: Int,
  followers: Int,
  favorites: Int)
object TwitterProfil {
  implicit val format = Json.format[TwitterProfil]

  def fromHTML(html: String, url: String): TwitterProfil = {
    val doc = Jsoup.parse(html)
    TwitterProfil(
      name = doc.select(".ProfileHeaderCard-name a").text(),
      account = doc.select(".ProfileHeaderCard-screenname a span").text(),
      avatar = doc.select(".ProfileAvatar-image").attr("src"),
      backgroundImage = doc.select(".ProfileCanopy-headerBg img").attr("src"),
      bio = doc.select(".ProfileHeaderCard-bio").text(),
      location = doc.select(".ProfileHeaderCard-locationText").text(),
      url = doc.select(".ProfileHeaderCard-urlText a").attr("title"),
      tweets = safeInt(doc.select(".ProfileNav-item--tweets a").attr("title")).getOrElse(-1),
      following = safeInt(doc.select(".ProfileNav-item--following a").attr("title")).getOrElse(-1),
      followers = safeInt(doc.select(".ProfileNav-item--followers a").attr("title")).getOrElse(-1),
      favorites = safeInt(doc.select(".ProfileNav-item--favorites a").attr("title")).getOrElse(-1)
    )
  }

  def safeInt(str: String): Option[Int] = Try(str.replaceAll("[^\\d.]", "").toInt).toOption
}
