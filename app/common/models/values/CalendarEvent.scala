package common.models.values

import org.joda.time.LocalDate
import play.api.libs.json._

case class CalendarEvent(
  title: String,
  start: LocalDate,
  end: LocalDate,
  url: String)
object CalendarEvent {
  private val defaultFormat = Json.format[CalendarEvent]
  implicit val format = Format(new Reads[CalendarEvent] {
    override def reads(json: JsValue): JsResult[CalendarEvent] = defaultFormat.reads(json)
  }, new Writes[CalendarEvent] {
    override def writes(value: CalendarEvent): JsValue = Json.obj(
      "title" -> value.title,
      "start" -> value.start.toString("yyyy-MM-dd"),
      "end" -> value.end.plusDays(1).toString("yyyy-MM-dd"),
      "url" -> value.url)
  })
}
