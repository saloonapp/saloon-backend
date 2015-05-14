package infrastructure.repository

import models.UserFav
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import reactivemongo.api.DB
import reactivemongo.core.commands.LastError
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

trait MongoDbUserFavRepository {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.USERFAVS)

  def insert(elt: UserFav): Future[LastError] = collection.update(Json.obj("elt" -> elt.elt, "eltId" -> elt.eltId, "userId" -> elt.userId), elt, upsert = true)
  def get(elt: String, eltId: String, userId: String): Future[Option[UserFav]] = collection.find(Json.obj("elt" -> elt, "eltId" -> eltId, "userId" -> userId)).one[UserFav]
  def findByType(elt: String): Future[List[UserFav]] = collection.find(Json.obj("elt" -> elt)).cursor[UserFav].collect[List]()
  def findByElt(elt: String, eltId: String): Future[List[UserFav]] = collection.find(Json.obj("elt" -> elt, "eltId" -> eltId)).cursor[UserFav].collect[List]()
  def findByEvent(eventId: String): Future[List[UserFav]] = collection.find(Json.obj("eventId" -> eventId)).cursor[UserFav].collect[List]()
  def findByUser(userId: String): Future[List[UserFav]] = collection.find(Json.obj("userId" -> userId)).cursor[UserFav].collect[List]()
  def findByEventUser(eventId: String, userId: String): Future[List[UserFav]] = collection.find(Json.obj("eventId" -> eventId, "userId" -> userId)).cursor[UserFav].collect[List]()
  def delete(elt: UserFav): Future[LastError] = collection.remove(Json.obj("elt" -> elt.elt, "eltId" -> elt.eltId, "userId" -> elt.userId))
  def delete(elt: String, eltId: String, userId: String): Future[LastError] = collection.remove(Json.obj("elt" -> elt, "eltId" -> eltId, "userId" -> userId))
}
object UserFavRepository extends MongoDbUserFavRepository
