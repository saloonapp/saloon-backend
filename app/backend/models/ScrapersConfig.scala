package backend.models

import common.models.values.typed.WebsiteUrl
import play.api.libs.json.Json
import org.joda.time.DateTime

case class ScraperResult(
  date: DateTime,
  nbElts: Int)
object ScraperResult {
  implicit val format = Json.format[ScraperResult]
}
case class Scraper(
  uuid: String,
  name: String,
  url: WebsiteUrl,
  lastExec: Option[ScraperResult])
object Scraper {
  implicit val format = Json.format[Scraper]
}
case class ScrapersConfig(
  scrapers: List[Scraper] = List(),
  scrapersConfig: Boolean = true)
object ScrapersConfig {
  implicit val format = Json.format[ScrapersConfig]
}
