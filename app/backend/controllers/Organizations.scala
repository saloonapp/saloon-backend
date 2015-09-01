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
import backend.forms.OrganizationData
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import com.mohiva.play.silhouette.core.LoginInfo

object Organizations extends SilhouetteEnvironment {
  val organizationForm: Form[OrganizationData] = Form(OrganizationData.fields)
  val organizationInviteForm = Form(tuple("email" -> email, "comment" -> optional(text)))

  def details(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
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

  def update(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    OrganizationRepository.getByUuid(uuid).map { organizationOpt =>
      organizationOpt.map { organization =>
        Ok(backend.views.html.Profile.Organizations.update(organizationForm.fill(OrganizationData.fromModel(organization)), organization))
      }.getOrElse {
        Redirect(backend.controllers.routes.Profile.details()).flashing("error" -> "L'organisation demandée n'existe pas.")
      }
    }
  }

  def doUpdate(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    OrganizationRepository.getByUuid(uuid).flatMap { organizationOpt =>
      organizationOpt.map { organization =>
        organizationForm.bindFromRequest.fold(
          formWithErrors => Future(BadRequest(backend.views.html.Profile.Organizations.update(formWithErrors, organization))),
          formData => OrganizationRepository.getByName(formData.name).flatMap { orgOpt =>
            orgOpt.map { org =>
              Future(BadRequest(backend.views.html.Profile.Organizations.update(organizationForm.fill(formData), organization)))
            }.getOrElse {
              OrganizationRepository.update(uuid, OrganizationData.merge(organization, formData)).map {
                _.map { updatedElt =>
                  Redirect(backend.controllers.routes.Organizations.details(uuid)).flashing("success" -> "Organisation mise à jour avec succès")
                }.getOrElse {
                  InternalServerError(backend.views.html.Profile.Organizations.update(organizationForm.fill(formData), organization)).flashing("error" -> "Erreur lors de la modification de l'organisation")
                }
              }
            }
          })
      }.getOrElse {
        Future(Redirect(backend.controllers.routes.Profile.details()).flashing("error" -> "L'organisation demandée n'existe pas."))
      }
    }
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
    if (user.canAdministrateOrganization(uuid)) {
      val res: Future[Future[Result]] = for {
        organizationOpt <- OrganizationRepository.getByUuid(uuid)
        members <- UserRepository.findOrganizationMembers(uuid)
      } yield {
        organizationOpt.map { organization =>
          members.filter(_.uuid != user.uuid).map { u =>
            val emailData = EmailSrv.generateOrganizationDeleteEmail(u, organization, user)
            MandrillSrv.sendEmail(emailData)
          }
          OrganizationRepository.delete(uuid).map { r =>
            Redirect(backend.controllers.routes.Profile.details()).flashing("success" -> "Organisation supprimée !")
          }
        }.getOrElse {
          Future(Redirect(backend.controllers.routes.Profile.details()).flashing("error" -> "L'organisation demandée n'existe pas."))
        }
      }
      res.flatMap(identity)
    } else {
      Future(Redirect(backend.controllers.routes.Organizations.details(uuid)).flashing("error" -> "Vous n'avez pas les droits pour supprimer cette organisation :("))
    }
  }

  def doOrganizationInvite(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
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

  // when the user leave an organization
  def leave(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    if (user.canAdministrateOrganization(uuid)) {
      Future(Redirect(backend.controllers.routes.Organizations.details(uuid)).flashing("error" -> "Impossible de quitter une organisation dont vous êtes le responsable."))
    } else if (user.organizationRole(uuid).isDefined) {
      val userWithoutOrg = user.copy(organizationIds = user.organizationIds.filter(_.organizationId != uuid))
      for {
        organizationOpt <- OrganizationRepository.getByUuid(uuid)
        organizationOwnerOpt <- UserRepository.getOrganizationOwner(uuid)
        updatedUserOpt <- UserRepository.update(userWithoutOrg.uuid, userWithoutOrg)
      } yield {
        for {
          organization <- organizationOpt
          organizationOwner <- organizationOwnerOpt
        } yield {
          val emailData = EmailSrv.generateOrganizationLeaveEmail(user, organization, organizationOwner)
          MandrillSrv.sendEmail(emailData)
        }
        Redirect(backend.controllers.routes.Profile.details()).flashing("success" -> s"Vous ne faites plus parti de l'organisation ${organizationOpt.map(_.name).getOrElse("")}")
      }
    } else {
      Future(Redirect(backend.controllers.routes.Profile.details()).flashing("error" -> "Vous ne faites pas parti de cette organisation."))
    }
  }

  // when the organization owner ban a member
  def ban(uuid: String, userId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    if (user.canAdministrateOrganization(uuid)) {
      val res: Future[Future[Result]] = for {
        organizationOpt <- OrganizationRepository.getByUuid(uuid)
        bannedUserOpt <- UserRepository.getByUuid(userId)
      } yield {
        if (bannedUserOpt.isDefined && bannedUserOpt.get.organizationRole(uuid).isDefined) {
          val bannedUser = bannedUserOpt.get
          val userWithoutOrg = bannedUser.copy(organizationIds = bannedUser.organizationIds.filter(_.organizationId != uuid))
          UserRepository.update(userWithoutOrg.uuid, userWithoutOrg).map { updatedUserOpt =>
            for {
              organization <- organizationOpt
            } yield {
              val emailData = EmailSrv.generateOrganizationBanEmail(bannedUser, organization, user)
              MandrillSrv.sendEmail(emailData)
            }
            Redirect(backend.controllers.routes.Organizations.details(uuid)).flashing("success" -> s"${userWithoutOrg.name()} ne fait plus parti de l'organisation ${organizationOpt.map(_.name).getOrElse("")}")
          }
        } else {
          Future(Redirect(backend.controllers.routes.Organizations.details(uuid)).flashing("error" -> "L'utilisateur ciblé n'existe pas où ne fait pas parti de l'organisation."))
        }
      }
      res.flatMap(identity)
    } else {
      Future(Redirect(backend.controllers.routes.Organizations.details(uuid)).flashing("error" -> "Vous n'avez pas les droits nécessaires."))
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
          ("success", s"Invitation à ${organization.name} envoyée !")
        }
      }.getOrElse(Future(("error", "L'organisation demandée n'existe pas :(")))
    }
    res.flatMap(identity)
  }

}
