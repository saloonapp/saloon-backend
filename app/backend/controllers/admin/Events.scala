package backend.controllers.admin

import common.models.user.User
import common.repositories.event.EventRepository
import common.services.EventSrv
import backend.utils.ControllerHelpers
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

object Events extends SilhouetteEnvironment with ControllerHelpers {

  def list = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    if (user.canAdministrateSalooN()) {
      EventRepository.findAll().flatMap { events =>
        EventSrv.addMetadata(events.sortBy(-_.info.start.map(_.getMillis()).getOrElse(9999999999999L))).map { fullEvents =>
          Ok(backend.views.html.Events.list(fullEvents.toList))
        }
      }
    } else {
      Future(Redirect(backend.controllers.routes.Events.list()))
    }
  }

}
