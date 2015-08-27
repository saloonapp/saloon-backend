package backend.controllers

import common.models.user.User
import common.models.user.UserInfo
import common.models.user.UserOrganization
import common.models.user.Request
import common.models.user.OrganizationInvite
import common.repositories.user.UserRepository
import common.repositories.user.OrganizationRepository
import common.repositories.user.RequestRepository
import common.services.EmailSrv
import common.services.MandrillSrv
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import com.mohiva.play.silhouette.core.LoginInfo

object Organizations extends SilhouetteEnvironment {
  val organizationInviteForm = Form(tuple("email" -> email, "comment" -> optional(text)))

  def details(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(organizationIds = List(UserOrganization("9fe5b3d4-714c-4c87-821a-677d57a314b7", "owner")), loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    user.organizationRole(uuid).map { role =>
      for {
        organizationOpt <- OrganizationRepository.getByUuid(uuid)
        members <- UserRepository.findOrganizationMembers(uuid)
        requests <- if (user.canAdministrateOrganization(uuid)) getOrganizationRequests(uuid) else Future(List())
        invites <- if (user.canAdministrateOrganization(uuid)) getOrganizationInvites(uuid) else Future(List())
      } yield {
        organizationOpt.map { organization =>
          Ok(backend.views.html.Profile.Organizations.details(organization, members.sortBy(m => UserOrganization.getPriority(m.organizationRole(uuid))), requests, invites, organizationInviteForm))
        }.getOrElse {
          Redirect(backend.controllers.routes.Profile.details()).flashing("error" -> "L'organisation demandée n'existe pas.")
        }
      }
    }.getOrElse {
      Future(Redirect(backend.controllers.routes.Profile.details()).flashing("error" -> "Vous n'êtes pas membre de cette organisation."))
    }
  }

  def doOrganizationInvite(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    organizationInviteForm.bindFromRequest.fold(
      formWithErrors => Future(Redirect(backend.controllers.routes.Organizations.details(uuid)).flashing("error" -> "Email non valide.")),
      formData => {
        if (user.canAdministrateOrganization(uuid)) {
          organizationInvite(uuid, formData._1, formData._2, user).map {
            case (category, message) => Redirect(backend.controllers.routes.Organizations.details(uuid)).flashing(category -> message)
          }
        } else {
          Future(Redirect(backend.controllers.routes.Organizations.details(uuid)).flashing("error" -> "Vous n'avez pas les droits pour inviter des personnes dans cette organisation :("))
        }
      })
  }

  /*
   * TODO :
   * 	- what to do for events related to this organization ?
   *  		-> delete unpublised & assign publised to an other organization
   *    	-> don't allow to delete an organization with events (they should be moved to an other organization before)
   * 	- notify all members that the organization is deleted
   */
  def delete(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    if (user.canAdministrateOrganization(uuid)) {
      OrganizationRepository.getByUuid(uuid).map { organizationOpt =>
        organizationOpt.map { organization =>
          Ok(backend.views.html.Profile.Organizations.delete(organization))
        }.getOrElse {
          Redirect(backend.controllers.routes.Profile.details()).flashing("error" -> "L'organisation demandée n'existe pas.")
        }
      }
    } else {
      Future(Redirect(backend.controllers.routes.Organizations.details(uuid)).flashing("error" -> "Vous n'avez pas les droits pour supprimer cette organisation :("))
    }
  }

  def doDelete(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    if (user.canAdministrateOrganization(uuid)) {
      OrganizationRepository.delete(uuid).map { res =>
        Redirect(backend.controllers.routes.Profile.details()).flashing("success" -> "Organisation supprimée !")
      }
    } else {
      Future(Redirect(backend.controllers.routes.Organizations.details(uuid)).flashing("error" -> "Vous n'avez pas les droits pour supprimer cette organisation :("))
    }
  }

  private def getOrganizationRequests(organizationId: String): Future[List[(Request, User)]] = {
    for {
      requests <- RequestRepository.findPendingOrganizationRequestsByOrganization(organizationId)
      users <- UserRepository.findByUuids(requests.map(_.userId).flatten)
    } yield {
      requests.map { r =>
        r.userId.flatMap { userId => users.find(_.uuid == userId) }.map { user =>
          (r, user)
        }
      }.flatten
    }
  }

  private def getOrganizationInvites(organizationId: String): Future[List[(Request, Option[User])]] = {
    for {
      requests <- RequestRepository.findPendingOrganizationInvitesByOrganization(organizationId)
      users <- UserRepository.findByEmails(requests.map {
        _.content match {
          case OrganizationInvite(_, email, _, _) => Some(email)
          case _ => None
        }
      }.flatten)
    } yield {
      requests.map { r =>
        r.content match {
          case OrganizationInvite(_, email, _, _) => (r, users.find(_.email == email))
          case _ => (r, None)
        }
      }
    }
  }

  private def organizationInvite(organizationId: String, email: String, comment: Option[String], user: User)(implicit req: RequestHeader): Future[(String, String)] = {
    val request = Request.organizationInvite(organizationId, email, comment, user)
    val res = for {
      organizationOpt <- OrganizationRepository.getByUuid(organizationId)
      userOpt <- UserRepository.getByEmail(email)
      requestErr <- RequestRepository.insert(request)
    } yield {
      organizationOpt.map { organization =>
        val emailData = userOpt.map { invitedUser =>
          EmailSrv.generateOrganizationInviteEmail(user, organization, invitedUser, request)
        }.getOrElse {
          val requestInvite = Request.accountInvite(email, Some(request.uuid), user)
          RequestRepository.insert(requestInvite) // send SalooN invite only if user doesn't exists
          EmailSrv.generateOrganizationAndSalooNInviteEmail(user, organization, email, comment, requestInvite)
        }
        MandrillSrv.sendEmail(emailData).map { res =>
          ("success", s"Demande d'accès à ${organization.name} envoyée !")
        }
      }.getOrElse(Future(("error", "L'organisation demandée n'existe pas :(")))
    }
    res.flatMap(identity)
  }

}
