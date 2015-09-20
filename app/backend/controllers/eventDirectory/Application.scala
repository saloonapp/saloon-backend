package backend.controllers.eventDirectory

import authentication.environments.SilhouetteEnvironment
import play.api.mvc._

object Application extends SilhouetteEnvironment {

  def index = SecuredAction { implicit req =>
    Redirect(backend.controllers.eventDirectory.routes.Events.list()).flashing(req.flash)
  }

}
