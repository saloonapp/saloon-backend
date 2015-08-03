package backend.controllers

import common.models.user.User
import common.models.user.UserInfo
import common.models.user.UserOrganization
import common.models.user.Request
import common.repositories.user.UserRepository
import common.repositories.user.OrganizationRepository
import common.repositories.user.RequestRepository
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import com.mohiva.play.silhouette.core.LoginInfo

object Organizations extends SilhouetteEnvironment {

  def details(uuid: String) = Action.async { implicit req =>
    //implicit val user = req.identity
    implicit val user = User(organizationIds = List(UserOrganization("9fe5b3d4-714c-4c87-821a-677d57a314b7", "owner")), loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    user.organizationRole(uuid).map { role =>
      for {
        organizationOpt <- OrganizationRepository.getByUuid(uuid)
        members <- UserRepository.findOrganizationMembers(uuid)
        requests <- if (user.canAdministrateOrganization(uuid)) getOrganizationRequests(uuid) else Future(List())
      } yield {
        organizationOpt.map { organization =>
          Ok(backend.views.html.Profile.Organizations.details(organization, members, requests))
        }.getOrElse {
          Redirect(backend.controllers.routes.Profile.details())
        }
      }
    }.getOrElse {
      Future(Redirect(backend.controllers.routes.Profile.details()))
    }
  }

  private def getOrganizationRequests(organizationId: String): Future[List[(User, Request)]] = {
    for {
      requests <- RequestRepository.findPendingOrganizationRequestsByOrganization(organizationId)
      users <- UserRepository.findByUuids(requests.map(_.userId).flatten)
    } yield {
      requests.map { r =>
        r.userId.flatMap { userId => users.find(_.uuid == userId) }.map { user =>
          (user, r)
        }
      }.flatten
    }
  }

}
