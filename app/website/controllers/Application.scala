package website.controllers

import play.api._
import play.api.mvc._
import common.models.user.User
import authentication.environments.SilhouetteEnvironment
import com.mohiva.play.silhouette.core.Silhouette
import com.mohiva.play.silhouette.contrib.services.CachedCookieAuthenticator

object Application extends Silhouette[User, CachedCookieAuthenticator] with SilhouetteEnvironment {

  def index = UserAwareAction { implicit req =>
    Ok(website.views.html.index(req.identity))
  }

  def sample = Action { implicit req =>
    Ok(website.views.html.sample.index())
  }

}
