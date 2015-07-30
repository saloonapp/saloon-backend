package backend.controllers

import common.models.user.User
import common.models.user.UserInfo
import common.repositories.event.EventRepository
import common.services.EventSrv
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import com.mohiva.play.silhouette.core.LoginInfo

object Profile extends SilhouetteEnvironment {
  
  def details = Action { implicit req =>
    //implicit val user = req.identity
    implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    Ok(backend.views.html.Profile.details())
  }

}
