package backend.controllers

import play.api._
import play.api.mvc._
import common.models.user.User
import authentication.environments.SilhouetteEnvironment
import com.mohiva.play.silhouette.core.Silhouette
import com.mohiva.play.silhouette.contrib.services.CachedCookieAuthenticator

object Application extends Silhouette[User, CachedCookieAuthenticator] with SilhouetteEnvironment {

  def index = SecuredAction { implicit req =>
    Ok(backend.views.html.index())
  }

}
