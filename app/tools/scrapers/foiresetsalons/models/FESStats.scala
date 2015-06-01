package tools.scrapers.foiresetsalons.models

import play.api.libs.json.Json

case class FESStats(
  area: Int,
  exponents: Int,
  visitors: Int,
  venues: Int,
  certified: String)
object FESStats {
  implicit val format = Json.format[FESStats]
}
