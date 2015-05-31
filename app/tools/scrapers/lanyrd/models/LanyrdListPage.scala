package tools.scrapers.lanyrd.models

import play.api.libs.json.Json

case class LanyrdListPage(
  urlSource: String,
  currentPage: Int,
  maxPages: Int,
  events: List[LanyrdEvent],
  loadedPages: List[Int])
object LanyrdListPage {
  implicit val format = Json.format[LanyrdListPage]
}
