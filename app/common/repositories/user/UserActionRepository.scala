package common.repositories.user

import common.models.event.EventId
import common.models.user.DeviceId
import common.models.user.UserAction
import common.models.user.UserActionId
import common.models.user.FavoriteUserAction
import common.models.user.DoneUserAction
import common.models.user.MoodUserAction
import common.models.user.CommentUserAction
import common.models.user.SubscribeUserAction
import common.models.values.typed._
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

  def findByUser(deviceId: DeviceId): Future[List[UserAction]] = crud.find(Json.obj("userId" -> deviceId.unwrap))
  def findByEvent(eventId: EventId): Future[List[UserAction]] = crud.find(Json.obj("eventId" -> eventId.unwrap))
  def findByUserEvent(deviceId: DeviceId, eventId: EventId): Future[List[UserAction]] = crud.find(Json.obj("userId" -> deviceId.unwrap, "eventId" -> eventId.unwrap), Json.obj("created" -> 1))

  def getFavorite(deviceId: DeviceId, itemType: ItemType, itemId: GenericId): Future[Option[UserAction]] = getAction(FavoriteUserAction.className)(deviceId, itemType, itemId)
  def insertFavorite(deviceId: DeviceId, itemType: ItemType, itemId: GenericId, eventId: EventId, time: Option[DateTime] = None): Future[Option[UserAction]] = insertAction(UserAction.favorite(deviceId, itemType, itemId, eventId, time))
  def deleteFavorite(deviceId: DeviceId, itemType: ItemType, itemId: GenericId): Future[LastError] = deleteAction(FavoriteUserAction.className)(deviceId, itemType, itemId)

  def getDone(deviceId: DeviceId, itemType: ItemType, itemId: GenericId): Future[Option[UserAction]] = getAction(DoneUserAction.className)(deviceId, itemType, itemId)
  def insertDone(deviceId: DeviceId, itemType: ItemType, itemId: GenericId, eventId: EventId, time: Option[DateTime] = None): Future[Option[UserAction]] = insertAction(UserAction.done(deviceId, itemType, itemId, eventId, time))
  def deleteDone(deviceId: DeviceId, itemType: ItemType, itemId: GenericId): Future[LastError] = deleteAction(DoneUserAction.className)(deviceId, itemType, itemId)

  def getMood(deviceId: DeviceId, itemType: ItemType, itemId: GenericId): Future[Option[UserAction]] = getAction(MoodUserAction.className)(deviceId, itemType, itemId)
  def setMood(rating: String)(deviceId: DeviceId, itemType: ItemType, itemId: GenericId, eventId: EventId, oldElt: Option[UserAction], time: Option[DateTime] = None): Future[Option[UserAction]] = {
    val elt = oldElt.map(e => e.withContent(MoodUserAction(rating), time)).getOrElse(UserAction.mood(deviceId, itemType, itemId, rating, eventId, time))
    crud.upsert(Json.obj("userId" -> deviceId.unwrap, "action." + MoodUserAction.className -> true, "itemType" -> itemType, "itemId" -> itemId.unwrap, "uuid" -> elt.uuid), elt).map { err => if (err.ok) Some(elt) else None }
  }
  def deleteMood(deviceId: DeviceId, itemType: ItemType, itemId: GenericId): Future[LastError] = deleteAction(MoodUserAction.className)(deviceId, itemType, itemId)

  def getComment(deviceId: DeviceId, itemType: ItemType, itemId: GenericId, uuid: UserActionId): Future[Option[UserAction]] = crud.get(Json.obj("userId" -> deviceId.unwrap, "action." + CommentUserAction.className -> true, "itemType" -> itemType, "itemId" -> itemId.unwrap, "uuid" -> uuid))
  def insertComment(deviceId: DeviceId, itemType: ItemType, itemId: GenericId, text: TextMultiline, eventId: EventId, time: Option[DateTime] = None): Future[Option[UserAction]] = insertAction(UserAction.comment(deviceId, itemType, itemId, text, eventId, time))
  def updateComment(deviceId: DeviceId, itemType: ItemType, itemId: GenericId, uuid: UserActionId, oldElt: UserAction, text: TextMultiline, time: Option[DateTime] = None): Future[Option[UserAction]] = {
    val elt = oldElt.withContent(CommentUserAction(text), time)
    crud.update(Json.obj("userId" -> deviceId.unwrap, "action." + CommentUserAction.className -> true, "itemType" -> itemType, "itemId" -> itemId.unwrap, "uuid" -> uuid), elt).map { err => if (err.ok) Some(elt) else None }
  }
  def deleteComment(deviceId: DeviceId, itemType: ItemType, itemId: GenericId, uuid: UserActionId): Future[LastError] = crud.delete(Json.obj("userId" -> deviceId.unwrap, "action." + CommentUserAction.className -> true, "itemType" -> itemType, "itemId" -> itemId.unwrap, "uuid" -> uuid))

  def getSubscribe(deviceId: DeviceId, itemType: ItemType, itemId: GenericId): Future[Option[UserAction]] = getAction(SubscribeUserAction.className)(deviceId, itemType, itemId)
  def findSubscribes(itemType: ItemType, itemId: GenericId): Future[List[UserAction]] = crud.find(Json.obj("action." + SubscribeUserAction.className -> true, "itemType" -> itemType, "itemId" -> itemId.unwrap))
  def setSubscribe(email: Email, filter: String)(deviceId: DeviceId, itemType: ItemType, itemId: GenericId, eventId: EventId, oldElt: Option[UserAction], time: Option[DateTime] = None): Future[Option[UserAction]] = {
    val elt = oldElt.map(e => e.withContent(SubscribeUserAction(email, filter), time)).getOrElse(UserAction.subscribe(deviceId, itemType, itemId, email, filter, eventId, time))
    crud.upsert(Json.obj("userId" -> deviceId.unwrap, "action." + SubscribeUserAction.className -> true, "itemType" -> itemType, "itemId" -> itemId.unwrap, "uuid" -> elt.uuid), elt).map { err => if (err.ok) Some(elt) else None }
  }
  def deleteSubscribe(deviceId: DeviceId, itemType: ItemType, itemId: GenericId): Future[LastError] = deleteAction(SubscribeUserAction.className)(deviceId, itemType, itemId)

  def countForEvent(eventId: EventId): Future[Int] = crud.countFor("eventId", eventId.unwrap)
  def countForEvents(eventIds: Seq[EventId]): Future[Map[EventId, Int]] = crud.countFor("eventId", eventIds.map(_.unwrap)).map(_.map { case (key, value) => (EventId(key), value) })
  def bulkInsert(elts: List[UserAction]): Future[Int] = crud.bulkInsert(elts)
  def deleteByEventUser(eventId: EventId, deviceId: DeviceId): Future[LastError] = collection.remove(Json.obj("eventId" -> eventId.unwrap, "userId" -> deviceId.unwrap))

  def deleteByEvent(eventId: EventId): Future[LastError] = crud.deleteBy("eventId", eventId.unwrap)
  def deleteByItem(itemType: ItemType, itemId: String): Future[LastError] = collection.remove(Json.obj("itemType" -> itemType, "itemId" -> itemId))
  def deleteByUser(deviceId: DeviceId): Future[LastError] = crud.deleteBy("userId", deviceId.unwrap)

  private def getAction(actionType: String)(deviceId: DeviceId, itemType: ItemType, itemId: GenericId): Future[Option[UserAction]] = crud.get(Json.obj("userId" -> deviceId.unwrap, "action." + actionType -> true, "itemType" -> itemType, "itemId" -> itemId.unwrap))
  private def insertAction(action: UserAction): Future[Option[UserAction]] = crud.insert(action).map { err => if (err.ok) Some(action) else None }
  private def deleteAction(actionType: String)(deviceId: DeviceId, itemType: ItemType, itemId: GenericId): Future[LastError] = crud.delete(Json.obj("userId" -> deviceId.unwrap, "action." + actionType -> true, "itemType" -> itemType, "itemId" -> itemId.unwrap))
}
object UserActionRepository extends MongoDbUserActionRepository
