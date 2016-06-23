package backend.controllers

import common.models.values.typed.UserRole
import common.models.values.typed.Email
import common.models.values.typed.TextMultiline
import common.models.user.User
import common.models.user.UserId
import common.models.user.UserOrganization
import common.models.user.Request
import common.models.user.OrganizationId
import common.models.user.OrganizationInvite
import common.repositories.user.UserRepository
import common.repositories.user.OrganizationRepository
import common.repositories.user.RequestRepository
import common.services.EmailSrv
import backend.forms.OrganizationData
import backend.utils.ControllerHelpers
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._

object Organizations extends SilhouetteEnvironment with ControllerHelpers {
  val organizationForm: Form[OrganizationData] = Form(OrganizationData.fields)
  val organizationInviteForm = Form(tuple("email" -> of[Email], "comment" -> optional(of[TextMultiline])))

  def details(organizationId: OrganizationId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    user.organizationRole(organizationId).map { role =>
      withOrganization(organizationId) { organization =>
        for {
          members <- UserRepository.findOrganizationMembers(organizationId)
          requests <- if (user.canAdministrateOrganization(organizationId)) getOrganizationRequests(organizationId) else Future(List())
          invites <- if (user.canAdministrateOrganization(organizationId)) getOrganizationInvites(organizationId) else Future(List())
        } yield {
          Ok(backend.views.html.Profile.Organizations.details(organization, members.sortBy(m => UserRole.getPriority(m.organizationRole(organizationId))), requests, invites, organizationInviteForm))
        }
      }
    }.getOrElse {
      Future(Redirect(backend.controllers.routes.Profile.details()).flashing("error" -> "Vous n'êtes pas membre de cette organisation."))
    }
  }

  def update(organizationId: OrganizationId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withOrganization(organizationId) { organization =>
      Future(Ok(backend.views.html.Profile.Organizations.update(organizationForm.fill(OrganizationData.fromModel(organization)), organization)))
    }
  }

  def doUpdate(organizationId: OrganizationId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    withOrganization(organizationId) { organization =>
      organizationForm.bindFromRequest.fold(
        formWithErrors => Future(BadRequest(backend.views.html.Profile.Organizations.update(formWithErrors, organization))),
        formData => OrganizationRepository.getByName(formData.name).flatMap { orgOpt =>
          orgOpt.map { org =>
            Future(BadRequest(backend.views.html.Profile.Organizations.update(organizationForm.fill(formData), organization))) // Organization name must be unique
          }.getOrElse {
            OrganizationRepository.update(organizationId, OrganizationData.merge(organization, formData)).map {
              _.map { organizationUpdated =>
                Redirect(backend.controllers.routes.Organizations.details(organizationId)).flashing("success" -> "Organisation mise à jour avec succès")
              }.getOrElse {
                InternalServerError(backend.views.html.Profile.Organizations.update(organizationForm.fill(formData), organization))
              }
            }
          }
        })
    }
  }

  /*
   * TODO :
   * 	- what to do for events related to this organization ?
   *  		-> delete unpublised & assign publised to an other organization
   *    	-> don't allow to delete an organization with events (they should be moved to an other organization before)
   * 	- notify all members that the organization is deleted
   */
  def delete(organizationId: OrganizationId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    if (user.canAdministrateOrganization(organizationId)) {
      withOrganization(organizationId) { organization =>
        Future(Ok(backend.views.html.Profile.Organizations.delete(organization)))
      }
    } else {
      Future(Redirect(backend.controllers.routes.Organizations.details(organizationId)).flashing("error" -> "Vous n'avez pas les droits pour supprimer cette organisation :("))
    }
  }

  def doDelete(organizationId: OrganizationId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    if (user.canAdministrateOrganization(organizationId)) {
      withOrganization(organizationId) { organization =>
        UserRepository.findOrganizationMembers(organizationId).flatMap { members =>
          members.filter(_.uuid != user.uuid).map { u =>
            val emailData = EmailSrv.generateOrganizationDeleteEmail(u, organization, user)
            EmailSrv.sendEmail(emailData)
          }
          OrganizationRepository.delete(organizationId).map { r =>
            Redirect(backend.controllers.routes.Profile.details()).flashing("success" -> "Organisation supprimée !")
          }
        }
      }
    } else {
      Future(Redirect(backend.controllers.routes.Organizations.details(organizationId)).flashing("error" -> "Vous n'avez pas les droits pour supprimer cette organisation :("))
    }
  }

  def doOrganizationInvite(organizationId: OrganizationId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    organizationInviteForm.bindFromRequest.fold(
      formWithErrors => Future(Redirect(backend.controllers.routes.Organizations.details(organizationId)).flashing("error" -> "Email non valide.")),
      formData => {
        if (user.canAdministrateOrganization(organizationId)) {
          organizationInvite(organizationId, formData._1, formData._2, user).map {
            case (category, message) => Redirect(backend.controllers.routes.Organizations.details(organizationId)).flashing(category -> message)
          }
        } else {
          Future(Redirect(backend.controllers.routes.Organizations.details(organizationId)).flashing("error" -> "Vous n'avez pas les droits pour inviter des personnes dans cette organisation :("))
        }
      })
  }

  // when the user leave an organization
  def doLeave(organizationId: OrganizationId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    if (user.canAdministrateOrganization(organizationId)) {
      Future(Redirect(backend.controllers.routes.Organizations.details(organizationId)).flashing("error" -> "Impossible de quitter une organisation dont vous êtes le responsable."))
    } else if (user.organizationRole(organizationId).isDefined) {
      val userWithoutOrg = user.copy(organizationIds = user.organizationIds.filter(_.organizationId != organizationId))
      for {
        organizationOpt <- OrganizationRepository.getByUuid(organizationId)
        organizationOwnerOpt <- UserRepository.getOrganizationOwner(organizationId)
        updatedUserOpt <- UserRepository.update(userWithoutOrg.uuid, userWithoutOrg)
      } yield {
        for {
          organization <- organizationOpt
          organizationOwner <- organizationOwnerOpt
        } yield {
          val emailData = EmailSrv.generateOrganizationLeaveEmail(user, organization, organizationOwner)
          EmailSrv.sendEmail(emailData)
        }
        Redirect(backend.controllers.routes.Profile.details()).flashing("success" -> s"Vous ne faites plus parti de l'organisation ${organizationOpt.map(_.name).getOrElse("")}")
      }
    } else {
      Future(Redirect(backend.controllers.routes.Profile.details()).flashing("error" -> "Vous ne faites pas parti de cette organisation."))
    }
  }

  // when the organization owner ban a member
  def doBan(organizationId: OrganizationId, userId: UserId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    if (user.canAdministrateOrganization(organizationId)) {
      val res: Future[Future[Result]] = for {
        organizationOpt <- OrganizationRepository.getByUuid(organizationId)
        bannedUserOpt <- UserRepository.getByUuid(userId)
      } yield {
        if (bannedUserOpt.isDefined && bannedUserOpt.get.organizationRole(organizationId).isDefined) {
          val bannedUser = bannedUserOpt.get
          val userWithoutOrg = bannedUser.copy(organizationIds = bannedUser.organizationIds.filter(_.organizationId != organizationId))
          UserRepository.update(userWithoutOrg.uuid, userWithoutOrg).map { updatedUserOpt =>
            organizationOpt.map { organization =>
              val emailData = EmailSrv.generateOrganizationBanEmail(bannedUser, organization, user)
              EmailSrv.sendEmail(emailData)
            }
            Redirect(backend.controllers.routes.Organizations.details(organizationId)).flashing("success" -> s"${userWithoutOrg.name()} ne fait plus parti de l'organisation ${organizationOpt.map(_.name).getOrElse("")}")
          }
        } else {
          Future(Redirect(backend.controllers.routes.Organizations.details(organizationId)).flashing("error" -> "L'utilisateur ciblé n'existe pas ou ne fait pas parti de l'organisation."))
        }
      }
      res.flatMap(identity)
    } else {
      Future(Redirect(backend.controllers.routes.Organizations.details(organizationId)).flashing("error" -> "Vous n'avez pas les droits nécessaires."))
    }
  }

  /*
   * Private methods
   */

  private def getOrganizationRequests(organizationId: OrganizationId): Future[List[(Request, User)]] = {
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

  private def getOrganizationInvites(organizationId: OrganizationId): Future[List[(Request, Option[User])]] = {
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

  private def organizationInvite(organizationId: OrganizationId, email: Email, comment: Option[TextMultiline], user: User)(implicit req: RequestHeader): Future[(String, String)] = {
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
        EmailSrv.sendEmail(emailData).map { success =>
          if(success) ("success", s"Invitation à ${organization.name} envoyée !") else ("error", "Problème d'envoi du mail")
        }
      }.getOrElse(Future(("error", "L'organisation demandée n'existe pas :(")))
    }
    res.flatMap(identity)
  }

}
