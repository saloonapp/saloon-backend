package backend.controllers

import common.models.user.User
import common.models.user.UserInfo
import common.models.utils.Page
import common.repositories.event.EventRepository
import common.repositories.event.ExponentRepository
import common.repositories.event.AttendeeRepository
import common.services.EventSrv
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import com.mohiva.play.silhouette.core.LoginInfo

object Exponents extends SilhouetteEnvironment {

  def list(eventId: String, query: Option[String], page: Option[Int], pageSize: Option[Int], sort: Option[String]) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    val curPage = page.getOrElse(1)
    for {
      eventOpt <- EventRepository.getByUuid(eventId)
      eltPage <- ExponentRepository.findPageByEvent(eventId, query.getOrElse(""), curPage, pageSize.getOrElse(Page.defaultSize), sort.getOrElse("name"))
    } yield {
      if (curPage > 1 && eltPage.totalPages < curPage) {
        Redirect(backend.controllers.routes.Exponents.list(eventId, query, Some(eltPage.totalPages), pageSize, sort))
      } else {
        eventOpt
          .map { event => Ok(backend.views.html.Exponents.list(eltPage, event)) }
          .getOrElse { NotFound(backend.views.html.error("404", "Event not found...")) }
      }
    }
  }

  def details(eventId: String, uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    val futureData = for {
      eventOpt <- EventRepository.getByUuid(eventId)
      eltOpt <- ExponentRepository.getByUuid(uuid)
    } yield (eltOpt, eventOpt)
    futureData.flatMap {
      case (eltOpt, eventOpt) =>
        eltOpt.flatMap { elt =>
          eventOpt.map { event =>
            AttendeeRepository.findByUuids(elt.info.team).map { team => Ok(backend.views.html.Exponents.details(elt, team, event)) }
          }
        }.getOrElse { Future(NotFound(backend.views.html.error("404", "Event not found..."))) }
    }
  }

}
