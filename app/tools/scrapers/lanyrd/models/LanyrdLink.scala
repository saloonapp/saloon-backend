package tools.scrapers.lanyrd.models

import play.api.libs.json.Json

case class LanyrdLink(name: String, url: String)
object LanyrdLink {
  implicit val format = Json.format[LanyrdLink]
}
