package tools.api.devfesttoulouse.models

import play.api.libs.json.Json

// https://devfesttoulouse.fr/data/sessions.json
case class DevFestSession(
  id: Int,
  title: String,
  description: String,
  language: Option[String],
  complexity: Option[String],
  presentation: Option[String],
  videoId: Option[String],
  track: Option[DevFestTrack],
  speakers: Option[List[Int]],
  tags: Option[List[String]],
  sourceUrl: Option[String])
object DevFestSession {
  implicit val format = Json.format[DevFestSession]
}
