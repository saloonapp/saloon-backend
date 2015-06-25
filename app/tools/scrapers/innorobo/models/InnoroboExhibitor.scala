package tools.scrapers.innorobo.models

import play.api.libs.json.Json

case class InnoroboExhibitor(
  name: String,
  description: String,
  logo: String,
  website: String,
  url: String) {
  def toMap(): Map[String, String] = Map(
    "name" -> this.name,
    "description" -> this.description,
    "images.logo" -> this.logo,
    "images.landing" -> this.logo,
    "info.website" -> this.website,
    "meta.source.ref" -> this.url,
    "meta.source.name" -> "Innorobo website",
    "meta.source.url" -> this.url)
}
object InnoroboExhibitor {
  implicit val format = Json.format[InnoroboExhibitor]
}
