package tools.models

import play.api.libs.json.Json

case class GenericExponent(
  source: Source,
  name: String)
object GenericExponent {
  implicit val format = Json.format[GenericExponent]
}