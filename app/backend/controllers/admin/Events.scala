package backend.controllers.admin

import common.models.values.typed.WebsiteUrl
import common.models.user.User
import common.models.user.OrganizationId
import common.repositories.event.EventRepository
import common.repositories.user.OrganizationRepository
import common.services.EventSrv
import backend.utils.ControllerHelpers
import backend.services.EventImport
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

object Events extends SilhouetteEnvironment with ControllerHelpers {
  val eventImportForm = Form(tuple(
    "organizationId" -> of[OrganizationId],
    "importUrl" -> of[WebsiteUrl]))

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
      urlImportView(eventImportForm)
    } else {
      Future(Redirect(backend.controllers.routes.Application.index()))
    }
  }

  def doUrlImport = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    if (user.canAdministrateSalooN()) {
      eventImportForm.bindFromRequest.fold(
        formWithErrors => urlImportView(formWithErrors, BadRequest),
        formData => {
          val organizationId = formData._1
          val importUrl = formData._2
          EventImport.fetchGenericEvent(importUrl) { eventFull =>
            EventRepository.getBySource(eventFull.event.source).flatMap {
              _.map { event =>
                // TODO update event
                Future(Ok(s"should update event ${event.name}"))
              }.getOrElse {
                EventImport.create(eventFull, organizationId, importUrl).map { eventId =>
                  Redirect(backend.controllers.routes.Events.details(eventId))
                }
              }
            }
          }
        })
    } else {
      Future(Redirect(backend.controllers.routes.Application.index()))
    }
  }

  private def urlImportView(eventImportForm: Form[(OrganizationId, WebsiteUrl)], status: Status = Ok)(implicit req: RequestHeader, user: User): Future[Result] = {
    for {
      organizations <- OrganizationRepository.findAllowed(user)
    } yield {
      status(backend.views.html.admin.Events.urlImport(eventImportForm, organizations))
    }
  }

}
