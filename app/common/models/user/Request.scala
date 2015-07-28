package common.models.user

import org.joda.time.DateTime
import play.api.libs.json._

case class Request(
  uuid: String,
  userId: Option[String], // id of user who initiated the request
  content: RequestContent,
  status: String, // pending, accepted, rejected
  created: DateTime)
object Request {
  private implicit val formatPasswordReset = Json.format[PasswordReset]
  private implicit val formatUserInvite = Json.format[UserInvite]
  implicit val formatRequestContent = Format(
    __.read[PasswordReset].map(x => x: RequestContent)
      .orElse(__.read[UserInvite].map(x => x: RequestContent)),
    Writes[RequestContent] {
      case c: PasswordReset => Json.toJson(c)(formatPasswordReset)
      case c: UserInvite => Json.toJson(c)(formatUserInvite)
    })
  implicit val format = Json.format[Request]
}

sealed trait RequestContent
case class PasswordReset(email: String, passwordReset: Boolean = true) extends RequestContent
case class UserInvite(email: String, lastInviteSent: DateTime, userInvite: Boolean = true) extends RequestContent
