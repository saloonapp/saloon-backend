package tools.repositories

import tools.models.WebpageCache
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import reactivemongo.core.commands.LastError
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

object WebpageCacheRepository {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection]("tmpWebpageCache")

  def get(url: String): Future[Option[WebpageCache]] = collection.find(Json.obj("url" -> url)).one[WebpageCache]
  def set(url: String, page: String): Future[LastError] = collection.update(Json.obj("url" -> url), Json.toJson(WebpageCache(url, page)), upsert = true)
}