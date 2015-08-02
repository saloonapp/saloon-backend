package backend.controllers

import common.models.user.User
import common.models.user.UserInfo
import common.models.user.UserOrganization
import common.models.user.OrganizationData
import common.repositories.user.UserRepository
import common.repositories.user.OrganizationRepository
import common.repositories.event.EventRepository
import common.services.EventSrv
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import com.mohiva.play.silhouette.core.LoginInfo

object Profile extends SilhouetteEnvironment {
  val organizationForm: Form[OrganizationData] = Form(OrganizationData.fields)
  val accessRequestForm = Form(single("organizationId" -> nonEmptyText))

  def details = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    OrganizationRepository.findAll().map { organizations =>
      val organizationsWithRole = organizations.map(o => (o, user.organizationIds.find(uo => uo.organizationId == o.uuid)))
      val notMemberOrganizations = organizationsWithRole.filter(_._2.isEmpty).map(_._1)
      val memberOrganizationsWithRole = organizationsWithRole.filter(_._2.isDefined).map { case (o, rOpt) => (o, rOpt.get.role) }.sortBy {
        case (orga, role) => UserOrganization.getPriority(role)
      }
      Ok(backend.views.html.Profile.details(memberOrganizationsWithRole, notMemberOrganizations, organizationForm, accessRequestForm))
    }
  }

  def doCreateOrganization = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    organizationForm.bindFromRequest.fold(
      formWithErrors => Future(Redirect(backend.controllers.routes.Profile.details()).flashing("error" -> "Votre organisation n'est pas correcte :(")),
      formData => {
        OrganizationRepository.getByName(formData.name).flatMap { orgOpt =>
          orgOpt.map { org =>
            Future(Redirect(backend.controllers.routes.Profile.details()).flashing("error" -> s"L'organisation ${formData.name} existe déjà !"))
          }.getOrElse {
            OrganizationRepository.insert(OrganizationData.toModel(formData)).flatMap { createdOpt =>
              createdOpt.map { created =>
                val userWithOrg = user.copy(organizationIds = user.organizationIds ++ List(UserOrganization(created.uuid, UserOrganization.owner)))
                UserRepository.update(userWithOrg.uuid, userWithOrg).map { userUpdatedOpt =>
                  Redirect(backend.controllers.routes.Profile.details()).flashing("success" -> s"Votre organisation ${formData.name} vient d'être créée. Invitez d'autres personnes à vous rejoindre :)")
                }
              }.getOrElse {
                Future(Redirect(backend.controllers.routes.Profile.details()).flashing("error" -> s"Erreur lors de la création :("))
              }
            }
          }
        }
      })
  }

  def doOrganizationRequestAccess = TODO
}
