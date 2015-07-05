package common.services

import common.Defaults
import common.models.event.Session
import common.models.event.Exponent
import common.models.user.User
import common.models.user.SubscribeUserAction
import common.repositories.event.EventRepository
import common.repositories.event.AttendeeRepository
import common.repositories.event.SessionRepository
import common.repositories.event.ExponentRepository
import common.repositories.user.UserActionRepository
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.jsoup.Jsoup

case class EmailData(fromName: String, fromEmail: String, to: String, subject: String, html: String, text: String)

object EmailSrv {
  val senderName = "L'équipe SalooN"
  val senderEmail = "contact@saloonapp.co"

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
              Some(EmailData(senderName, senderEmail, subscribe.email, s"Bilan ${eventOpt.get.name} by SalooN", html, text))
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

  def generateUserInviteEmail(inviteUrl: String, email: String): EmailData = {
    val html = common.views.html.Email.userInvite(inviteUrl).toString
    val text = common.views.txt.Email.userInvite(inviteUrl).toString
    EmailData(senderName, senderEmail, email, "Création de votre compte SalooN", html, text)
  }

}