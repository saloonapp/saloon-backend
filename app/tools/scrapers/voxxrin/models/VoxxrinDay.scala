package tools.scrapers.voxxrin.models

import play.api.libs.json.Json

case class VoxxrinDay(
  id: String,
  schedule: List[VoxxrinSession],
  dayNumber: Int,
  lastmodified: Long)
object VoxxrinDay {
  implicit val format = Json.format[VoxxrinDay]
}
