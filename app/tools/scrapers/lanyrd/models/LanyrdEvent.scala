package tools.scrapers.lanyrd.models

import tools.utils.CsvElt
import tools.utils.CsvUtils
import play.api.libs.json.Json
import org.joda.time.DateTime

case class LanyrdAddress(
  name: String,
  street: String,
  city: String,
  country: String,
  url: String,
  gmap: String)
object LanyrdAddress {
  implicit val format = Json.format[LanyrdAddress]
}
case class LanyrdEvent(
  name: String,
  descriptionHTML: String,
  start: Option[DateTime],
  end: Option[DateTime],
  websiteUrl: String,
  scheduleUrl: String,
  address: LanyrdAddress,
  twitterAccountUrl: String,
  twitterHashtagUrl: String,
  tags: List[String],
  url: String) extends CsvElt {
  def toCsv(): Map[String, String] = LanyrdEvent.toCsv(this)
}
object LanyrdEvent {
  implicit val format = Json.format[LanyrdEvent]
  def toCsv(e: LanyrdEvent): Map[String, String] = CsvUtils.jsonToCsv(Json.toJson(e), 4)
}
