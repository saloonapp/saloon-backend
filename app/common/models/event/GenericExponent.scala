package common.models.event

import common.models.values.Source
import play.api.libs.json.Json

case class GenericExponent(
  source: Source,
  name: String,
  description: String,
  descriptionHTML: String,
  logo: String,
  website: String,
  place: String)
object GenericExponent {
  implicit val format = Json.format[GenericExponent]
}