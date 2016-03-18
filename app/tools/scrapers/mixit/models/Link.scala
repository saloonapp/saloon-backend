package tools.scrapers.mixit.models

import play.api.libs.json.Json

case class Link(
                 rel: String,
                 href: String
               )
object Link {
  implicit val format = Json.format[Link]
}
