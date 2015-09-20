package backend.models

import common.models.values.typed.WebsiteUrl
import play.api.libs.json.Json
import org.joda.time.DateTime

case class Scraper(
  uuid: String,
  name: String,
  url: WebsiteUrl,
  lastExec: Option[DateTime])
object Scraper {
  implicit val format = Json.format[Scraper]
}
case class ScrapersConfig(
  scrapers: List[Scraper] = List(),
  scrapersConfig: Boolean = true)
object ScrapersConfig {
  implicit val format = Json.format[ScrapersConfig]
}
