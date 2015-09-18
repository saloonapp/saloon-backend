package tools.models

import play.api.libs.json.Json
import org.joda.time.DateTime

case class WebpageCache(
  url: String,
  page: String,
  cached: DateTime = new DateTime())
object WebpageCache {
  implicit val format = Json.format[WebpageCache]
}
