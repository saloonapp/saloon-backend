package backend.controllers.eventDirectory

import authentication.environments.SilhouetteEnvironment
import play.api.mvc._

object Events extends SilhouetteEnvironment {

  def list = SecuredAction { implicit req =>
    implicit val user = req.identity
    Ok(backend.views.html.eventDirectory.Events.list())
  }

}
