package tools.scrapers.foiresetsalons.models

import tools.utils.CsvElt
import tools.utils.CsvUtils
import play.api.libs.json.Json
import org.joda.time.DateTime

case class FoiresEtSalonsAddress(
  name: String,
  street: String,
  city: String)
object FoiresEtSalonsAddress {
  implicit val format = Json.format[FoiresEtSalonsAddress]
  def build(list: List[String]): FoiresEtSalonsAddress =
    if (list.length == 0) FoiresEtSalonsAddress("", "", "")
    else if (list.length == 1) FoiresEtSalonsAddress("", "", list(0))
    else if (list.length == 2) FoiresEtSalonsAddress("", list(0), list(1))
    else FoiresEtSalonsAddress(list(0), list(1), list(2))
}
case class FoiresEtSalonsStats(
  area: Int,
  venues: Int,
  exponents: Int,
  visitors: Int,
  certified: String)
object FoiresEtSalonsStats {
  implicit val format = Json.format[FoiresEtSalonsStats]
}
case class FoiresEtSalonsOrga(
  name: String,
  sigle: String,
  address: FoiresEtSalonsAddress,
  phone: String,
  site: String)
object FoiresEtSalonsOrga {
  implicit val format = Json.format[FoiresEtSalonsOrga]
}
case class FoiresEtSalonsEvent(
  name: String,
  address: FoiresEtSalonsAddress,
  category: String,
  access: List[String],
  start: Option[DateTime],
  end: Option[DateTime],
  sectors: List[String],
  products: List[String],
  stats: FoiresEtSalonsStats,
  orga: FoiresEtSalonsOrga,
  url: String) extends CsvElt {
  def toCsv(): Map[String, String] = FoiresEtSalonsEvent.toCsv(this)
}
object FoiresEtSalonsEvent {
  implicit val format = Json.format[FoiresEtSalonsEvent]
  def toCsv(e: FoiresEtSalonsEvent): Map[String, String] = CsvUtils.jsonToCsv(Json.toJson(e), 4)
}
