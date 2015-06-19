package services

import models.Session
import models.Exponent
import models.SubscribeUserAction
import infrastructure.repository.EventRepository
import infrastructure.repository.SessionRepository
import infrastructure.repository.ExponentRepository
import infrastructure.repository.UserActionRepository
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.jsoup.Jsoup

case class EmailData(to: String, subject: String, html: String, text: String)

object MailSrv {

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
              sessions <- if (subscribe.filter == "favorites") SessionRepository.findByUuids(favoriteSessionUuids) else SessionRepository.findByEvent(eventId)
              exponents <- if (subscribe.filter == "favorites") ExponentRepository.findByUuids(favoriteExponentUuids) else ExponentRepository.findByEvent(eventId)
            } yield {
              val html = views.html.Mail.eventAttendeeReport(eventOpt.get, sessions, exponents, actions, subscribe.filter).toString
              val text = Jsoup.parse(html).text()
              Some(EmailData(subscribe.email, s"Bilan ${eventOpt.get.name} by SalooN", html, text))
            }
          }
          case _ => Future(None) // not subscribed
        }
      }.getOrElse(Future(None)) // not subscribed
    }
  }

}