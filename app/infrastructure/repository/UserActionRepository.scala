package infrastructure.repository

import common.models.Page
import common.infrastructure.repository.Repository
import common.infrastructure.repository.MongoDbCrudUtils
import models.UserAction
import models.FavoriteUserAction
import models.DoneUserAction
import models.MoodUserAction
import models.CommentUserAction
import models.SubscribeUserAction
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

  def getFavorite(userId: String, itemType: String, itemId: String): Future[Option[UserAction]] = getAction(FavoriteUserAction.className)(userId, itemType, itemId)
  def insertFavorite(userId: String, itemType: String, itemId: String, eventId: String, time: Option[DateTime] = None): Future[Option[UserAction]] = insertAction(UserAction.favorite(userId, itemType, itemId, eventId, time))
  def deleteFavorite(userId: String, itemType: String, itemId: String): Future[LastError] = deleteAction(FavoriteUserAction.className)(userId, itemType, itemId)

  def getDone(userId: String, itemType: String, itemId: String): Future[Option[UserAction]] = getAction(DoneUserAction.className)(userId, itemType, itemId)
  def insertDone(userId: String, itemType: String, itemId: String, eventId: String, time: Option[DateTime] = None): Future[Option[UserAction]] = insertAction(UserAction.done(userId, itemType, itemId, eventId, time))
  def deleteDone(userId: String, itemType: String, itemId: String): Future[LastError] = deleteAction(DoneUserAction.className)(userId, itemType, itemId)

  def getMood(userId: String, itemType: String, itemId: String): Future[Option[UserAction]] = getAction(MoodUserAction.className)(userId, itemType, itemId)
  def setMood(rating: String)(userId: String, itemType: String, itemId: String, eventId: String, oldElt: Option[UserAction], time: Option[DateTime] = None): Future[Option[UserAction]] = {
    val elt = oldElt.map(e => e.withContent(MoodUserAction(rating), time)).getOrElse(UserAction.mood(userId, itemType, itemId, rating, eventId, time))
    crud.upsert(Json.obj("userId" -> userId, "action." + MoodUserAction.className -> true, "itemType" -> itemType, "itemId" -> itemId, "uuid" -> elt.uuid), elt).map { err => if (err.ok) Some(elt) else None }
  }
  def deleteMood(userId: String, itemType: String, itemId: String): Future[LastError] = deleteAction(MoodUserAction.className)(userId, itemType, itemId)

  def getComment(userId: String, itemType: String, itemId: String, uuid: String): Future[Option[UserAction]] = crud.get(Json.obj("userId" -> userId, "action." + CommentUserAction.className -> true, "itemType" -> itemType, "itemId" -> itemId, "uuid" -> uuid))
  def insertComment(userId: String, itemType: String, itemId: String, text: String, eventId: String, time: Option[DateTime] = None): Future[Option[UserAction]] = insertAction(UserAction.comment(userId, itemType, itemId, text, eventId, time))
  def updateComment(userId: String, itemType: String, itemId: String, uuid: String, oldElt: UserAction, text: String, time: Option[DateTime] = None): Future[Option[UserAction]] = {
    val elt = oldElt.withContent(CommentUserAction(text), time)
    crud.update(Json.obj("userId" -> userId, "action." + CommentUserAction.className -> true, "itemType" -> itemType, "itemId" -> itemId, "uuid" -> uuid), elt).map { err => if (err.ok) Some(elt) else None }
  }
  def deleteComment(userId: String, itemType: String, itemId: String, uuid: String): Future[LastError] = crud.delete(Json.obj("userId" -> userId, "action." + CommentUserAction.className -> true, "itemType" -> itemType, "itemId" -> itemId, "uuid" -> uuid))

  def getSubscribe(userId: String, itemType: String, itemId: String): Future[Option[UserAction]] = getAction(SubscribeUserAction.className)(userId, itemType, itemId)
  def setSubscribe(email: String, filter: String)(userId: String, itemType: String, itemId: String, eventId: String, oldElt: Option[UserAction], time: Option[DateTime] = None): Future[Option[UserAction]] = {
    val elt = oldElt.map(e => e.withContent(SubscribeUserAction(email, filter), time)).getOrElse(UserAction.subscribe(userId, itemType, itemId, email, filter, eventId, time))
    crud.upsert(Json.obj("userId" -> userId, "action." + SubscribeUserAction.className -> true, "itemType" -> itemType, "itemId" -> itemId, "uuid" -> elt.uuid), elt).map { err => if (err.ok) Some(elt) else None }
  }
  def deleteSubscribe(userId: String, itemType: String, itemId: String): Future[LastError] = deleteAction(SubscribeUserAction.className)(userId, itemType, itemId)

  private def getAction(actionType: String)(userId: String, itemType: String, itemId: String): Future[Option[UserAction]] = crud.get(Json.obj("userId" -> userId, "action." + actionType -> true, "itemType" -> itemType, "itemId" -> itemId))
  private def insertAction(action: UserAction): Future[Option[UserAction]] = crud.insert(action).map { err => if (err.ok) Some(action) else None }
  private def deleteAction(actionType: String)(userId: String, itemType: String, itemId: String): Future[LastError] = crud.delete(Json.obj("userId" -> userId, "action." + actionType -> true, "itemType" -> itemType, "itemId" -> itemId))
}
object UserActionRepository extends MongoDbUserActionRepository
