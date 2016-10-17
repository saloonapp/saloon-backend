package tools.api.devfesttoulouse.models

import play.api.libs.json.Json

// from https://devfesttoulouse.fr/data/speakers.json
case class DevFestSpeaker(
  id: Int,
  featured: Boolean,
  name: String,
  title: String,
  company: String,
  country: String,
  photoUrl: String,
  bio: String,
  sourceUrl: Option[String])
object DevFestSpeaker {
  implicit val format = Json.format[DevFestSpeaker]
}