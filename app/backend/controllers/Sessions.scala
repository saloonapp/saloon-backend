package backend.controllers

import common.models.event.Session
import common.models.user.User
import common.models.user.UserInfo
import common.repositories.event.EventRepository
import common.repositories.event.SessionRepository
import common.repositories.event.AttendeeRepository
import common.services.EventSrv
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import org.joda.time.DateTime
import com.mohiva.play.silhouette.core.LoginInfo

object Sessions extends SilhouetteEnvironment {

  def list(eventId: String, query: Option[String], page: Option[Int], pageSize: Option[Int], sort: Option[String]) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    val curPage = page.getOrElse(1)
    for {
      eventOpt <- EventRepository.getByUuid(eventId)
      elts <- SessionRepository.findByEvent(eventId, query.getOrElse(""), sort.getOrElse("info.start"))
    } yield {
      eventOpt
        .map { event =>
          val sessionsByDay: List[(DateTime, List[Session])] = elts.groupBy(startDate).toList.sortWith(sortByDate)
          Ok(backend.views.html.Sessions.list(sessionsByDay, event))
        }.getOrElse { NotFound(backend.views.html.error("404", "Event not found...")) }
    }
  }

  def details(eventId: String, uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    val futureData = for {
      eventOpt <- EventRepository.getByUuid(eventId)
      eltOpt <- SessionRepository.getByUuid(uuid)
    } yield (eltOpt, eventOpt)
    futureData.flatMap {
      case (eltOpt, eventOpt) =>
        eltOpt.flatMap { elt =>
          eventOpt.map { event =>
            AttendeeRepository.findByUuids(elt.info.speakers).map { attendees => Ok(backend.views.html.Sessions.details(elt, attendees, event)) }
          }
        }.getOrElse { Future(NotFound(backend.views.html.error("404", "Event not found..."))) }
    }
  }

  private def startDate(s: Session): DateTime = {
    if (s.info.start.isEmpty || s.info.end.isEmpty) new DateTime(0)
    else s.info.start.get.withTimeAtStartOfDay()
  }
  private def sortByDate(e1: (DateTime, List[Session]), e2: (DateTime, List[Session])): Boolean = {
    e1._1.isBefore(e2._1)
  }

}
