package tools.scrapers.salonreunir.models

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
  url: String)
object SalonReunirSession {
  implicit val format = Json.format[SalonReunirSession]
}
