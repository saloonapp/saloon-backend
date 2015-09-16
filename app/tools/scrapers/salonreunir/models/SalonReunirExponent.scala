package tools.scrapers.salonreunir.models

import tools.utils.CsvElt
import tools.utils.CsvUtils
import play.api.libs.json.Json

case class SalonReunirExponent(
  ref: String,
  name: String,
  place: String,
  address: String,
  contactName: String,
  contactPhone: String,
  contactEmail: String,
  descriptionHTML: String,
  url: String) extends CsvElt {
  def toCsv(): Map[String, String] = SalonReunirExponent.toCsv(this)
}
object SalonReunirExponent {
  implicit val format = Json.format[SalonReunirExponent]
  def toCsv(e: SalonReunirExponent): Map[String, String] = CsvUtils.jsonToCsv(Json.toJson(e), 4)
}
