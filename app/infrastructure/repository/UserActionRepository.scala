package infrastructure.repository

import infrastructure.repository.common.Repository
import infrastructure.repository.common.MongoDbCrudUtils
import models.common.Page
import models.UserAction
import models.MoodUserAction
import models.CommentUserAction
import org.joda.time.DateTime
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import reactivemongo.api.DB
import reactivemongo.core.commands.LastError
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin

trait MongoDbUserActionRepository {
  val db = ReactiveMongoPlugin.db
  lazy val collection: JSONCollection = db[JSONCollection](CollectionReferences.USERACTIONS)

  private val crud = MongoDbCrudUtils(collection, UserAction.format, List("action.text"), "uuid")

  def findByUser(userId: String): Future[List[UserAction]] = crud.find(Json.obj("userId" -> userId))
  def findByUserEvent(userId: String, eventId: String): Future[List[UserAction]] = crud.find(Json.obj("userId" -> userId, "eventId" -> eventId))

  def getFavorite(userId: String, itemType: String, itemId: String): Future[Option[UserAction]] = crud.get(Json.obj("userId" -> userId, "action.favorite" -> true, "itemType" -> itemType, "itemId" -> itemId))
  def insertFavorite(userId: String, itemType: String, itemId: String, eventId: String, time: Option[DateTime] = None): Future[Option[UserAction]] = {
    val elt = UserAction.favorite(userId, itemType, itemId, eventId, time)
    crud.insert(elt).map { err => if (err.ok) Some(elt) else None }
  }
  def deleteFavorite(userId: String, itemType: String, itemId: String): Future[LastError] = crud.delete(Json.obj("userId" -> userId, "action.favorite" -> true, "itemType" -> itemType, "itemId" -> itemId))

  def getMood(userId: String, itemType: String, itemId: String): Future[Option[UserAction]] = crud.get(Json.obj("userId" -> userId, "action.mood" -> true, "itemType" -> itemType, "itemId" -> itemId))
  def setMood(userId: String, itemType: String, itemId: String, eventId: String, oldElt: Option[UserAction], rating: String, time: Option[DateTime] = None): Future[Option[UserAction]] = {
    val elt = oldElt.map(e => e.withContent(MoodUserAction(rating), time)).getOrElse(UserAction.mood(userId, itemType, itemId, rating, eventId, time))
    crud.upsert(Json.obj("userId" -> userId, "action.mood" -> true, "itemType" -> itemType, "itemId" -> itemId, "uuid" -> elt.uuid), elt).map { err => if (err.ok) Some(elt) else None }
  }
  def deleteMood(userId: String, itemType: String, itemId: String): Future[LastError] = crud.delete(Json.obj("userId" -> userId, "action.mood" -> true, "itemType" -> itemType, "itemId" -> itemId))

  def getComment(userId: String, itemType: String, itemId: String, uuid: String): Future[Option[UserAction]] = crud.get(Json.obj("userId" -> userId, "action.comment" -> true, "itemType" -> itemType, "itemId" -> itemId, "uuid" -> uuid))
  def insertComment(userId: String, itemType: String, itemId: String, text: String, eventId: String, time: Option[DateTime] = None): Future[Option[UserAction]] = {
    val elt = UserAction.comment(userId, itemType, itemId, text, eventId, time)
    crud.insert(elt).map { err => if (err.ok) Some(elt) else None }
  }
  def updateComment(userId: String, itemType: String, itemId: String, uuid: String, oldElt: UserAction, text: String, time: Option[DateTime] = None): Future[Option[UserAction]] = {
    val elt = oldElt.withContent(CommentUserAction(text), time)
    crud.update(Json.obj("userId" -> userId, "action.comment" -> true, "itemType" -> itemType, "itemId" -> itemId, "uuid" -> uuid), elt).map { err => if (err.ok) Some(elt) else None }
  }
  def deleteComment(userId: String, itemType: String, itemId: String, uuid: String): Future[LastError] = crud.delete(Json.obj("userId" -> userId, "action.comment" -> true, "itemType" -> itemType, "itemId" -> itemId, "uuid" -> uuid))
}
object UserActionRepository extends MongoDbUserActionRepository
