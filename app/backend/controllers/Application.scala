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

object Application extends SilhouetteEnvironment {

  def index = SecuredAction { implicit req =>
    Redirect(backend.controllers.routes.Events.list())
  }

  def welcome = SecuredAction { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    Ok(backend.views.html.User.welcome())
  }

  def mockups = Action { implicit req =>
    implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    Ok(backend.views.html.mockups.list())
  }

  def mockupActivityWall = Action { implicit req =>
    implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    Ok(backend.views.html.mockups.activityWall())
  }

  def mockupExponentForm = Action { implicit req =>
    implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    Ok(backend.views.html.mockups.exponentForm())
  }

  def mockupLeads = Action { implicit req =>
    implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    Ok(backend.views.html.mockups.leads())
  }

  def mockupRegister1 = Action { implicit req =>
    implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    Ok(backend.views.html.mockups.register1())
  }

  def mockupRegister2 = Action { implicit req =>
    implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    Ok(backend.views.html.mockups.register2())
  }

  def mockupRegister3 = Action { implicit req =>
    implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    Ok(backend.views.html.mockups.register3())
  }

  def mockupScannedAttendees = Action { implicit req =>
    implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    Ok(backend.views.html.mockups.scannedAttendees())
  }

  def mockupScannedDocuments = Action { implicit req =>
    implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    Ok(backend.views.html.mockups.scannedDocuments())
  }

}
