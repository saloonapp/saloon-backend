package tools.scrapers.lanyrd.models

import tools.utils.CsvElt
import tools.utils.CsvUtils
import play.api.libs.json.Json
import org.joda.time.DateTime

case class LanyrdEvent(
  name: String) extends CsvElt {
  def toCsv(): Map[String, String] = LanyrdEvent.toCsv(this)
}
object LanyrdEvent {
  implicit val format = Json.format[LanyrdEvent]
  def toCsv(e: LanyrdEvent): Map[String, String] = CsvUtils.jsonToCsv(Json.toJson(e), 4)
}
