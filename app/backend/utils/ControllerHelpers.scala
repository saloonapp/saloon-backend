package backend.utils

import common.models.user.User
import common.models.user.Organization
import common.models.event.Event
import common.models.event.EventId
import common.models.event.Attendee
import common.models.event.AttendeeId
import common.models.event.Exponent
import common.models.event.ExponentId
import common.models.event.Session
import common.models.event.SessionId
import common.repositories.Repository
import common.repositories.user.UserRepository
import common.repositories.user.OrganizationRepository
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

  def withUser(userId: String)(block: User => Future[Result])(implicit flash: Flash, req: RequestHeader): Future[Result] = withData(UserRepository, "User")(userId)(block)
  def withOrganization(organizationId: String)(block: Organization => Future[Result])(implicit flash: Flash, req: RequestHeader): Future[Result] = withData(OrganizationRepository, "Organization")(organizationId)(block)
  def withEvent(eventId: EventId)(block: Event => Future[Result])(implicit flash: Flash, req: RequestHeader): Future[Result] = withData(EventRepository, "Event")(eventId)(block)
  def withAttendee(attendeeId: AttendeeId)(block: Attendee => Future[Result])(implicit flash: Flash, req: RequestHeader): Future[Result] = withData(AttendeeRepository, "Attendee")(attendeeId)(block)
  def withExponent(exponentId: ExponentId)(block: Exponent => Future[Result])(implicit flash: Flash, req: RequestHeader): Future[Result] = withData(ExponentRepository, "Exponent")(exponentId)(block)
  def withSession(sessionId: SessionId)(block: Session => Future[Result])(implicit flash: Flash, req: RequestHeader): Future[Result] = withData(SessionRepository, "Session")(sessionId)(block)

  private def withData[T, U](repository: Repository[T, U], name: String)(uuid: U)(block: T => Future[Result])(implicit flash: Flash, req: RequestHeader): Future[Result] = {
    repository.getByUuid(uuid).flatMap {
      case Some(data) => block(data)
      case None => Future(NotFound(backend.views.html.error("404", s"$name not found...")))
    }
  }

  def followRedirect(redirectOpt: Option[String], defaultRedirect: Call): Result = {
    redirectOpt.map { redirect => Redirect(redirect) }.getOrElse { Redirect(defaultRedirect) }
  }

}