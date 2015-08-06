package common.models.user

import common.repositories.Repository
import org.joda.time.DateTime
import play.api.libs.json._

case class Request(
  uuid: String,
  userId: Option[String], // id of user who initiated the request
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
  def accountInvite(email: String, next: Option[String], user: User): Request = build(AccountInvite(email, next), user)
  def passwordReset(email: String): Request = build(PasswordReset(email))
  def organizationRequest(organizationId: String, comment: Option[String], user: User): Request = build(OrganizationRequest(organizationId, comment), user)
  def organizationInvite(organizationId: String, email: String, comment: Option[String], user: User): Request = build(OrganizationInvite(organizationId, email, comment), user)
  private def build(content: RequestContent): Request = Request(Repository.generateUuid(), None, content, Request.Status.pending, new DateTime(), new DateTime())
  private def build(content: RequestContent, user: User): Request = Request(Repository.generateUuid(), Some(user.uuid), content, Request.Status.pending, new DateTime(), new DateTime())

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
case class AccountInvite(email: String, next: Option[String], visited: Int = 0, accountInvite: Boolean = true) extends RequestContent
case class PasswordReset(email: String, passwordReset: Boolean = true) extends RequestContent
case class OrganizationRequest(organizationId: String, comment: Option[String], organizationRequest: Boolean = true) extends RequestContent
case class OrganizationInvite(organizationId: String, email: String, comment: Option[String], organizationInvite: Boolean = true) extends RequestContent
