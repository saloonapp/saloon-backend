package tools.api.devfesttoulouse.models

import play.api.libs.json.Json

// https://devfesttoulouse.fr/data/schedule.json
case class DevFestSchedule(
  date: String,
  dateReadable: String,
  tracks: List[DevFestTrack],
  timeslots: List[DevFestSlot],
  sourceUrl: Option[String])
object DevFestSchedule {
  implicit val format = Json.format[DevFestSchedule]
}
