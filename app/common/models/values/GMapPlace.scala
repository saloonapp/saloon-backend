package common.models.values

import play.api.data.Forms._
import play.api.libs.json.Json

case class GMapPlace(
  id: String,
  name: String,
  streetNo: Option[String],
  street: Option[String],
  postalCode: Option[String],
  locality: Option[String],
  country: String,
  formatted: String,
  input: String,
  geo: Geo,
  url: String,
  website: Option[String],
  phone: Option[String])
object GMapPlace {
  implicit val format = Json.format[GMapPlace]
  val fields = mapping(
    "id" -> nonEmptyText,
    "name" -> nonEmptyText,
    "streetNo" -> optional(nonEmptyText),
    "street" -> optional(nonEmptyText),
    "postalCode" -> optional(nonEmptyText),
    "locality" -> optional(nonEmptyText),
    "country" -> nonEmptyText,
    "formatted" -> nonEmptyText,
    "input" -> nonEmptyText,
    "geo" -> Geo.fields,
    "url" -> nonEmptyText,
    "website" -> optional(nonEmptyText),
    "phone" -> optional(nonEmptyText)
  )(GMapPlace.apply)(GMapPlace.unapply)
}