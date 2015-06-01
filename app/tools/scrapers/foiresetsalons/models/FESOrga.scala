package tools.scrapers.foiresetsalons.models

import play.api.libs.json.Json

case class FESOrga(
  name: String,
  sigle: String,
  address: String,
  phone: String,
  site: String)
object FESOrga {
  implicit val format = Json.format[FESOrga]
}
