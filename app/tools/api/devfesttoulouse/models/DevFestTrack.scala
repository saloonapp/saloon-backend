package tools.api.devfesttoulouse.models

import play.api.libs.json.Json

case class DevFestTrack(
  title: String)
object DevFestTrack {
  implicit val format = Json.format[DevFestTrack]
}
