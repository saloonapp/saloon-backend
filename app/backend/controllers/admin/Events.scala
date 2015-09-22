package backend.controllers.admin

import common.models.values.typed.WebsiteUrl
import common.models.user.User
import common.models.user.OrganizationId
import common.models.event.Event
import common.models.event.EventId
import common.models.event.GenericEvent
import common.repositories.event.EventRepository
import common.repositories.event.AttendeeRepository
import common.repositories.event.ExponentRepository
import common.repositories.event.SessionRepository
import common.repositories.user.OrganizationRepository
import common.services.EventSrv
import backend.utils.ControllerHelpers
import backend.services.EventImport
import authentication.environments.SilhouetteEnvironment
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import reactivemongo.core.commands.LastError

object Events extends SilhouetteEnvironment with ControllerHelpers {
  val eventImportUrlForm = Form(tuple(
    "organizationId" -> of[OrganizationId],
    "importUrl" -> of[WebsiteUrl]))
  val eventImportDataForm = Form(tuple(
    "organizationId" -> of[OrganizationId],
    "importData" -> nonEmptyText))
  val refreshForm = Form(single(
    "data" -> nonEmptyText))

  def list = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    if (user.canAdministrateSalooN()) {
      EventRepository.findAll().flatMap { events =>
        EventSrv.addMetadata(events.sortBy(-_.info.start.map(_.getMillis()).getOrElse(9999999999999L))).map { fullEvents =>
          Ok(backend.views.html.Events.list(fullEvents.toList))
        }
      }
    } else {
      Future(Redirect(backend.controllers.routes.Application.index()))
    }
  }

  def urlImport = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    if (user.canAdministrateSalooN()) {
      urlImportView(eventImportUrlForm, eventImportDataForm)
    } else {
      Future(Redirect(backend.controllers.routes.Application.index()))
    }
  }

  def doUrlImport = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    if (user.canAdministrateSalooN()) {
      eventImportUrlForm.bindFromRequest.fold(
        formWithErrors => urlImportView(formWithErrors, eventImportDataForm, BadRequest),
        formData => {
          val organizationId = formData._1
          val importUrl = formData._2
          EventImport.withGenericEvent(importUrl) { eventFull =>
            EventRepository.getBySources(eventFull.sources).flatMap {
              _.map { event =>
                refreshView(refreshForm.fill(Json.stringify(Json.toJson(eventFull))), eventFull, event)
              }.getOrElse {
                EventImport.create(eventFull, organizationId, Some(importUrl)).map { eventId =>
                  Redirect(backend.controllers.routes.Events.details(eventId)).flashing("success" -> "Félicitations, votre événement vient d'être importé avec succès !")
                }
              }
            }
          }
        })
    } else {
      Future(Redirect(backend.controllers.routes.Application.index()))
    }
  }

  def doDataImport = SecuredAction.async(parse.urlFormEncoded(maxLength = 1024 * 1000)) { implicit req =>
    implicit val user = req.identity
    if (user.canAdministrateSalooN()) {
      eventImportDataForm.bindFromRequest.fold(
        formWithErrors => urlImportView(eventImportUrlForm, formWithErrors, BadRequest),
        formData => {
          val organizationId = formData._1
          val importData = formData._2
          Try(Json.parse(importData)).toOption.flatMap(_.asOpt[GenericEvent]).map { eventFull =>
            EventRepository.getBySources(eventFull.sources).flatMap {
              _.map { event =>
                refreshView(refreshForm.fill(Json.stringify(Json.toJson(eventFull))), eventFull, event)
              }.getOrElse {
                EventImport.create(eventFull, organizationId, None).map { eventId =>
                  Redirect(backend.controllers.routes.Events.details(eventId)).flashing("success" -> "Félicitations, votre événement vient d'être importé avec succès !")
                }
              }
            }
          }.getOrElse {
            Future(Redirect(backend.controllers.admin.routes.Events.urlImport()).flashing("error" -> s"Your JSON is not formatted as a GenericEvent instance..."))
          }
        })
    } else {
      Future(Redirect(backend.controllers.routes.Application.index()))
    }
  }

  def refresh(eventId: EventId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    if (user.canAdministrateSalooN()) {
      withEvent(eventId) { event =>
        event.meta.refreshUrl.map { refreshUrl =>
          EventImport.fetchGenericEvent(refreshUrl).flatMap { eventFullOpt =>
            eventFullOpt.map { eventFull =>
              refreshView(refreshForm.fill(Json.stringify(Json.toJson(eventFull))), eventFull, event)
            }.getOrElse {
              Future(Redirect(backend.controllers.routes.Events.details(eventId)).flashing("error" -> "Impossible de mettre à jour l'événement :("))
            }
          }
        }.getOrElse {
          Future(Redirect(backend.controllers.routes.Events.details(eventId)).flashing("error" -> "Impossible de mettre à jour l'événement :("))
        }
      }
    } else {
      Future(Redirect(backend.controllers.routes.Application.index()))
    }
  }

  def doRefresh(eventId: EventId) = SecuredAction.async(parse.urlFormEncoded(maxLength = 1024 * 1000)) { implicit req =>
    implicit val user = req.identity
    if (user.canAdministrateSalooN()) {
      refreshForm.bindFromRequest.fold(
        formWithErrors => Future(Redirect(backend.controllers.admin.routes.Events.refresh(eventId)).flashing("error" -> "Format de données incorrect...")),
        formData => {
          Try(Json.parse(formData).as[GenericEvent]) match {
            case Success(eventFull) => {
              withEvent(eventId) { event =>
                val res: Future[Future[Result]] = for {
                  attendees <- AttendeeRepository.findByEvent(event.uuid)
                  exponents <- ExponentRepository.findByEvent(event.uuid)
                  sessions <- SessionRepository.findByEvent(event.uuid)
                } yield {
                  val (updatedEvent, createdAttendees, deletedAttendees, updatedAttendees, createdExponents, deletedExponents, updatedExponents, createdSessions, deletedSessions, updatedSessions) = EventImport.makeDiff(eventFull, event, attendees, exponents, sessions)
                  val err = new LastError(true, None, None, None, None, 0, false)
                  for {
                    eventUpdated <- EventRepository.update(event.uuid, updatedEvent)
                    attendeesCreated <- if (createdAttendees.length > 0) { AttendeeRepository.bulkInsert(createdAttendees) } else { Future(0) }
                    attendeesDeleted <- if (deletedAttendees.length > 0) { AttendeeRepository.bulkDelete(deletedAttendees.map(_.uuid)) } else { Future(err) }
                    attendeesUpdated <- if (updatedAttendees.length > 0) { AttendeeRepository.bulkUpdate(updatedAttendees.map(s => (s._2.uuid, s._2))) } else { Future(0) }
                    exponentsCreated <- if (createdExponents.length > 0) { ExponentRepository.bulkInsert(createdExponents) } else { Future(0) }
                    exponentsDeleted <- if (deletedExponents.length > 0) { ExponentRepository.bulkDelete(deletedExponents.map(_.uuid)) } else { Future(err) }
                    exponentsUpdated <- if (updatedExponents.length > 0) { ExponentRepository.bulkUpdate(updatedExponents.map(e => (e._2.uuid, e._2))) } else { Future(0) }
                    sessionsCreated <- if (createdSessions.length > 0) { SessionRepository.bulkInsert(createdSessions) } else { Future(0) }
                    sessionsDeleted <- if (deletedSessions.length > 0) { SessionRepository.bulkDelete(deletedSessions.map(_.uuid)) } else { Future(err) }
                    sessionsUpdated <- if (updatedSessions.length > 0) { SessionRepository.bulkUpdate(updatedSessions.map(s => (s._2.uuid, s._2))) } else { Future(0) }
                  } yield {
                    Redirect(backend.controllers.routes.Events.details(eventId)).flashing("success" ->
                      (s"${updatedEvent.name} updated :" +
                        s"Attendees: $attendeesCreated/$attendeesUpdated/${deletedAttendees.length}, " +
                        s"Exponents: $exponentsCreated/$exponentsUpdated/${deletedExponents.length}, " +
                        s"Sessions: $sessionsCreated/$sessionsUpdated/${deletedSessions.length} (create/update/delete)"))
                  }
                }
                res.flatMap(identity)
              }
            }
            case Failure(e) => Future(Redirect(backend.controllers.admin.routes.Events.refresh(eventId)).flashing("error" -> "Format de données incorrect..."))
          }
        })
    } else {
      Future(Redirect(backend.controllers.routes.Application.index()))
    }
  }

  /*
   * Private methods
   */

  private def urlImportView(eventImportUrlForm: Form[(OrganizationId, WebsiteUrl)], eventImportDataForm: Form[(OrganizationId, String)], status: Status = Ok)(implicit req: RequestHeader, user: User): Future[Result] = {
    for {
      organizations <- OrganizationRepository.findAllowed(user)
    } yield {
      status(backend.views.html.admin.Events.urlImport(eventImportUrlForm, eventImportDataForm, organizations))
    }
  }

  private def refreshView(refreshForm: Form[String], eventFull: GenericEvent, event: Event, status: Status = Ok)(implicit req: RequestHeader, user: User): Future[Result] = {
    for {
      attendees <- AttendeeRepository.findByEvent(event.uuid)
      exponents <- ExponentRepository.findByEvent(event.uuid)
      sessions <- SessionRepository.findByEvent(event.uuid)
    } yield {
      val (updatedEvent, createdAttendees, deletedAttendees, updatedAttendees, createdExponents, deletedExponents, updatedExponents, createdSessions, deletedSessions, updatedSessions) =
        EventImport.makeDiff(eventFull, event, attendees, exponents, sessions)
      status(backend.views.html.admin.Events.refresh(refreshForm, event, updatedEvent, createdAttendees, deletedAttendees, updatedAttendees, createdExponents, deletedExponents, updatedExponents, createdSessions, deletedSessions, updatedSessions))
    }
  }

}
