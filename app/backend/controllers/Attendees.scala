package backend.controllers

import common.models.event.EventId
import common.models.event.Attendee
import common.models.event.AttendeeId
import common.models.user.User
import common.models.utils.Page
import common.services.FileExporter
import common.repositories.event.EventRepository
import common.repositories.event.AttendeeRepository
import common.repositories.event.ExponentRepository
import common.repositories.event.SessionRepository
import backend.forms.AttendeeCreateData
import backend.utils.ControllerHelpers
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form

object Attendees extends SilhouetteEnvironment with ControllerHelpers {
  val createForm: Form[AttendeeCreateData] = Form(AttendeeCreateData.fields)

  def list(eventId: EventId, query: Option[String], page: Option[Int], pageSize: Option[Int], sort: Option[String]) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    val curPage = page.getOrElse(1)
    withEvent(eventId) { event =>
      AttendeeRepository.findPageByEvent(eventId, query.getOrElse(""), curPage, pageSize.getOrElse(Page.defaultSize), sort.getOrElse("name")).map { attendeePage =>
        if (1 < curPage && attendeePage.totalPages < curPage) {
          Redirect(backend.controllers.routes.Attendees.list(eventId, query, Some(attendeePage.totalPages), pageSize, sort))
        } else {
          Ok(backend.views.html.Events.Attendees.list(attendeePage, event))
        }
      }
    }
  }

  def details(eventId: EventId, attendeeId: AttendeeId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withEvent(eventId) { event =>
      withAttendee(attendeeId) { attendee =>
        for {
          attendeeSessions <- SessionRepository.findByEventAttendee(eventId, attendeeId)
          attendeeExponents <- ExponentRepository.findByEventAttendee(eventId, attendeeId)
        } yield {
          Ok(backend.views.html.Events.Attendees.details(attendee, attendeeSessions, attendeeExponents, event))
        }
      }
    }
  }

  def create(eventId: EventId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    createView(createForm, eventId)
  }

  def doCreate(eventId: EventId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    createForm.bindFromRequest.fold(
      formWithErrors => createView(formWithErrors, eventId, BadRequest),
      formData => AttendeeRepository.insert(AttendeeCreateData.toModel(formData)).flatMap {
        _.map { attendee =>
          Future(Redirect(backend.controllers.routes.Attendees.details(eventId, attendee.uuid)).flashing("success" -> s"Participant '${attendee.name}' créé !"))
        }.getOrElse {
          createView(createForm.fill(formData), eventId, InternalServerError)
        }
      })
  }

  def update(eventId: EventId, attendeeId: AttendeeId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withAttendee(attendeeId) { attendee =>
      updateView(createForm.fill(AttendeeCreateData.fromModel(attendee)), attendee, eventId)
    }
  }

  def doUpdate(eventId: EventId, attendeeId: AttendeeId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withAttendee(attendeeId) { attendee =>
      createForm.bindFromRequest.fold(
        formWithErrors => updateView(formWithErrors, attendee, eventId, BadRequest),
        formData => AttendeeRepository.update(attendeeId, AttendeeCreateData.merge(attendee, formData)).flatMap {
          _.map { attendeeUpdated =>
            Future(Redirect(backend.controllers.routes.Attendees.details(eventId, attendeeId)).flashing("success" -> s"Le participant '${attendeeUpdated.name}' a bien été modifié"))
          }.getOrElse {
            updateView(createForm.fill(formData), attendee, eventId, InternalServerError)
          }
        })
    }
  }

  // TODO : force to manually remove from all exponents/sessions before delete ?
  def doDelete(eventId: EventId, attendeeId: AttendeeId, redirectOpt: Option[String]) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withAttendee(attendeeId) { attendee =>
      for {
        res <- AttendeeRepository.delete(attendeeId)
        res2 <- ExponentRepository.removeFromAllTeams(attendeeId)
      } yield {
        followRedirect(redirectOpt, backend.controllers.routes.Attendees.list(eventId)).flashing("success" -> s"Suppression du profil ${attendee.name}")
      }
    }
  }

  def doFileExport(eventId: EventId) = SecuredAction.async { implicit req =>
    withEvent(eventId) { event =>
      AttendeeRepository.findByEvent(eventId).map { attendees =>
        val filename = event.name + "_attendees.csv"
        val content = FileExporter.makeCsv(attendees.map(_.toBackendExport))
        Ok(content)
          .withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + "\""))
          .as("text/csv")
      }
    }
  }

  /*
   * Private methods
   */

  private def createView(createForm: Form[AttendeeCreateData], eventId: EventId, status: Status = Ok)(implicit req: RequestHeader, user: User): Future[Result] = {
    withEvent(eventId) { event =>
      AttendeeRepository.findEventRoles(event.uuid).map { roles =>
        status(backend.views.html.Events.Attendees.create(createForm, roles, event))
      }
    }
  }

  private def updateView(createForm: Form[AttendeeCreateData], attendee: Attendee, eventId: EventId, status: Status = Ok)(implicit req: RequestHeader, user: User): Future[Result] = {
    withEvent(eventId) { event =>
      AttendeeRepository.findEventRoles(event.uuid).map { roles =>
        status(backend.views.html.Events.Attendees.update(createForm.fill(AttendeeCreateData.fromModel(attendee)), attendee, roles, event))
      }
    }
  }

}
