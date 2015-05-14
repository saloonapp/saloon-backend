package infrastructure.repository

import infrastructure.repository.common.Repository
import infrastructure.repository.common.MongoDbCrudUtils
import models.common.Page
import models.UserAction
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import reactivemongo.api.DB
import reactivemongo.core.commands.LastError
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

trait MongoDbUserActionRepository extends Repository[UserAction] {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.USERACTIONS)

  private val crud = MongoDbCrudUtils(collection, UserAction.format, List("action.text"), "uuid")

  override def findAll(query: String = "", sort: String = ""): Future[List[UserAction]] = crud.findAll(query, sort)
  override def findPage(query: String = "", page: Int = 1, sort: String = ""): Future[Page[UserAction]] = crud.findPage(query, page, sort)
  override def getByUuid(uuid: String): Future[Option[UserAction]] = crud.getByUuid(uuid)
  override def insert(elt: UserAction): Future[Option[UserAction]] = { crud.insert(elt).map(err => if (err.ok) Some(elt) else None) }
  override def update(uuid: String, elt: UserAction): Future[Option[UserAction]] = crud.update(uuid, elt).map(err => if (err.ok) Some(elt) else None)
  override def delete(uuid: String): Future[Option[UserAction]] = crud.delete(uuid).map(err => None) // TODO : return deleted elt !

  def findByItem(itemType: String, itemId: String): Future[List[UserAction]] = crud.find(Json.obj("itemType" -> itemType, "itemId" -> itemId))
  def findByUser(userId: String): Future[List[UserAction]] = crud.find(Json.obj("userId" -> userId))
  def findByUserEvent(userId: String, eventId: String): Future[List[UserAction]] = crud.find(Json.obj("userId" -> userId, "eventId" -> eventId))
  def getFavorite(userId: String, itemType: String, itemId: String): Future[Option[UserAction]] = crud.get(Json.obj("userId" -> userId, "actionType" -> UserAction.favorite, "itemType" -> itemType, "itemId" -> itemId))
  def deleteFavorite(userId: String, itemType: String, itemId: String): Future[LastError] = crud.delete(Json.obj("userId" -> userId, "actionType" -> UserAction.favorite, "itemType" -> itemType, "itemId" -> itemId))
}
object UserActionRepository extends MongoDbUserActionRepository
