package tools.scrapers.lanyrd.models

import play.api.libs.json.Json

case class LanyrdVenue(
  name: String,
  address: String,
  url: String,
  map: String)
object LanyrdVenue {
  implicit val format = Json.format[LanyrdVenue]
}
