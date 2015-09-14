package tools.scrapers.foiresetsalons.models

import tools.utils.CsvElt
import tools.utils.CsvUtils
import play.api.libs.json.Json

case class FoiresEtSalonsEvent(
  name: String) extends CsvElt {
  def toCsv(): Map[String, String] = FoiresEtSalonsEvent.toCsv(this)
}
object FoiresEtSalonsEvent {
  implicit val format = Json.format[FoiresEtSalonsEvent]
  def toCsv(e: FoiresEtSalonsEvent): Map[String, String] = CsvUtils.jsonToCsv(Json.toJson(e), 4)
}
