package backend.controllers

import common.models.user.User
import common.models.user.UserInfo
import common.models.utils.Page
import common.repositories.event.EventRepository
import common.repositories.event.AttendeeRepository
import common.repositories.event.SessionRepository
import common.repositories.event.ExponentRepository
import common.services.EventSrv
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import com.mohiva.play.silhouette.core.LoginInfo

object Attendees extends SilhouetteEnvironment {

  def list(eventId: String, query: Option[String], page: Option[Int], pageSize: Option[Int], sort: Option[String]) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    val curPage = page.getOrElse(1)
    for {
      eventOpt <- EventRepository.getByUuid(eventId)
      eltPage <- AttendeeRepository.findPageByEvent(eventId, query.getOrElse(""), curPage, pageSize.getOrElse(Page.defaultSize), sort.getOrElse("name"))
    } yield {
      if (curPage > 1 && eltPage.totalPages < curPage) {
        Redirect(backend.controllers.routes.Attendees.list(eventId, query, Some(eltPage.totalPages), pageSize, sort))
      } else {
        eventOpt
          .map { event => Ok(backend.views.html.Attendees.list(eltPage, event)) }
          .getOrElse { NotFound(backend.views.html.error("404", "Event not found...")) }
      }
    }
  }

  def details(eventId: String, uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    for {
      eventOpt <- EventRepository.getByUuid(eventId)
      eltOpt <- AttendeeRepository.getByUuid(uuid)
      sessions <- SessionRepository.findByEventAttendee(eventId, uuid)
      exponents <- ExponentRepository.findByEventAttendee(eventId, uuid)
    } yield {
      eltOpt.flatMap { elt =>
        eventOpt.map { event => Ok(backend.views.html.Attendees.details(elt, sessions, exponents, event)) }
      }.getOrElse { NotFound(backend.views.html.error("404", "Event not found...")) }
    }
  }

}
