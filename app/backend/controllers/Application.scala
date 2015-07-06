package backend.controllers

import play.api._
import play.api.mvc._
import common.models.user.User
import common.models.user.UserInfo
import authentication.environments.SilhouetteEnvironment
import com.mohiva.play.silhouette.core.Silhouette
import com.mohiva.play.silhouette.contrib.services.CachedCookieAuthenticator
import com.mohiva.play.silhouette.core.LoginInfo

object Application extends SilhouetteEnvironment {

  def index = SecuredAction { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"))
    Ok(backend.views.html.index())
  }

}
