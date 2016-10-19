package tools.repositories

import tools.models.WebpageCache
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import reactivemongo.api.commands.UpdateWriteResult
import play.modules.reactivemongo.json.JsObjectDocumentWriter
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin
import org.joda.time.DateTime

object WebpageCacheRepository {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection]("tmpWebpageCache")

  def get(url: String, since: DateTime): Future[Option[WebpageCache]] = collection.find(Json.obj("url" -> url, "cached" -> Json.obj("$gte" -> since))).one[WebpageCache]
  def set(url: String, page: String): Future[UpdateWriteResult] = collection.update(Json.obj("url" -> url), Json.toJson(WebpageCache(url, page)).as[JsObject], upsert = true)
}