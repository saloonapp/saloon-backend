package common.models.values

import play.api.libs.json.Json

case class GMapMarker(
  title: String,
  date: String,
  location: String,
  lat: Double,
  lng: Double,
  url: String)
object GMapMarker {
  implicit val format = Json.format[GMapMarker]
}
