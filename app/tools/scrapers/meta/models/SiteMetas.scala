package tools.scrapers.meta.models

import play.api.libs.json.Json

case class SiteMetas(
  urlSource: String,
  title: List[String],
  description: List[String],
  image: List[String],
  keywords: List[String],
  url: List[String],
  color: List[String],
  social: Map[String, List[String]],
  all: Map[String, List[String]])
object SiteMetas {
  implicit val format = Json.format[SiteMetas]

  def build(urlSource: String, all: Map[String, List[String]]): SiteMetas = {
    SiteMetas(
      urlSource,
      merge(all, "og:title", "og:site_name", "twitter:title", "application-name", "title"),
      merge(all, "og:description", "twitter:description", "description"),
      merge(all, "og:image", "icon"),
      merge(all, "keywords").map(_.split(",").map(_.trim)).flatten,
      merge(all, "og:url", "twitter:url", "url"),
      merge(all, "theme-color", "msapplication-TileColor"),
      Map("twitter" -> merge(all, "twitter:site")),
      all)
  }

  private def merge(all: Map[String, List[String]], attrs: String*): List[String] = {
    attrs.toList.map { attr => all.get(attr) }.flatten.flatten.distinct.filter(_ != "")
  }
}
