package backend.controllers.eventDirectory

import common.repositories.event.GenericEventRepository
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

object Events extends SilhouetteEnvironment {

  def list = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    GenericEventRepository.findAll().map { events =>
      Ok(backend.views.html.eventDirectory.Events.list(events))
    }
  }

  def details(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    GenericEventRepository.getByUuid(eventId).map { eventOpt =>
      eventOpt.map { event =>
        Ok(backend.views.html.eventDirectory.Events.details(event))
      }.getOrElse {
        Redirect(backend.controllers.eventDirectory.routes.Events.list)
      }
    }
  }

}
