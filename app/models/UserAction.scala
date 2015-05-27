package models

import infrastructure.repository.common.Repository
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

sealed trait UserActionConent
case class FavoriteUserAction(favorite: Boolean = true) extends UserActionConent
case class DoneUserAction(done: Boolean = true) extends UserActionConent
case class MoodUserAction(rating: String, mood: Boolean = true) extends UserActionConent
case class CommentUserAction(text: String, comment: Boolean = true) extends UserActionConent
case class SubscribeUserAction(email: String, filter: String, subscribe: Boolean = true) extends UserActionConent

