package common.models.values

import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.libs.json.Json

case class Geo(
  lat: Double,
  lng: Double)
object Geo {
  implicit val format = Json.format[Geo]
  val fields = mapping(
    "lat" -> of(doubleFormat),
    "lng" -> of(doubleFormat)
  )(Geo.apply)(Geo.unapply)
}
