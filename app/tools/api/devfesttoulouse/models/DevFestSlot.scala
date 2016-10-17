package tools.api.devfesttoulouse.models

import play.api.libs.json.Json

case class DevFestSlot(
  startTime: String,
  endTime: String,
  sessions: List[List[Int]])
object DevFestSlot {
  implicit val format = Json.format[DevFestSlot]
}
