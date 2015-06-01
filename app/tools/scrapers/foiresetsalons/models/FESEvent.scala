package tools.scrapers.foiresetsalons.models

import play.api.libs.json.Json

case class FESEvent(
  url: String,
  name: String,
  address: String,
  categorie: String,
  access: List[String],
  start: String,
  end: String,
  sectors: List[String],
  products: List[String],
  stats: FESStats,
  orga: FESOrga) {
  def toMap(): Map[String, String] = {
    Map(
      "url" -> this.url,
      "name" -> this.name,
      "address" -> this.address,
      "categorie" -> this.categorie,
      "access" -> this.access.mkString(", "),
      "start" -> this.start,
      "end" -> this.end,
      "sectors" -> this.sectors.mkString(", "),
      "products" -> this.products.mkString(", "),
      "area" -> this.stats.area.toString,
      "exponents" -> this.stats.exponents.toString,
      "visitors" -> this.stats.visitors.toString,
      "venues" -> this.stats.venues.toString,
      "certified" -> this.stats.certified,
      "orga.name" -> this.orga.name,
      "orga.address" -> this.orga.address,
      "orga.phone" -> this.orga.phone,
      "orga.site" -> this.orga.site)
  }
}
object FESEvent {
  implicit val format = Json.format[FESEvent]
}
