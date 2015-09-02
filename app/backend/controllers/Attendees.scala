package backend.controllers

import common.models.event.Event
import common.models.event.Attendee
import common.models.user.User
import common.models.utils.Page
import common.services.FileExporter
import common.repositories.event.EventRepository
import common.repositories.event.AttendeeRepository
import common.repositories.event.SessionRepository
import common.repositories.event.ExponentRepository
import backend.utils.ControllerHelpers
import backend.forms.AttendeeCreateData
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form

object Attendees extends SilhouetteEnvironment with ControllerHelpers {
  val createForm: Form[AttendeeCreateData] = Form(AttendeeCreateData.fields)

  def list(eventId: String, query: Option[String], page: Option[Int], pageSize: Option[Int], sort: Option[String]) = SecuredAction.async { implicit req =>
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

  def details(eventId: String, attendeeId: String) = SecuredAction.async { implicit req =>
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

  def create(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withEvent(eventId) { event =>
      createView(createForm, event)
    }
  }

  def doCreate(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withEvent(eventId) { event =>
      createForm.bindFromRequest.fold(
        formWithErrors => createView(formWithErrors, event, BadRequest),
        formData => AttendeeRepository.insert(AttendeeCreateData.toModel(formData)).flatMap {
          _.map { attendee =>
            Future(Redirect(backend.controllers.routes.Attendees.details(eventId, attendee.uuid)).flashing("success" -> s"Participant '${attendee.name}' créé !"))
          }.getOrElse {
            createView(createForm.fill(formData), event, InternalServerError)
          }
        })
    }
  }

  def update(eventId: String, attendeeId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withEvent(eventId) { event =>
      withAttendee(attendeeId) { attendee =>
        updateView(createForm.fill(AttendeeCreateData.fromModel(attendee)), attendee, event)
      }
    }
  }

  def doUpdate(eventId: String, attendeeId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withEvent(eventId) { event =>
      withAttendee(attendeeId) { attendee =>
        createForm.bindFromRequest.fold(
          formWithErrors => updateView(formWithErrors, attendee, event, BadRequest),
          formData => AttendeeRepository.update(attendeeId, AttendeeCreateData.merge(attendee, formData)).flatMap {
            _.map { updatedElt =>
              Future(Redirect(backend.controllers.routes.Attendees.details(eventId, updatedElt.uuid)).flashing("success" -> s"Le participant '${updatedElt.name}' a bien été modifié"))
            }.getOrElse {
              updateView(createForm.fill(formData), attendee, event, InternalServerError)
            }
          })
      }
    }
  }

  // TODO : force to manually remove from all exponents/sessions before delete ?
  def doDelete(eventId: String, attendeeId: String, redirectOpt: Option[String]) = SecuredAction.async { implicit req =>
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

  def fileExport(eventId: String) = SecuredAction.async { implicit req =>
    withEvent(eventId) { event =>
      AttendeeRepository.findByEvent(eventId).map { elts =>
        val filename = event.name + "_attendees.csv"
        val content = FileExporter.makeCsv(elts.map(_.toBackendExport))
        Ok(content)
          .withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + "\""))
          .as("text/csv")
      }
    }
  }

  /*
   * Private methods
   */

  private def createView(createForm: Form[AttendeeCreateData], event: Event, status: Status = Ok)(implicit req: RequestHeader, user: User): Future[Result] = {
    AttendeeRepository.findEventRoles(event.uuid).map { roles =>
      status(backend.views.html.Events.Attendees.create(createForm, roles, event))
    }
  }

  private def updateView(createForm: Form[AttendeeCreateData], attendee: Attendee, event: Event, status: Status = Ok)(implicit req: RequestHeader, user: User): Future[Result] = {
    AttendeeRepository.findEventRoles(event.uuid).map { roles =>
      status(backend.views.html.Events.Attendees.update(createForm.fill(AttendeeCreateData.fromModel(attendee)), attendee, roles, event))
    }
  }

}
