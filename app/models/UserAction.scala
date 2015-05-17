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
  def withContent(c: UserActionConent): UserAction = this.copy(action = c, updated = new DateTime())
}
object UserAction {
  def favorite(userId: String, itemType: String, itemId: String, eventId: String): UserAction = UserAction(Repository.generateUuid(), userId, FavoriteUserAction(), itemType, itemId, Some(eventId), new DateTime(), new DateTime())
  def mood(userId: String, itemType: String, itemId: String, rating: String, eventId: String): UserAction = UserAction(Repository.generateUuid(), userId, MoodUserAction(rating), itemType, itemId, Some(eventId), new DateTime(), new DateTime())
  def comment(userId: String, itemType: String, itemId: String, text: String, eventId: String): UserAction = UserAction(Repository.generateUuid(), userId, CommentUserAction(text), itemType, itemId, Some(eventId), new DateTime(), new DateTime())

  implicit val formatFavoriteUserAction = Json.format[FavoriteUserAction]
  implicit val formatMoodUserAction = Json.format[MoodUserAction]
  implicit val formatCommentUserAction = Json.format[CommentUserAction]
  implicit val formatUserActionConent = Format(
    __.read[FavoriteUserAction].map(x => x: UserActionConent)
      .orElse(__.read[MoodUserAction].map(x => x: UserActionConent))
      .orElse(__.read[CommentUserAction].map(x => x: UserActionConent)),
    Writes[UserActionConent] {
      case favorite: FavoriteUserAction => Json.toJson(favorite)(formatFavoriteUserAction)
      case mood: MoodUserAction => Json.toJson(mood)(formatMoodUserAction)
      case comment: CommentUserAction => Json.toJson(comment)(formatCommentUserAction)
    })
  implicit val format = Json.format[UserAction]
}

sealed trait UserActionConent
case class FavoriteUserAction(favorite: Boolean = true) extends UserActionConent
case class MoodUserAction(rating: String, mood: Boolean = true) extends UserActionConent
case class CommentUserAction(text: String, comment: Boolean = true) extends UserActionConent