package common.models.user

import common.models.utils.tString
import common.models.utils.tStringHelper
import common.models.values.UUID
import common.models.values.typed._
import org.joda.time.DateTime
import play.api.libs.json._

case class RequestId(id: String) extends AnyVal with tString with UUID {
  def unwrap: String = this.id
}
object RequestId extends tStringHelper[RequestId] {
  def generate(): RequestId = RequestId(UUID.generate())
  def build(str: String): Either[String, RequestId] = UUID.toUUID(str).right.map(id => RequestId(id)).left.map(_ + " for RequestId")
}

case class Request(
  uuid: RequestId,
  userId: Option[UserId], // id of user who initiated the request
  content: RequestContent,
  status: RequestStatus,
  created: DateTime,
  updated: DateTime)
object Request {
  def accountRequest(email: Email): Request = build(AccountRequest(email))
  def accountInvite(email: Email, next: Option[RequestId], user: User): Request = build(AccountInvite(email, next), user)
  def passwordReset(email: Email): Request = build(PasswordReset(email))
  def organizationRequest(organizationId: OrganizationId, comment: Option[TextMultiline], user: User): Request = build(OrganizationRequest(organizationId, comment), user)
  def organizationInvite(organizationId: OrganizationId, email: Email, comment: Option[TextMultiline], user: User): Request = build(OrganizationInvite(organizationId, email, comment), user)
  private def build(content: RequestContent): Request = Request(RequestId.generate(), None, content, RequestStatus.pending, new DateTime(), new DateTime())
  private def build(content: RequestContent, user: User): Request = Request(RequestId.generate(), Some(user.uuid), content, RequestStatus.pending, new DateTime(), new DateTime())

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
case class AccountRequest(email: Email, visited: Int = 0, accountRequest: Boolean = true) extends RequestContent
case class AccountInvite(email: Email, next: Option[RequestId], visited: Int = 0, accountInvite: Boolean = true) extends RequestContent
case class PasswordReset(email: Email, passwordReset: Boolean = true) extends RequestContent
case class OrganizationRequest(organizationId: OrganizationId, comment: Option[TextMultiline], organizationRequest: Boolean = true) extends RequestContent
case class OrganizationInvite(organizationId: OrganizationId, email: Email, comment: Option[TextMultiline], organizationInvite: Boolean = true) extends RequestContent
