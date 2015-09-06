package common.models.user

import common.models.utils.tString
import common.models.utils.tStringHelper
import common.models.values.UUID
import org.joda.time.DateTime
import play.api.libs.json._

case class RequestId(val id: String) extends AnyVal with tString with UUID {
  def unwrap: String = this.id
}
object RequestId extends tStringHelper[RequestId] {
  def generate(): RequestId = RequestId(UUID.generate())
  def build(str: String): Option[RequestId] = UUID.toUUID(str).map(id => RequestId(id))
}

case class Request(
  uuid: RequestId,
  userId: Option[UserId], // id of user who initiated the request
  content: RequestContent,
  status: String, // pending, accepted, rejected, canceled
  created: DateTime,
  updated: DateTime)
object Request {
  object Status {
    val pending = "pending"
    val accepted = "accepted"
    val rejected = "rejected"
    val canceled = "canceled"
  }

  def accountRequest(email: String): Request = build(AccountRequest(email))
  def accountInvite(email: String, next: Option[RequestId], user: User): Request = build(AccountInvite(email, next), user)
  def passwordReset(email: String): Request = build(PasswordReset(email))
  def organizationRequest(organizationId: OrganizationId, comment: Option[String], user: User): Request = build(OrganizationRequest(organizationId, comment), user)
  def organizationInvite(organizationId: OrganizationId, email: String, comment: Option[String], user: User): Request = build(OrganizationInvite(organizationId, email, comment), user)
  private def build(content: RequestContent): Request = Request(RequestId.generate(), None, content, Request.Status.pending, new DateTime(), new DateTime())
  private def build(content: RequestContent, user: User): Request = Request(RequestId.generate(), Some(user.uuid), content, Request.Status.pending, new DateTime(), new DateTime())

  private implicit val formatAccountRequest = Json.format[AccountRequest]
  private implicit val formatAccountInvite = Json.format[AccountInvite]
  private implicit val formatPasswordReset = Json.format[PasswordReset]
  private implicit val formatOrganizationRequest = Json.format[OrganizationRequest]
  private implicit val formatOrganizationInvite = Json.format[OrganizationInvite]
  implicit val formatRequestContent = Format(
    __.read[AccountRequest].map(x => x: RequestContent)
      .orElse(__.read[AccountInvite].map(x => x: RequestContent))
      .orElse(__.read[PasswordReset].map(x => x: RequestContent))
      .orElse(__.read[OrganizationRequest].map(x => x: RequestContent))
      .orElse(__.read[OrganizationInvite].map(x => x: RequestContent)),
    Writes[RequestContent] {
      case c: AccountRequest => Json.toJson(c)(formatAccountRequest)
      case c: AccountInvite => Json.toJson(c)(formatAccountInvite)
      case c: PasswordReset => Json.toJson(c)(formatPasswordReset)
      case c: OrganizationRequest => Json.toJson(c)(formatOrganizationRequest)
      case c: OrganizationInvite => Json.toJson(c)(formatOrganizationInvite)
    })
  implicit val format = Json.format[Request]
}

sealed trait RequestContent
case class AccountRequest(email: String, visited: Int = 0, accountRequest: Boolean = true) extends RequestContent
case class AccountInvite(email: String, next: Option[RequestId], visited: Int = 0, accountInvite: Boolean = true) extends RequestContent
case class PasswordReset(email: String, passwordReset: Boolean = true) extends RequestContent
case class OrganizationRequest(organizationId: OrganizationId, comment: Option[String], organizationRequest: Boolean = true) extends RequestContent
case class OrganizationInvite(organizationId: OrganizationId, email: String, comment: Option[String], organizationInvite: Boolean = true) extends RequestContent
