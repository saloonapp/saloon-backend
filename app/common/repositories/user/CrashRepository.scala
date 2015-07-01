package common.repositories.user

import common.repositories.Repository
import common.repositories.CollectionReferences
import org.joda.time.DateTime
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.iteratee.Enumerator
import reactivemongo.api.DB
import reactivemongo.core.commands.LastError
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

trait MongoDbCrashRepository {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.CRASHS)

  def find(filter: JsValue = Json.obj()): Future[List[JsValue]] = collection.find(filter).sort(Json.obj("created" -> -1)).cursor[JsValue].collect[List]().map(_.map(removeOid))
  def get(uuid: String): Future[Option[JsValue]] = collection.find(Json.obj("uuid" -> uuid)).one[JsValue].map(_.map(removeOid))
  def get(filter: JsValue): Future[Option[JsValue]] = collection.find(filter).one[JsValue].map(_.map(removeOid))
  def insert(elt: JsValue): Future[LastError] = collection.save(addMeta(elt))
  def bulkInsert(elts: List[JsValue]): Future[Int] = collection.bulkInsert(Enumerator.enumerate(elts.map(addMeta)))
  def markAsSolved(filter: JsValue): Future[LastError] = collection.update(filter, Json.obj("$set" -> Json.obj("solved" -> true)), multi = true)
  def delete(uuid: String): Future[LastError] = collection.remove(Json.obj("uuid" -> uuid))

  private def addMeta(elt: JsValue): JsValue = elt.as[JsObject] ++ Json.obj("uuid" -> Repository.generateUuid(), "created" -> new DateTime())
  private def removeOid(elt: JsValue): JsValue = elt.as[JsObject] - "_id"
}
object CrashRepository extends MongoDbCrashRepository
