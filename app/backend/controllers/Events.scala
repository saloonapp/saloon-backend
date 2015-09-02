package backend.controllers

import common.models.event.Event
import common.models.user.User
import common.models.user.UserInfo
import common.models.event.AttendeeRegistration
import common.models.event.EventConfigAttendeeSurvey
import common.models.event.EventConfigAttendeeSurveyQuestion
import common.repositories.user.OrganizationRepository
import common.repositories.event.EventRepository
import common.services.EventSrv
import backend.forms.EventCreateData
import backend.utils.ControllerHelpers
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form

object Events extends SilhouetteEnvironment with ControllerHelpers {
  val createForm: Form[EventCreateData] = Form(EventCreateData.fields)
  val registerForm: Form[AttendeeRegistration] = Form(AttendeeRegistration.fields)

  def list = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    EventSrv.findVisibleEvents(user).flatMap { events =>
      EventSrv.addMetadata(events).map { fullEvents =>
        Ok(backend.views.html.Events.list(fullEvents.toList))
      }
    }
  }

  def details(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withEvent(eventId) { event =>
      EventSrv.addMetadata(event).map {
        case (_, attendeeCount, sessionCount, exponentCount, actionCount) =>
          Ok(backend.views.html.Events.details(event, attendeeCount, sessionCount, exponentCount, actionCount))
      }
    }
  }

  def create = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    createView(createForm)
  }

  def doCreate = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    createForm.bindFromRequest.fold(
      formWithErrors => createView(formWithErrors, BadRequest),
      formData => EventRepository.insert(EventCreateData.toModel(formData)).flatMap {
        _.map { event =>
          Future(Redirect(backend.controllers.routes.Events.details(event.uuid)).flashing("success" -> s"Événement '${event.name}' créé !"))
        }.getOrElse {
          createView(createForm.fill(formData), InternalServerError)
        }
      })
  }

  def update(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withEvent(eventId) { event =>
      updateView(createForm.fill(EventCreateData.fromModel(event)), event)
    }
  }

  def doUpdate(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withEvent(eventId) { event =>
      createForm.bindFromRequest.fold(
        formWithErrors => updateView(formWithErrors, event, BadRequest),
        formData => EventRepository.update(eventId, EventCreateData.merge(event, formData)).flatMap {
          _.map { eventUpdated =>
            Future(Redirect(backend.controllers.routes.Events.details(eventId)).flashing("success" -> s"L'événement '${eventUpdated.name}' a bien été modifié"))
          }.getOrElse {
            updateView(createForm.fill(formData), event, InternalServerError)
          }
        })
    }
  }

  def doDelete(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withEvent(eventId) { event =>
      EventRepository.delete(eventId).map { res =>
        Redirect(backend.controllers.routes.Events.list()).flashing("success" -> s"Suppression de l'événement '${event.name}'")
      }
    }
  }

  /*
   * Private methods
   */

  private def createView(createForm: Form[EventCreateData], status: Status = Ok)(implicit req: RequestHeader, user: User): Future[Result] = {
    for {
      organizations <- OrganizationRepository.findAllowed(user)
      categories <- EventRepository.getCategories()
    } yield {
      if (organizations.length > 0) {
        status(backend.views.html.Events.create(createForm, organizations, categories))
      } else {
        Redirect(backend.controllers.routes.Events.list()).flashing("error" -> s"Vous devez appartenir à une organisation pour créer un événement")
      }
    }
  }

  private def updateView(createForm: Form[EventCreateData], event: Event, status: Status = Ok)(implicit req: RequestHeader, user: User): Future[Result] = {
    for {
      organizations <- OrganizationRepository.findAllowed(user)
      categories <- EventRepository.getCategories()
    } yield {
      if (organizations.length > 0) {
        status(backend.views.html.Events.update(createForm, event, organizations, categories))
      } else {
        Redirect(backend.controllers.routes.Events.list()).flashing("error" -> s"Vous devez appartenir à une organisation pour créer un événement")
      }
    }
  }

}
