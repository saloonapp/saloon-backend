package tools.scrapers.foiresetsalons.models

import org.joda.time.DateTime
import play.api.libs.json.Json

case class FESEventItem(
  url: String,
  name: String,
  city: String,
  start: String,
  end: String,
  sectors: List[String]) {
  def toMap(): Map[String, String] = {
    Map(
      "url" -> this.url,
      "name" -> this.name,
      "city" -> this.city,
      "start" -> this.start,
      "end" -> this.end,
      "sectors" -> this.sectors.mkString(", "))
  }
}
object FESEventItem {
  implicit val format = Json.format[FESEventItem]
}