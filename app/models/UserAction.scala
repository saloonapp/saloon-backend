package models

import common.infrastructure.repository.Repository
import org.joda.time.DateTime
import play.api.libs.json._

/*
 * Actions possibles :
 * 	- mise en favoris
 *  - commentaire
 */
case class UserAction(
  uuid: String,
  userId: String,
  action: UserActionConent,
  itemType: String,
  itemId: String,
  eventId: Option[String],
  created: DateTime,
  updated: DateTime) {
  def withContent(c: UserActionConent, time: Option[DateTime] = None): UserAction = this.copy(action = c, updated = time.getOrElse(new DateTime()))
  def toMap(): Map[String, String] = {
    Map(
      "eventId" -> this.eventId.getOrElse(""),
      "userId" -> this.userId,
      "itemType" -> this.itemType,
      "itemId" -> this.itemId) ++ this.action.toMap()
  }
}
object UserAction {
  def favorite(userId: String, itemType: String, itemId: String, eventId: String, time: Option[DateTime] = None): UserAction = UserAction(Repository.generateUuid(), userId, FavoriteUserAction(), itemType, itemId, Some(eventId), time.getOrElse(new DateTime()), time.getOrElse(new DateTime()))
  def done(userId: String, itemType: String, itemId: String, eventId: String, time: Option[DateTime] = None): UserAction = UserAction(Repository.generateUuid(), userId, DoneUserAction(), itemType, itemId, Some(eventId), time.getOrElse(new DateTime()), time.getOrElse(new DateTime()))
  def mood(userId: String, itemType: String, itemId: String, rating: String, eventId: String, time: Option[DateTime] = None): UserAction = UserAction(Repository.generateUuid(), userId, MoodUserAction(rating), itemType, itemId, Some(eventId), time.getOrElse(new DateTime()), time.getOrElse(new DateTime()))
  def comment(userId: String, itemType: String, itemId: String, text: String, eventId: String, time: Option[DateTime] = None): UserAction = UserAction(Repository.generateUuid(), userId, CommentUserAction(text), itemType, itemId, Some(eventId), time.getOrElse(new DateTime()), time.getOrElse(new DateTime()))
  def subscribe(userId: String, itemType: String, itemId: String, email: String, filter: String, eventId: String, time: Option[DateTime] = None): UserAction = UserAction(Repository.generateUuid(), userId, SubscribeUserAction(email, filter), itemType, itemId, Some(eventId), time.getOrElse(new DateTime()), time.getOrElse(new DateTime()))

  implicit val formatFavoriteUserAction = Json.format[FavoriteUserAction]
  implicit val formatDoneUserAction = Json.format[DoneUserAction]
  implicit val formatMoodUserAction = Json.format[MoodUserAction]
  implicit val formatCommentUserAction = Json.format[CommentUserAction]
  implicit val formatSubscribeUserAction = Json.format[SubscribeUserAction]
  implicit val formatUserActionConent = Format(
    __.read[FavoriteUserAction].map(x => x: UserActionConent)
      .orElse(__.read[DoneUserAction].map(x => x: UserActionConent))
      .orElse(__.read[MoodUserAction].map(x => x: UserActionConent))
      .orElse(__.read[CommentUserAction].map(x => x: UserActionConent))
      .orElse(__.read[SubscribeUserAction].map(x => x: UserActionConent)),
    Writes[UserActionConent] {
      case favorite: FavoriteUserAction => Json.toJson(favorite)(formatFavoriteUserAction)
      case done: DoneUserAction => Json.toJson(done)(formatDoneUserAction)
      case mood: MoodUserAction => Json.toJson(mood)(formatMoodUserAction)
      case comment: CommentUserAction => Json.toJson(comment)(formatCommentUserAction)
      case subscribe: SubscribeUserAction => Json.toJson(subscribe)(formatSubscribeUserAction)
    })
  implicit val format = Json.format[UserAction]
}

case class UserActionFull(event: Event, user: User, action: UserActionConent, item: EventItem) {
  def toMap(): Map[String, String] = {
    Map(
      "eventId" -> this.event.uuid,
      "eventName" -> this.event.name,
      "userId" -> this.user.uuid,
      "itemType" -> this.item.getType(),
      "itemId" -> this.item.uuid,
      "itemName" -> this.item.name) ++ this.action.toMap()
  }
}

sealed trait UserActionConent {
  def toMap(): Map[String, String] = {
    this match {
      case FavoriteUserAction(favorite) => Map("actionType" -> FavoriteUserAction.className, "rating" -> "", "text" -> "", "email" -> "", "filter" -> "")
      case DoneUserAction(done) => Map("actionType" -> DoneUserAction.className, "rating" -> "", "text" -> "", "email" -> "", "filter" -> "")
      case MoodUserAction(rating, mood) => Map("actionType" -> MoodUserAction.className, "rating" -> rating, "text" -> "", "email" -> "", "filter" -> "")
      case CommentUserAction(text, comment) => Map("actionType" -> CommentUserAction.className, "rating" -> "", "text" -> text, "email" -> "", "filter" -> "")
      case SubscribeUserAction(email, filter, subscribe) => Map("actionType" -> SubscribeUserAction.className, "rating" -> "", "text" -> "", "email" -> email, "filter" -> filter)
      case _ => Map("actionType" -> "Unknown", "rating" -> "", "text" -> "", "email" -> "", "filter" -> "")
    }
  }
}
case class FavoriteUserAction(favorite: Boolean = true) extends UserActionConent
object FavoriteUserAction {
  val className = "favorite"
}
case class DoneUserAction(done: Boolean = true) extends UserActionConent
object DoneUserAction {
  val className = "done"
}
case class MoodUserAction(rating: String, mood: Boolean = true) extends UserActionConent
object MoodUserAction {
  val className = "mood"
}
case class CommentUserAction(text: String, comment: Boolean = true) extends UserActionConent
object CommentUserAction {
  val className = "comment"
}
case class SubscribeUserAction(email: String, filter: String, subscribe: Boolean = true) extends UserActionConent
object SubscribeUserAction {
  val className = "subscribe"
}
