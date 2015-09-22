package tools.scrapers.salonreunir.models

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
  url: String)
object SalonReunirExponent {
  implicit val format = Json.format[SalonReunirExponent]
}
