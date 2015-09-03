package common.repositories.user

import common.models.event.EventId
import common.models.user.UserAction
import common.models.user.FavoriteUserAction
import common.models.user.DoneUserAction
import common.models.user.MoodUserAction
import common.models.user.CommentUserAction
import common.models.user.SubscribeUserAction
import common.models.values.GenericId
import common.models.utils.Page
import common.repositories.Repository
import common.repositories.CollectionReferences
import common.repositories.utils.MongoDbCrudUtils
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
  def findByEvent(eventId: EventId): Future[List[UserAction]] = crud.find(Json.obj("eventId" -> eventId.unwrap))
  def findByUserEvent(userId: String, eventId: EventId): Future[List[UserAction]] = crud.find(Json.obj("userId" -> userId, "eventId" -> eventId.unwrap), Json.obj("created" -> 1))

  def getFavorite(userId: String, itemType: String, itemId: GenericId): Future[Option[UserAction]] = getAction(FavoriteUserAction.className)(userId, itemType, itemId)
  def insertFavorite(userId: String, itemType: String, itemId: GenericId, eventId: EventId, time: Option[DateTime] = None): Future[Option[UserAction]] = insertAction(UserAction.favorite(userId, itemType, itemId, eventId, time))
  def deleteFavorite(userId: String, itemType: String, itemId: GenericId): Future[LastError] = deleteAction(FavoriteUserAction.className)(userId, itemType, itemId)

  def getDone(userId: String, itemType: String, itemId: GenericId): Future[Option[UserAction]] = getAction(DoneUserAction.className)(userId, itemType, itemId)
  def insertDone(userId: String, itemType: String, itemId: GenericId, eventId: EventId, time: Option[DateTime] = None): Future[Option[UserAction]] = insertAction(UserAction.done(userId, itemType, itemId, eventId, time))
  def deleteDone(userId: String, itemType: String, itemId: GenericId): Future[LastError] = deleteAction(DoneUserAction.className)(userId, itemType, itemId)

  def getMood(userId: String, itemType: String, itemId: GenericId): Future[Option[UserAction]] = getAction(MoodUserAction.className)(userId, itemType, itemId)
  def setMood(rating: String)(userId: String, itemType: String, itemId: GenericId, eventId: EventId, oldElt: Option[UserAction], time: Option[DateTime] = None): Future[Option[UserAction]] = {
    val elt = oldElt.map(e => e.withContent(MoodUserAction(rating), time)).getOrElse(UserAction.mood(userId, itemType, itemId, rating, eventId, time))
    crud.upsert(Json.obj("userId" -> userId, "action." + MoodUserAction.className -> true, "itemType" -> itemType, "itemId" -> itemId.unwrap, "uuid" -> elt.uuid), elt).map { err => if (err.ok) Some(elt) else None }
  }
  def deleteMood(userId: String, itemType: String, itemId: GenericId): Future[LastError] = deleteAction(MoodUserAction.className)(userId, itemType, itemId)

  def getComment(userId: String, itemType: String, itemId: GenericId, uuid: String): Future[Option[UserAction]] = crud.get(Json.obj("userId" -> userId, "action." + CommentUserAction.className -> true, "itemType" -> itemType, "itemId" -> itemId.unwrap, "uuid" -> uuid))
  def insertComment(userId: String, itemType: String, itemId: GenericId, text: String, eventId: EventId, time: Option[DateTime] = None): Future[Option[UserAction]] = insertAction(UserAction.comment(userId, itemType, itemId, text, eventId, time))
  def updateComment(userId: String, itemType: String, itemId: GenericId, uuid: String, oldElt: UserAction, text: String, time: Option[DateTime] = None): Future[Option[UserAction]] = {
    val elt = oldElt.withContent(CommentUserAction(text), time)
    crud.update(Json.obj("userId" -> userId, "action." + CommentUserAction.className -> true, "itemType" -> itemType, "itemId" -> itemId.unwrap, "uuid" -> uuid), elt).map { err => if (err.ok) Some(elt) else None }
  }
  def deleteComment(userId: String, itemType: String, itemId: GenericId, uuid: String): Future[LastError] = crud.delete(Json.obj("userId" -> userId, "action." + CommentUserAction.className -> true, "itemType" -> itemType, "itemId" -> itemId.unwrap, "uuid" -> uuid))

  def getSubscribe(userId: String, itemType: String, itemId: GenericId): Future[Option[UserAction]] = getAction(SubscribeUserAction.className)(userId, itemType, itemId)
  def findSubscribes(itemType: String, itemId: GenericId): Future[List[UserAction]] = crud.find(Json.obj("action." + SubscribeUserAction.className -> true, "itemType" -> itemType, "itemId" -> itemId.unwrap))
  def setSubscribe(email: String, filter: String)(userId: String, itemType: String, itemId: GenericId, eventId: EventId, oldElt: Option[UserAction], time: Option[DateTime] = None): Future[Option[UserAction]] = {
    val elt = oldElt.map(e => e.withContent(SubscribeUserAction(email, filter), time)).getOrElse(UserAction.subscribe(userId, itemType, itemId, email, filter, eventId, time))
    crud.upsert(Json.obj("userId" -> userId, "action." + SubscribeUserAction.className -> true, "itemType" -> itemType, "itemId" -> itemId.unwrap, "uuid" -> elt.uuid), elt).map { err => if (err.ok) Some(elt) else None }
  }
  def deleteSubscribe(userId: String, itemType: String, itemId: GenericId): Future[LastError] = deleteAction(SubscribeUserAction.className)(userId, itemType, itemId)

  def countForEvent(eventId: EventId): Future[Int] = crud.countFor("eventId", eventId.unwrap)
  def countForEvents(eventIds: Seq[EventId]): Future[Map[EventId, Int]] = crud.countFor("eventId", eventIds.map(_.unwrap)).map(_.map { case (key, value) => (EventId(key), value) })
  def bulkInsert(elts: List[UserAction]): Future[Int] = crud.bulkInsert(elts)
  def deleteByEventUser(eventId: EventId, userId: String): Future[LastError] = collection.remove(Json.obj("eventId" -> eventId.unwrap, "userId" -> userId))

  def deleteByEvent(eventId: EventId): Future[LastError] = crud.deleteBy("eventId", eventId.unwrap)
  def deleteByItem(itemType: String, itemId: String): Future[LastError] = collection.remove(Json.obj("itemType" -> itemType, "itemId" -> itemId))
  def deleteByUser(userId: String): Future[LastError] = crud.deleteBy("userId", userId)

  private def getAction(actionType: String)(userId: String, itemType: String, itemId: GenericId): Future[Option[UserAction]] = crud.get(Json.obj("userId" -> userId, "action." + actionType -> true, "itemType" -> itemType, "itemId" -> itemId.unwrap))
  private def insertAction(action: UserAction): Future[Option[UserAction]] = crud.insert(action).map { err => if (err.ok) Some(action) else None }
  private def deleteAction(actionType: String)(userId: String, itemType: String, itemId: GenericId): Future[LastError] = crud.delete(Json.obj("userId" -> userId, "action." + actionType -> true, "itemType" -> itemType, "itemId" -> itemId.unwrap))
}
object UserActionRepository extends MongoDbUserActionRepository
