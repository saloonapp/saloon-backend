package common.repositories.event

import common.models.event.GenericEvent
import common.repositories.CollectionReferences
import common.repositories.utils.MongoDbCrudUtils
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import reactivemongo.api.DB
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.commands.MultiBulkWriteResult
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.JsObjectDocumentWriter
import play.modules.reactivemongo.json.collection.JSONCollection

trait MongoDbGenericEventRepository {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.GENERICEVENTS)

  def findAll(): Future[List[GenericEvent]] = collection.find(Json.obj()).sort(Json.obj("info.start" -> 1)).cursor[GenericEvent].collect[List]()
  def getByUuid(eventId: String): Future[Option[GenericEvent]] = collection.find(Json.obj("uuid" -> eventId)).one[GenericEvent]
  def findBySourceName(sourceName: String): Future[List[GenericEvent]] = collection.find(Json.obj("sources.name" -> sourceName)).cursor[GenericEvent].collect[List]()

  def bulkInsert(elts: List[GenericEvent]): Future[MultiBulkWriteResult] = MongoDbCrudUtils.bulkInsert(elts, collection)
  def bulkUpdate(elts: List[(String, GenericEvent)]): Future[Int] = MongoDbCrudUtils.bulkUpdate(elts.map(p => (p._1, p._2)), collection)
  def bulkDelete(eventIds: List[String]): Future[WriteResult] = MongoDbCrudUtils.bulkDelete(eventIds, collection)
}
object GenericEventRepository extends MongoDbGenericEventRepository
