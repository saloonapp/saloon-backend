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
  actionType: String,
  action: UserActionConent,
  itemType: String,
  itemId: String,
  eventId: Option[String],
  created: DateTime,
  updated: DateTime)
object UserAction {
  val favorite = "favorite"
  val comment = "comment"

  def fav(userId: String, itemType: String, itemId: String, eventId: String): UserAction = UserAction(Repository.generateUuid(), userId, favorite, FavoriteUserAction(), itemType, itemId, Some(eventId), new DateTime(), new DateTime())

  implicit val formatFavoriteUserAction = Json.format[FavoriteUserAction]
  implicit val formatCommentUserAction = Json.format[CommentUserAction]
  implicit val formatUserActionConent = Format(
    __.read[CommentUserAction].map(x => x: UserActionConent)
      .orElse(__.read[FavoriteUserAction].map(x => x: UserActionConent)),
    Writes[UserActionConent] {
      case comment: CommentUserAction => Json.toJson(comment)(formatCommentUserAction)
      case favorite: FavoriteUserAction => Json.toJson(favorite)(formatFavoriteUserAction)
    })
  implicit val format = Json.format[UserAction]
}

sealed trait UserActionConent
case class FavoriteUserAction(favorite: Boolean = true) extends UserActionConent
case class CommentUserAction(text: String, comment: Boolean = true) extends UserActionConent
