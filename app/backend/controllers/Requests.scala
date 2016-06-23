package backend.controllers

import common.models.values.typed.Email
import common.models.values.typed.UserRole
import common.models.user.User
import common.models.user.UserOrganization
import common.models.user.OrganizationRequest
import common.models.user.OrganizationInvite
import common.models.user.OrganizationId
import common.models.user.Request
import common.models.user.RequestId
import common.repositories.user.UserRepository
import common.repositories.user.OrganizationRepository
import common.repositories.user.RequestRepository
import common.services.EmailSrv
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._

object Requests extends SilhouetteEnvironment {

  def details(requestId: RequestId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    RequestRepository.getPending(requestId).flatMap { requestOpt =>
      requestOpt.map { request =>
        request.content match {
          case OrganizationInvite(organizationId, email, comment, _) => {
            if (user.email == email) {
              for {
                organizationOpt <- OrganizationRepository.getByUuid(organizationId)
                organizationOwnerOpt <- request.userId.map { userId => UserRepository.getByUuid(userId) }.getOrElse { Future(None) }
              } yield {
                val res = for {
                  organization <- organizationOpt
                  organizationOwner <- organizationOwnerOpt
                } yield {
                  Ok(backend.views.html.Profile.Requests.organizationInvite(request, organization, organizationOwner, comment))
                }
                res.getOrElse(Redirect(backend.controllers.routes.Application.index()).flashing("error" -> "Organisation inconnue :("))
              }
            } else {
              Future(Redirect(backend.controllers.routes.Application.index()).flashing("error" -> "Aucune demande correspondante :("))
            }
          }
          case _ => Future(Redirect(backend.controllers.routes.Application.index()).flashing("error" -> "Aucune demande correspondante :("))
        }
      }.getOrElse {
        Future(Redirect(backend.controllers.routes.Application.index()).flashing("error" -> "Aucune demande correspondante :("))
      }
    }
  }

  def doReminder(requestId: RequestId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    RequestRepository.getPendingByUser(requestId, user.uuid).flatMap { requestOpt =>
      requestOpt.map { request =>
        request.content match {
          case OrganizationRequest(organizationId, _, _) => {
            val res = for {
              organizationOpt <- OrganizationRepository.getByUuid(organizationId)
              organizationOwnerOpt <- UserRepository.getOrganizationOwner(organizationId)
            } yield {
              val resOpt: Option[Future[Result]] = for {
                organization <- organizationOpt
                organizationOwner <- organizationOwnerOpt
              } yield {
                val emailData = EmailSrv.generateOrganizationRequestEmail(user, organization, organizationOwner, request)
                EmailSrv.sendEmail(emailData).map { success =>
                  Redirect(backend.controllers.routes.Profile.details()).flashing(if(success) ("success", s"Demande d'accès à ${organization.name} renvoyée !") else ("error", "Problème d'envoi du mail"))
                }
              }
              resOpt.getOrElse(Future(Redirect(backend.controllers.routes.Profile.details()).flashing("error" -> "L'organisation n'existe plus :(")))
            }
            res.flatMap(identity)
          }
          case OrganizationInvite(organizationId, email, _, _) => {
            val res: Future[Future[(String, String)]] = for {
              organizationOpt <- OrganizationRepository.getByUuid(organizationId)
              userOpt <- UserRepository.getByEmail(email)
              requestInviteOpt <- RequestRepository.getPendingInviteForRequest(request.uuid)
            } yield {
              organizationOpt.map { organization =>
                val emailDataOpt = userOpt.map { invitedUser =>
                  Some(EmailSrv.generateOrganizationInviteEmail(user, organization, invitedUser, request))
                }.getOrElse {
                  requestInviteOpt.map { requestInvite => EmailSrv.generateOrganizationAndSalooNInviteEmail(user, organization, email, None, requestInvite) }
                }
                emailDataOpt.map { emailData =>
                  EmailSrv.sendEmail(emailData).map { success =>
                    if(success) ("success", s"Demande d'accès à ${organization.name} envoyée !") else ("error", "Problème d'envoi du mail")
                  }
                }.getOrElse(Future(("error", "L'invitation correspondant à la demande n'a pas été trouvée :(")))
              }.getOrElse(Future(("error", "L'organisation demandée n'existe pas :(")))
            }
            res.flatMap(identity).map {
              case (category, message) => Redirect(backend.controllers.routes.Organizations.details(organizationId)).flashing(category -> message)
            }
          }
          case _ => Future(Redirect(backend.controllers.routes.Application.index()).flashing("error" -> "Aucune action correspondante à cette demande :("))
        }
      }.getOrElse {
        Future(Redirect(backend.controllers.routes.Application.index()).flashing("error" -> "Aucune demande correspondante :("))
      }
    }
  }

  def doCancel(requestId: RequestId) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    RequestRepository.getPendingInviteForRequest(requestId).map { // cancel linked invite request if it exists
      _.map { inviteRequest =>
        RequestRepository.setCanceled(inviteRequest.uuid)
      }
    }
    RequestRepository.getPendingByUser(requestId, user.uuid).flatMap { requestOpt =>
      requestOpt.map { request =>
        RequestRepository.setCanceled(requestId).flatMap { err =>
          request.content match {
            case OrganizationRequest(_, _, _) => Future(Redirect(backend.controllers.routes.Profile.details()).flashing("success" -> "Demande annulée !"))
            case OrganizationInvite(organizationId, email, _, _) => {
              OrganizationRepository.getByUuid(organizationId).flatMap { organizationOpt =>
                organizationOpt.map { organization =>
                  val emailData = EmailSrv.generateOrganizationInviteCanceledEmail(email, organization)
                  EmailSrv.sendEmail(emailData).map { success =>
                    Redirect(backend.controllers.routes.Organizations.details(organizationId)).flashing(if(success) ("success", "Invitation annulée !") else ("error", "Problème d'envoi du mail"))
                  }
                }.getOrElse(Future(Redirect(backend.controllers.routes.Profile.details()).flashing("error" -> "L'organisation n'existe plus :(")))
              }
            }
            case _ => Future(Redirect(backend.controllers.routes.Application.index()).flashing("success" -> "Demande annulée !"))
          }
        }
      }.getOrElse {
        Future(Redirect(backend.controllers.routes.Application.index()).flashing("error" -> "Aucune demande correspondante :("))
      }
    }
  }

  def doAccept(requestId: RequestId, redirection: Option[String]) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    RequestRepository.getPending(requestId).flatMap { requestOpt =>
      requestOpt.map { request =>
        request.content match {
          case OrganizationRequest(organizationId, _, _) => {
            val redirect = Redirect(backend.controllers.routes.Organizations.details(organizationId))
            if (user.canAdministrateOrganization(organizationId)) {
              acceptOrganizationRequest(request, organizationId, user).map { case (category, message) => redirect.flashing(category -> message) }
            } else {
              Future(redirect.flashing("error" -> "Impossible d'accepter cette demande :("))
            }
          }
          case OrganizationInvite(organizationId, email, _, _) => {
            val redirect = redirection match {
              case Some("welcome") => Redirect(backend.controllers.routes.Application.welcome)
              case _ => Redirect(backend.controllers.routes.Organizations.details(organizationId))
            }
            if (user.email == email) {
              acceptOrganizationInvite(request, organizationId, user).map { case (category, message) => redirect.flashing(category -> message) }
            } else {
              Future(redirect.flashing("error" -> "Impossible d'accepter cette demande :("))
            }
          }
          case _ => Future(Redirect(backend.controllers.routes.Application.index()).flashing("error" -> "Impossible d'accepter cette demande :("))
        }
      }.getOrElse {
        Future(Redirect(backend.controllers.routes.Application.index()).flashing("error" -> "Aucune demande correspondante :("))
      }
    }
  }

  def doReject(requestId: RequestId, redirection: Option[String]) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    RequestRepository.getPending(requestId).flatMap { requestOpt =>
      requestOpt.map { request =>
        request.content match {
          case OrganizationRequest(organizationId, _, _) => {
            val redirect = Redirect(backend.controllers.routes.Organizations.details(organizationId))
            if (user.canAdministrateOrganization(organizationId)) {
              rejectOrganizationRequest(request, organizationId).map { case (category, message) => redirect.flashing(category -> message) }
            } else {
              Future(redirect.flashing("error" -> "Impossible de refuser cette demande :("))
            }
          }
          case OrganizationInvite(organizationId, email, _, _) => {
            val redirect = redirection match {
              case Some("welcome") => Redirect(backend.controllers.routes.Application.welcome)
              case _ => Redirect(backend.controllers.routes.Profile.details())
            }
            if (user.email == email) {
              rejectOrganizationInvite(request, organizationId, email).map { case (category, message) => redirect.flashing(category -> message) }
            } else {
              Future(redirect.flashing("error" -> "Impossible de refuser cette demande :("))
            }
          }
          case _ => Future(Redirect(backend.controllers.routes.Application.index()).flashing("error" -> "Impossible de refuser cette demande :("))
        }
      }.getOrElse {
        Future(Redirect(backend.controllers.routes.Application.index()).flashing("error" -> "Aucune demande correspondante :("))
      }
    }
  }

  /*
   * Private methods
   */

  private def acceptOrganizationRequest(request: Request, organizationId: OrganizationId, organizationOwner: User): Future[(String, String)] = {
    val res: Future[Future[Future[(String, String)]]] = for {
      organizationOpt <- OrganizationRepository.getByUuid(organizationId)
      userOpt <- request.userId.map { userId => UserRepository.getByUuid(userId) }.getOrElse(Future(None))
    } yield {
      val res2: Option[Future[Future[(String, String)]]] = for {
        organization <- organizationOpt
        user <- userOpt
      } yield {
        val userWithOrg = if (user.organizationRole(organization.uuid).isDefined) { user } else { user.copy(organizationIds = user.organizationIds ++ List(UserOrganization(organization.uuid, UserRole.member))) }
        for {
          userUpdatedOpt <- UserRepository.update(userWithOrg.uuid, userWithOrg)
          acceptErr <- RequestRepository.setAccepted(request.uuid)
        } yield {
          val emailData = EmailSrv.generateOrganizationRequestAcceptedEmail(user, organization, organizationOwner)
          EmailSrv.sendEmail(emailData).map { success =>
            if(success) ("success", "Demande acceptée !") else ("error", "Problème d'envoi du mail")
          }
        }
      }
      res2.getOrElse(Future(Future(("error", "Impossible d'accepter cette demande :("))))
    }
    res.flatMap(identity).flatMap(identity)
  }

  private def rejectOrganizationRequest(request: Request, organizationId: OrganizationId): Future[(String, String)] = {
    val res: Future[Future[(String, String)]] = for {
      organizationOpt <- OrganizationRepository.getByUuid(organizationId)
      requestUserOpt <- request.userId.map { userId => UserRepository.getByUuid(userId) }.getOrElse(Future(None))
    } yield {
      val resOpt: Option[Future[(String, String)]] = for {
        organization <- organizationOpt
        requestUser <- requestUserOpt
      } yield {
        RequestRepository.setRejected(request.uuid).flatMap { _ =>
          val emailData = EmailSrv.generateOrganizationRequestRejectedEmail(requestUser, organization)
          EmailSrv.sendEmail(emailData).map { success =>
            if(success) ("success", "Demande refusée !") else ("error", "Problème d'envoi du mail")
          }
        }
      }
      resOpt.getOrElse(Future(("error", "Impossible de refuser cette demande :(")))
    }
    res.flatMap(identity)
  }

  private def acceptOrganizationInvite(request: Request, organizationId: OrganizationId, invitedUser: User): Future[(String, String)] = {
    val res: Future[Future[(String, String)]] = for {
      organizationOpt <- OrganizationRepository.getByUuid(organizationId)
      organizationOwnerOpt <- request.userId.map { userId => UserRepository.getByUuid(userId) }.getOrElse(Future(None))
    } yield {
      organizationOpt.map { organization =>
        val invitedUserWithOrg = invitedUser.copy(organizationIds = invitedUser.organizationIds ++ List(UserOrganization(organizationId, UserRole.member)))
        UserRepository.update(invitedUserWithOrg.uuid, invitedUserWithOrg).flatMap { userUpdatedOpt =>
          RequestRepository.setAccepted(request.uuid).flatMap { _ =>
            organizationOwnerOpt.map { organizationOwner =>
              val emailData = EmailSrv.generateOrganizationInviteAcceptedEmail(invitedUser, organization, organizationOwner)
              EmailSrv.sendEmail(emailData).map { success =>
                if(success) ("success", s"Invitation à l'organisation ${organization.name} acceptée !") else ("error", "Problème d'envoi du mail")
              }
            }.getOrElse {
              Future(("success", s"Invitation à l'organisation ${organization.name} acceptée !"))
            }
          }
        }
      }.getOrElse(Future(("error", "L'organisation n'existe plus :(")))
    }
    res.flatMap(identity)
  }

  private def rejectOrganizationInvite(request: Request, organizationId: OrganizationId, inviteEmail: Email): Future[(String, String)] = {
    val res: Future[Future[(String, String)]] = for {
      organizationOpt <- OrganizationRepository.getByUuid(organizationId)
      organizationOwnerOpt <- request.userId.map { userId => UserRepository.getByUuid(userId) }.getOrElse(Future(None))
    } yield {
      organizationOpt.map { organization =>
        RequestRepository.setRejected(request.uuid).flatMap { _ =>
          organizationOwnerOpt.map { organizationOwner =>
            val emailData = EmailSrv.generateOrganizationInviteRejectedEmail(inviteEmail, organization, organizationOwner)
            EmailSrv.sendEmail(emailData).map { success =>
              if(success) ("success", s"Invitation à l'organisation ${organization.name} déclinée !") else ("error", "Problème d'envoi du mail")
            }
          }.getOrElse {
            Future(("success", s"Invitation à l'organisation ${organization.name} déclinée !"))
          }
        }
      }.getOrElse(Future(("error", "L'organisation n'existe plus :(")))
    }
    res.flatMap(identity)
  }

}
