package backend.controllers.admin

import backend.forms.EventUpdateData
import common.models.values.typed.WebsiteUrl
import common.models.user.User
import common.models.user.OrganizationId
import common.models.event._
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
import reactivemongo.api.commands.{MultiBulkWriteResult, DefaultWriteResult}

object Events extends SilhouetteEnvironment with ControllerHelpers {
  val eventImportUrlForm = Form(tuple(
    "organizationId" -> of[OrganizationId],
    "importUrl" -> of[WebsiteUrl]))
  val eventImportDataForm = Form(tuple(
    "organizationId" -> of[OrganizationId],
    "importData" -> nonEmptyText))
  val refreshForm = Form(EventUpdateData.fields)

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
                refreshView(refreshForm, eventFull, event)
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
                refreshView(refreshForm, eventFull, event)
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
              refreshView(refreshForm, eventFull, event)
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

  def doRefresh(eventId: EventId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    if (user.canAdministrateSalooN()) {
      refreshForm.bindFromRequest.fold(
        formWithErrors => Future(Redirect(backend.controllers.admin.routes.Events.refresh(eventId)).flashing("error" -> "Format de données incorrect...")),
        formData => {
          withEvent(eventId) { event =>
            event.meta.refreshUrl.map { refreshUrl =>
              EventImport.fetchGenericEvent(refreshUrl).flatMap { eventFullOpt =>
                eventFullOpt.map { eventFull =>
                  (for {
                    attendees <- AttendeeRepository.findByEvent(event.uuid)
                    exponents <- ExponentRepository.findByEvent(event.uuid)
                    sessions <- SessionRepository.findByEvent(event.uuid)
                  } yield {
                    val diff = EventImport.makeDiff(eventFull, event, attendees, exponents, sessions)
                    val toUpdate = diff.filterWith(formData)
                    val res = DefaultWriteResult(ok = true, n = 0, writeErrors = Seq(), writeConcernError = None, code = None, errmsg = None)
                    val res2 = MultiBulkWriteResult(ok = true, n = 0, nModified = 0, upserted = Seq(), writeErrors = Seq(), writeConcernError = None, code = None, errmsg = None, totalN = 0)
                    for { // TODO : use EventDiff.persist() instead...
                      eventUpdated <- if(formData.updateEvent){ EventRepository.update(event.uuid, toUpdate.newEvent) } else { Future(None) }
                      attendeesCreated <- if (toUpdate.createdAttendees.length > 0) { AttendeeRepository.bulkInsert(toUpdate.createdAttendees) } else { Future(res2) }
                      attendeesDeleted <- if (toUpdate.deletedAttendees.length > 0) { AttendeeRepository.bulkDelete(toUpdate.deletedAttendees.map(_.uuid)) } else { Future(res) }
                      attendeesUpdated <- if (toUpdate.updatedAttendees.length > 0) { AttendeeRepository.bulkUpdate(toUpdate.updatedAttendees.map(s => (s._2.uuid, s._2))) } else { Future(0) }
                      exponentsCreated <- if (toUpdate.createdExponents.length > 0) { ExponentRepository.bulkInsert(toUpdate.createdExponents) } else { Future(res2) }
                      exponentsDeleted <- if (toUpdate.deletedExponents.length > 0) { ExponentRepository.bulkDelete(toUpdate.deletedExponents.map(_.uuid)) } else { Future(res) }
                      exponentsUpdated <- if (toUpdate.updatedExponents.length > 0) { ExponentRepository.bulkUpdate(toUpdate.updatedExponents.map(e => (e._2.uuid, e._2))) } else { Future(0) }
                      sessionsCreated  <- if  (toUpdate.createdSessions.length > 0) {  SessionRepository.bulkInsert(toUpdate.createdSessions) } else { Future(res2) }
                      sessionsDeleted  <- if  (toUpdate.deletedSessions.length > 0) {  SessionRepository.bulkDelete(toUpdate.deletedSessions.map(_.uuid)) } else { Future(res) }
                      sessionsUpdated  <- if  (toUpdate.updatedSessions.length > 0) {  SessionRepository.bulkUpdate(toUpdate.updatedSessions.map(s => (s._2.uuid, s._2))) } else { Future(0) }
                    } yield {
                      Redirect(backend.controllers.routes.Events.details(eventId)).flashing("success" ->
                        (s"${toUpdate.newEvent.name} updated : " +
                          s"Attendees: ${attendeesCreated.n}/$attendeesUpdated/${attendeesDeleted.n}, " +
                          s"Exponents: ${exponentsCreated.n}/$exponentsUpdated/${exponentsDeleted.n}, " +
                          s"Sessions: ${sessionsCreated.n}/$sessionsUpdated/${sessionsDeleted.n} (create/update/delete)"))
                    }
                  }).flatMap(identity)
                }.getOrElse {
                  Future(Redirect(backend.controllers.routes.Events.details(eventId)).flashing("error" -> "Impossible de mettre à jour l'événement :("))
                }
              }
            }.getOrElse {
              Future(Redirect(backend.controllers.routes.Events.details(eventId)).flashing("error" -> "Impossible de mettre à jour l'événement :("))
            }
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

  private def refreshView(refreshForm: Form[EventUpdateData], eventFull: GenericEvent, event: Event, status: Status = Ok)(implicit req: RequestHeader, user: User): Future[Result] = {
    for {
      attendees <- AttendeeRepository.findByEvent(event.uuid)
      exponents <- ExponentRepository.findByEvent(event.uuid)
      sessions <- SessionRepository.findByEvent(event.uuid)
    } yield {
      val diff = EventImport.makeDiff(eventFull, event, attendees, exponents, sessions)
      status(backend.views.html.admin.Events.refresh(refreshForm.fill(diff.toUpdateForm()), diff))
    }
  }
}
