package common.services

import common.Defaults
import common.models.event.Session
import common.models.event.Exponent
import common.models.user.User
import common.models.user.Organization
import common.models.user.Request
import common.models.user.SubscribeUserAction
import common.repositories.event.EventRepository
import common.repositories.event.AttendeeRepository
import common.repositories.event.SessionRepository
import common.repositories.event.ExponentRepository
import common.repositories.user.UserActionRepository
import scala.concurrent.Future
import play.api.mvc.RequestHeader
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.jsoup.Jsoup

case class EmailData(fromName: String, fromEmail: String, to: String, subject: String, html: String, text: String)

object EmailSrv {
  def generateEventReport(eventId: String, userId: String): Future[Option[EmailData]] = {
    UserActionRepository.findByUserEvent(userId, eventId).flatMap { actions =>
      val subscribeOpt = actions.find(_.action.isSubscribe())
      subscribeOpt.map {
        _.action match {
          case subscribe: SubscribeUserAction => {
            val favoriteSessionUuids = actions.filter(a => a.action.isFavorite() && a.itemType == Session.className).map(_.itemId)
            val favoriteExponentUuids = actions.filter(a => a.action.isFavorite() && a.itemType == Exponent.className).map(_.itemId)
            for {
              eventOpt <- EventRepository.getByUuid(eventId)
              attendees <- AttendeeRepository.findByEvent(eventId)
              sessions <- if (subscribe.filter == "favorites") SessionRepository.findByUuids(favoriteSessionUuids) else SessionRepository.findByEvent(eventId)
              exponents <- if (subscribe.filter == "favorites") ExponentRepository.findByUuids(favoriteExponentUuids) else ExponentRepository.findByEvent(eventId)
            } yield {
              val sessionsWithSpeakers = sessions.map(e => (e, attendees.filter(a => e.info.speakers.contains(a.uuid))))
              val exponentsWithTeam = exponents.map(e => (e, attendees.filter(a => e.info.team.contains(a.uuid))))
              val html = admin.views.html.Email.eventAttendeeReport(eventOpt.get, sessionsWithSpeakers, exponentsWithTeam, actions, subscribe.filter).toString
              val text = Jsoup.parse(html).text()
              Some(EmailData(Defaults.contactName, Defaults.contactEmail, subscribe.email, s"Bilan ${eventOpt.get.name} by SalooN", html, text))
            }
          }
          case _ => Future(None) // not subscribed
        }
      }.getOrElse(Future(None)) // not subscribed
    }
  }

  def generateContactEmail(source: String, name: String, email: String, message: String, userOpt: Option[User]): EmailData = {
    val html = common.views.html.Email.contact(source, name, email, message, userOpt).toString
    val text = common.views.txt.Email.contact(source, name, email, message, userOpt).toString
    EmailData(name, email, Defaults.contactEmail, s"Contact SalooN depuis ${source}", html, text)
  }

  def generateAccountRequestEmail(email: String, requestId: String)(implicit req: RequestHeader): EmailData = {
    val saloonUrl = website.controllers.routes.Application.index().absoluteURL(Defaults.secureUrl)
    val inviteUrl = authentication.controllers.routes.Auth.createAccount(requestId).absoluteURL(Defaults.secureUrl)
    val html = authentication.views.html.Email.accountRequest(email, saloonUrl, inviteUrl).toString
    val text = Jsoup.parse(html).text()
    EmailData(Defaults.contactName, Defaults.contactEmail, email, "Invitation à SalooN", html, text)
  }

  def generateOrganizationRequestEmail(user: User, organization: Organization, organizationOwner: User, request: Request)(implicit req: RequestHeader): EmailData = {
    val acceptUrl = backend.controllers.routes.Requests.accept(request.uuid).absoluteURL(Defaults.secureUrl)
    val rejectUrl = backend.controllers.routes.Requests.reject(request.uuid).absoluteURL(Defaults.secureUrl)
    val html = backend.views.html.Emails.organizationRequest(user, organization, request, acceptUrl, rejectUrl).toString
    val text = Jsoup.parse(html).text()
    EmailData(user.name(), user.email, organizationOwner.email, s"Demande d'accès à l'organisation ${organization.name} sur SalooN", html, text)
  }

  def generateOrganizationRequestAcceptedEmail(user: User, organization: Organization, organizationOwner: User): EmailData = {
    val html = backend.views.html.Emails.organizationRequestAccepted(organization, organizationOwner).toString
    val text = Jsoup.parse(html).text()
    EmailData(Defaults.contactName, Defaults.contactEmail, user.email, s"Accès accordé à l'organisation ${organization.name} sur SalooN", html, text)
  }

  def generateOrganizationRequestRejectedEmail(user: User, organization: Organization): EmailData = {
    val html = backend.views.html.Emails.organizationRequestRejected(organization).toString
    val text = Jsoup.parse(html).text()
    EmailData(Defaults.contactName, Defaults.contactEmail, user.email, s"Accès à l'organisation ${organization.name} refusé :(", html, text)
  }

  def generateOrganizationInviteEmail(user: User, organization: Organization, invitedUser: User, request: Request)(implicit req: RequestHeader): EmailData = {
    val acceptUrl = backend.controllers.routes.Requests.accept(request.uuid).absoluteURL(Defaults.secureUrl)
    val rejectUrl = backend.controllers.routes.Requests.reject(request.uuid).absoluteURL(Defaults.secureUrl)
    val html = backend.views.html.Emails.organizationInvite(organization, user, request, acceptUrl, rejectUrl).toString
    val text = Jsoup.parse(html).text()
    EmailData(user.name(), user.email, invitedUser.email, s"Invitation à l'organisation ${organization.name} sur SalooN", html, text)
  }

  def generateOrganizationAndSalooNInviteEmail(user: User, organization: Organization, invitedEmail: String, commentOpt: Option[String], request: Request)(implicit req: RequestHeader): EmailData = {
    val inviteUrl = authentication.controllers.routes.Auth.createAccount(request.uuid).absoluteURL(Defaults.secureUrl)
    val html = backend.views.html.Emails.organizationAndSalooNInvite(organization, user, commentOpt, request, inviteUrl).toString
    val text = Jsoup.parse(html).text()
    EmailData(user.name(), user.email, invitedEmail, s"Invitation à l'organisation ${organization.name} sur SalooN", html, text)
  }

  def generateOrganizationInviteAcceptedEmail(invitedUser: User, organization: Organization, organizationOwner: User): EmailData = {
    val html = backend.views.html.Emails.organizationInviteAccepted(invitedUser, organization).toString
    val text = Jsoup.parse(html).text()
    EmailData(Defaults.contactName, Defaults.contactEmail, organizationOwner.email, s"Invitation à SalooN acceptée par ${invitedUser.name()}", html, text)
  }

  def generateOrganizationInviteRejectedEmail(invitedEmail: String, organization: Organization, organizationOwner: User): EmailData = {
    val html = backend.views.html.Emails.organizationInviteRejected(invitedEmail, organization).toString
    val text = Jsoup.parse(html).text()
    EmailData(Defaults.contactName, Defaults.contactEmail, organizationOwner.email, s"Invitation à l'organisation ${organization.name} refusée :(", html, text)
  }

  def generateOrganizationInviteCanceledEmail(invitedEmail: String, organization: Organization): EmailData = {
    val html = backend.views.html.Emails.organizationInviteCanceled(organization).toString
    val text = Jsoup.parse(html).text()
    EmailData(Defaults.contactName, Defaults.contactEmail, invitedEmail, s"Invitation à ${organization.name} annulée :(", html, text)
  }

  def generateOrganizationLeaveEmail(leavingUser: User, organization: Organization, organizationOwner: User): EmailData = {
    val html = backend.views.html.Emails.organizationLeave(leavingUser, organization).toString
    val text = Jsoup.parse(html).text()
    EmailData(Defaults.contactName, Defaults.contactEmail, organizationOwner.email, s"${leavingUser.name()} quitte l'organisation ${organization.name}", html, text)
  }

  def generateOrganizationBanEmail(bannedUser: User, organization: Organization, organizationOwner: User): EmailData = {
    val html = backend.views.html.Emails.organizationBan(organization, organizationOwner).toString
    val text = Jsoup.parse(html).text()
    EmailData(Defaults.contactName, Defaults.contactEmail, bannedUser.email, s"Vos accès à l'organisation ${organization.name} sont révoqués", html, text)
  }

  def generateOrganizationDeleteEmail(user: User, organization: Organization, deletingUser: User): EmailData = {
    val html = backend.views.html.Emails.organizationDelete(deletingUser, organization).toString
    val text = Jsoup.parse(html).text()
    EmailData(Defaults.contactName, Defaults.contactEmail, user.email, s"${deletingUser.name()} supprime l'organisation ${organization.name}", html, text)
  }

}