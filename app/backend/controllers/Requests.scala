package backend.controllers

import common.models.user.User
import common.models.user.UserInfo
import common.models.user.UserOrganization
import common.models.user.OrganizationRequest
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
import com.mohiva.play.silhouette.core.LoginInfo

object Requests extends SilhouetteEnvironment {

  def reminder(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    RequestRepository.getPendingByUser(uuid, user.uuid).flatMap { requestOpt =>
      requestOpt.map { request =>
        request.content match {
          case OrganizationRequest(organizationId, _, _) => {
            val res = for {
              organizationOpt <- OrganizationRepository.getByUuid(organizationId)
              organizationOwnerOpt <- UserRepository.getOrganizationOwner(organizationId)
            } yield {
              val emailData = EmailSrv.generateOrganizationRequestEmail(user, organizationOpt.get, organizationOwnerOpt.get, request)
              MandrillSrv.sendEmail(emailData).map { res =>
                Redirect(backend.controllers.routes.Profile.details()).flashing("success" -> s"Demande d'accès à ${organizationOpt.get.name} renvoyée !")
              }
            }
            res.flatMap(identity)
          }
          case _ => Future(Redirect(backend.controllers.routes.Application.index()).flashing("error" -> "Aucune action correspondante à cette demande :("))
        }
      }.getOrElse {
        Future(Redirect(backend.controllers.routes.Application.index()).flashing("error" -> "Aucune demande correspondante :("))
      }
    }
  }

  def cancel(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    RequestRepository.getPendingByUser(uuid, user.uuid).flatMap { requestOpt =>
      requestOpt.map { request =>
        RequestRepository.setCanceled(uuid).map { err =>
          request.content match {
            case OrganizationRequest(_, _, _) => Redirect(backend.controllers.routes.Profile.details()).flashing("success" -> s"Demande annulée !")
            case _ => Redirect(backend.controllers.routes.Application.index()).flashing("success" -> s"Demande annulée !")
          }
        }
      }.getOrElse {
        Future(Redirect(backend.controllers.routes.Application.index()).flashing("error" -> "Aucune demande correspondante :("))
      }
    }
  }

  def accept(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    RequestRepository.getPending(uuid).flatMap { requestOpt =>
      requestOpt.map { request =>
        request.content match {
          case OrganizationRequest(organizationId, _, _) => {
            val redirect = Redirect(backend.controllers.routes.Organizations.details(organizationId))
            if (user.canAdministrateOrganization(organizationId)) {
              val res: Future[Future[Result]] = for {
                organizationOpt <- OrganizationRepository.getByUuid(organizationId)
                requestUserOpt <- request.userId.map { userId => UserRepository.getByUuid(userId) }.getOrElse(Future(None))
              } yield {
                val resOpt: Option[Future[Result]] = for {
                  organization <- organizationOpt
                  requestUser <- requestUserOpt
                } yield {
                  val requestUserWithOrg = requestUser.copy(organizationIds = requestUser.organizationIds ++ List(UserOrganization(organization.uuid, UserOrganization.member)))
                  UserRepository.update(requestUserWithOrg.uuid, requestUserWithOrg).flatMap { userUpdatedOpt =>
                    RequestRepository.setAccepted(uuid).flatMap { _ =>
                      val emailData = EmailSrv.generateOrganizationRequestAcceptedEmail(requestUser, organization, user)
                      MandrillSrv.sendEmail(emailData).map { _ =>
                        redirect.flashing("success" -> "Demande acceptée !")
                      }
                    }
                  }
                }
                resOpt.getOrElse(Future(redirect.flashing("error" -> "Impossible d'accepter cette demande :(")))
              }
              res.flatMap(identity)
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

  def reject(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    RequestRepository.getPending(uuid).flatMap { requestOpt =>
      requestOpt.map { request =>
        request.content match {
          case OrganizationRequest(organizationId, _, _) => {
            val redirect = Redirect(backend.controllers.routes.Organizations.details(organizationId))
            if (user.canAdministrateOrganization(organizationId)) {
              val res: Future[Future[Result]] = for {
                organizationOpt <- OrganizationRepository.getByUuid(organizationId)
                requestUserOpt <- request.userId.map { userId => UserRepository.getByUuid(userId) }.getOrElse(Future(None))
              } yield {
                val resOpt: Option[Future[Result]] = for {
                  organization <- organizationOpt
                  requestUser <- requestUserOpt
                } yield {
                  RequestRepository.setRejected(uuid).flatMap { _ =>
                    val emailData = EmailSrv.generateOrganizationRequestRejectedEmail(requestUser, organization)
                    MandrillSrv.sendEmail(emailData).map { _ =>
                      redirect.flashing("success" -> "Demande refusée !")
                    }
                  }
                }
                resOpt.getOrElse(Future(redirect.flashing("error" -> "Impossible de refuser cette demande :(")))
              }
              res.flatMap(identity)
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

}
