package backend.forms

import common.models.utils.tStringConstraints._
import common.models.values.typed.WebsiteUrl
import common.models.values.UUID
import backend.models.Scraper
import play.api.data.Forms._
import org.joda.time.DateTime

case class ScraperData(
  name: String,
  url: WebsiteUrl)
object ScraperData {
  val fields = mapping(
    "name" -> nonEmptyText,
    "url" -> of[WebsiteUrl].verifying(nonEmpty))(ScraperData.apply)(ScraperData.unapply)

  def toModel(d: ScraperData): Scraper = Scraper(UUID.generate(), d.name, d.url, None)
  def fromModel(d: Scraper): ScraperData = ScraperData(d.name, d.url)
  def merge(s: Scraper, d: ScraperData): Scraper = s.copy(name = d.name, url = d.url)
}