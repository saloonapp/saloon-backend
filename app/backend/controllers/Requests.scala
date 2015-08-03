package backend.controllers

import common.models.user.User
import common.models.user.UserInfo
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
}
