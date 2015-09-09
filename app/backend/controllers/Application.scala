package backend.controllers

import common.models.values.typed.Email
import common.models.values.typed.FirstName
import common.models.values.typed.LastName
import common.models.user.User
import common.models.user.UserInfo
import com.mohiva.play.silhouette.core.LoginInfo
import authentication.environments.SilhouetteEnvironment
import play.api.mvc._

object Application extends SilhouetteEnvironment {

  def index = SecuredAction { implicit req =>
    Redirect(backend.controllers.routes.Events.list()).flashing(req.flash)
  }

  def welcome = SecuredAction { implicit req =>
    implicit val user = req.identity
    // TODO : customiser le welcome en fonction du profil : orga, exposant, speaker, visiteur
    Ok(backend.views.html.Profile.welcome())
  }

  def mockups = Action { implicit req =>
    implicit val user = User(loginInfo = LoginInfo("", ""), email = Email("loicknuchel@gmail.com"), info = UserInfo(FirstName("Loïc"), LastName("Knuchel")), rights = Map("administrateSaloon" -> true))
    Ok(backend.views.html.mockups.list())
  }

  def mockupActivityWall = Action { implicit req =>
    implicit val user = User(loginInfo = LoginInfo("", ""), email = Email("loicknuchel@gmail.com"), info = UserInfo(FirstName("Loïc"), LastName("Knuchel")), rights = Map("administrateSaloon" -> true))
    Ok(backend.views.html.mockups.activityWall())
  }

  def mockupExponentForm = Action { implicit req =>
    implicit val user = User(loginInfo = LoginInfo("", ""), email = Email("loicknuchel@gmail.com"), info = UserInfo(FirstName("Loïc"), LastName("Knuchel")), rights = Map("administrateSaloon" -> true))
    Ok(backend.views.html.mockups.exponentForm())
  }

  def mockupLeads = Action { implicit req =>
    implicit val user = User(loginInfo = LoginInfo("", ""), email = Email("loicknuchel@gmail.com"), info = UserInfo(FirstName("Loïc"), LastName("Knuchel")), rights = Map("administrateSaloon" -> true))
    Ok(backend.views.html.mockups.leads())
  }

  def mockupRegister1 = Action { implicit req =>
    implicit val user = User(loginInfo = LoginInfo("", ""), email = Email("loicknuchel@gmail.com"), info = UserInfo(FirstName("Loïc"), LastName("Knuchel")), rights = Map("administrateSaloon" -> true))
    Ok(backend.views.html.mockups.register1())
  }

  def mockupRegister2 = Action { implicit req =>
    implicit val user = User(loginInfo = LoginInfo("", ""), email = Email("loicknuchel@gmail.com"), info = UserInfo(FirstName("Loïc"), LastName("Knuchel")), rights = Map("administrateSaloon" -> true))
    Ok(backend.views.html.mockups.register2())
  }

  def mockupRegister3 = Action { implicit req =>
    implicit val user = User(loginInfo = LoginInfo("", ""), email = Email("loicknuchel@gmail.com"), info = UserInfo(FirstName("Loïc"), LastName("Knuchel")), rights = Map("administrateSaloon" -> true))
    Ok(backend.views.html.mockups.register3())
  }

  def mockupScannedAttendees = Action { implicit req =>
    implicit val user = User(loginInfo = LoginInfo("", ""), email = Email("loicknuchel@gmail.com"), info = UserInfo(FirstName("Loïc"), LastName("Knuchel")), rights = Map("administrateSaloon" -> true))
    Ok(backend.views.html.mockups.scannedAttendees())
  }

  def mockupScannedDocuments = Action { implicit req =>
    implicit val user = User(loginInfo = LoginInfo("", ""), email = Email("loicknuchel@gmail.com"), info = UserInfo(FirstName("Loïc"), LastName("Knuchel")), rights = Map("administrateSaloon" -> true))
    Ok(backend.views.html.mockups.scannedDocuments())
  }

}
