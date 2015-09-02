package backend.utils

import common.models.event.Event
import common.models.event.Attendee
import common.models.event.Exponent
import common.models.event.Session
import common.repositories.event.EventRepository
import common.repositories.event.AttendeeRepository
import common.repositories.event.ExponentRepository
import common.repositories.event.SessionRepository
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Call
import play.api.mvc.Flash
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.mvc.Results._

trait ControllerHelpers {

  def withEvent(eventId: String)(block: Event => Future[Result])(implicit flash: Flash, req: RequestHeader): Future[Result] = {
    EventRepository.getByUuid(eventId).flatMap {
      case Some(event) => block(event)
      case None => Future(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }

  def withAttendee(attendeeId: String)(block: Attendee => Future[Result])(implicit flash: Flash, req: RequestHeader): Future[Result] = {
    AttendeeRepository.getByUuid(attendeeId).flatMap {
      case Some(attendee) => block(attendee)
      case None => Future(NotFound(backend.views.html.error("404", "Attendee not found...")))
    }
  }

  def withExponent(exponentId: String)(block: Exponent => Future[Result])(implicit flash: Flash, req: RequestHeader): Future[Result] = {
    ExponentRepository.getByUuid(exponentId).flatMap {
      case Some(exponent) => block(exponent)
      case None => Future(NotFound(backend.views.html.error("404", "Exponent not found...")))
    }
  }

  def withSession(sessionId: String)(block: Session => Future[Result])(implicit flash: Flash, req: RequestHeader): Future[Result] = {
    SessionRepository.getByUuid(sessionId).flatMap {
      case Some(session) => block(session)
      case None => Future(NotFound(backend.views.html.error("404", "Session not found...")))
    }
  }

  def followRedirect(redirectOpt: Option[String], defaultRedirect: Call): Result = {
    redirectOpt.map { redirect => Redirect(redirect) }.getOrElse { Redirect(defaultRedirect) }
  }

}