package common.models.user

import common.repositories.Repository
import org.joda.time.DateTime
import play.api.libs.json._

case class Request(
  uuid: String,
  userId: Option[String], // id of user who initiated the request
  content: RequestContent,
  status: String, // pending, accepted, rejected
  created: DateTime,
  updated: DateTime)
object Request {
  object Status {
    val pending = "pending"
    val accepted = "accepted"
    val rejected = "rejected"
  }

  def accountRequest(email: String): Request = build(AccountRequest(email))
  def passwordReset(email: String): Request = build(PasswordReset(email))
  def userInvite(email: String, user: User): Request = build(UserInvite(email, None), user)
  def organizationRequest(organizationId: String, comment: Option[String], user: User): Request = build(OrganizationRequest(organizationId, comment), user)
  private def build(content: RequestContent): Request = Request(Repository.generateUuid(), None, content, Request.Status.pending, new DateTime(), new DateTime())
  private def build(content: RequestContent, user: User): Request = Request(Repository.generateUuid(), Some(user.uuid), content, Request.Status.pending, new DateTime(), new DateTime())

  private implicit val formatAccountRequest = Json.format[AccountRequest]
  private implicit val formatPasswordReset = Json.format[PasswordReset]
  private implicit val formatUserInvite = Json.format[UserInvite]
  private implicit val formatOrganizationRequest = Json.format[OrganizationRequest]
  implicit val formatRequestContent = Format(
    __.read[AccountRequest].map(x => x: RequestContent)
      .orElse(__.read[PasswordReset].map(x => x: RequestContent))
      .orElse(__.read[UserInvite].map(x => x: RequestContent))
      .orElse(__.read[OrganizationRequest].map(x => x: RequestContent)),
    Writes[RequestContent] {
      case c: AccountRequest => Json.toJson(c)(formatAccountRequest)
      case c: PasswordReset => Json.toJson(c)(formatPasswordReset)
      case c: UserInvite => Json.toJson(c)(formatUserInvite)
      case c: OrganizationRequest => Json.toJson(c)(formatOrganizationRequest)
    })
  implicit val format = Json.format[Request]
}

sealed trait RequestContent
case class AccountRequest(email: String, visited: Int = 0, accountRequest: Boolean = true) extends RequestContent
case class PasswordReset(email: String, passwordReset: Boolean = true) extends RequestContent
case class UserInvite(email: String, lastInviteSent: Option[DateTime], visited: Int = 0, userInvite: Boolean = true) extends RequestContent
case class OrganizationRequest(organizationId: String, comment: Option[String], organizationRequest: Boolean = true) extends RequestContent
