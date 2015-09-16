package tools.scrapers.salonreunir.models

import tools.utils.CsvElt
import tools.utils.CsvUtils
import play.api.libs.json.Json
import org.joda.time.DateTime

case class SalonReunirSession(
  ref: String,
  start: DateTime,
  end: DateTime,
  name: String,
  animator: String,
  format: String,
  place: String,
  url: String) extends CsvElt {
  def toCsv(): Map[String, String] = SalonReunirSession.toCsv(this)
}
object SalonReunirSession {
  implicit val format = Json.format[SalonReunirSession]
  def toCsv(e: SalonReunirSession): Map[String, String] = CsvUtils.jsonToCsv(Json.toJson(e), 4)
}
