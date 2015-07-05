package admin.controllers

import common.models.utils.Page
import common.models.user.User
import common.models.user.UserData
import common.models.user.UserAction
import common.services.EmailSrv
import common.services.MandrillSrv
import common.repositories.Repository
import common.repositories.user.UserRepository
import common.repositories.user.UserActionRepository
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import reactivemongo.core.commands.LastError
import common.models.user.User
import authentication.environments.SilhouetteEnvironment
import com.mohiva.play.silhouette.core.Silhouette
import com.mohiva.play.silhouette.contrib.services.CachedCookieAuthenticator

object Users extends Silhouette[User, CachedCookieAuthenticator] with SilhouetteEnvironment {
  val form: Form[UserData] = Form(UserData.fields)
  val repository: Repository[User] = UserRepository
  val mainRoute = routes.Users
  val viewList = admin.views.html.Users.list
  val viewDetails = admin.views.html.Users.details
  val viewCreate = admin.views.html.Users.create
  val viewUpdate = admin.views.html.Users.update
  def createElt(data: UserData): User = UserData.toModel(data)
  def toData(elt: User): UserData = UserData.fromModel(elt)
  def updateElt(elt: User, data: UserData): User = UserData.merge(elt, data)
  def successCreateFlash(elt: User) = s"User '${elt.email}' has been created"
  def errorCreateFlash(elt: UserData) = s"User '${elt.email}' can't be created"
  def successUpdateFlash(elt: User) = s"User '${elt.email}' has been modified"
  def errorUpdateFlash(elt: User) = s"User '${elt.email}' can't be modified"
  def successDeleteFlash(elt: User) = s"User '${elt.email}' has been deleted"

  def list(query: Option[String], page: Option[Int], pageSize: Option[Int], sort: Option[String]) = SecuredAction.async { implicit req =>
    val curPage = page.getOrElse(1)
    repository.findPage(query.getOrElse(""), curPage, pageSize.getOrElse(Page.defaultSize), sort.getOrElse("-meta.created")).map { eltPage =>
      if (curPage > 1 && eltPage.totalPages < curPage)
        Redirect(mainRoute.list(query, Some(eltPage.totalPages), pageSize, sort))
      else
        Ok(viewList(eltPage))
    }
  }

  def create = SecuredAction { implicit req =>
    Ok(viewCreate(form))
  }

  def doCreate = SecuredAction.async { implicit req =>
    form.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(viewCreate(formWithErrors))),
      formData => repository.insert(createElt(formData)).map {
        _.map { elt =>
          Redirect(mainRoute.list()).flashing("success" -> successCreateFlash(elt))
        }.getOrElse(InternalServerError(viewCreate(form.fill(formData))).flashing("error" -> errorCreateFlash(formData)))
      })
  }

  def details(uuid: String) = SecuredAction.async { implicit req =>
    for {
      userOpt <- repository.getByUuid(uuid)
    } yield {
      userOpt.map { elt =>
        Ok(viewDetails(elt))
      }.getOrElse(NotFound(admin.views.html.error404()))
    }
  }

  def update(uuid: String) = SecuredAction.async { implicit req =>
    repository.getByUuid(uuid).map {
      _.map { elt =>
        Ok(viewUpdate(form.fill(toData(elt)), elt))
      }.getOrElse(NotFound(admin.views.html.error404()))
    }
  }

  def doUpdate(uuid: String) = SecuredAction.async { implicit req =>
    repository.getByUuid(uuid).flatMap {
      _.map { elt =>
        form.bindFromRequest.fold(
          formWithErrors => Future(BadRequest(viewUpdate(formWithErrors, elt))),
          formData => repository.update(uuid, updateElt(elt, formData)).map {
            _.map { updatedElt =>
              Redirect(mainRoute.details(uuid)).flashing("success" -> successUpdateFlash(updatedElt))
            }.getOrElse(InternalServerError(viewUpdate(form.fill(formData), elt)).flashing("error" -> errorUpdateFlash(elt)))
          })
      }.getOrElse(Future(NotFound(admin.views.html.error404())))
    }
  }

  def delete(uuid: String) = SecuredAction.async { implicit req =>
    repository.getByUuid(uuid).map {
      _.map { elt =>
        repository.delete(uuid)
        Redirect(mainRoute.list()).flashing("success" -> successDeleteFlash(elt))
      }.getOrElse(NotFound(admin.views.html.error404()))
    }
  }

  def deleteAction(userId: String, itemType: String, itemId: String, actionType: String, actionId: String) = SecuredAction.async { implicit req =>
    repository.getByUuid(userId).flatMap {
      _.map { elt =>
        val res: Future[LastError] =
          if (actionType == "favorite") UserActionRepository.deleteFavorite(userId, itemType, itemId)
          else if (actionType == "mood") UserActionRepository.deleteMood(userId, itemType, itemId)
          else if (actionType == "comment") UserActionRepository.deleteComment(userId, itemType, itemId, actionId)
          else Future.successful(LastError(false, Some("Unknown actionType <" + actionType + ">"), None, None, None, 0, false))

        res.map(err => Redirect(mainRoute.details(userId)))
      }.getOrElse(Future(NotFound(admin.views.html.error404())))
    }
  }

  def sendInviteEmail(userId: String) = SecuredAction.async { implicit request =>
    UserRepository.getByUuid(userId).flatMap { userOpt =>
      userOpt.map { user =>
        if (user.loginInfo.providerID == "") {
          val inviteUrl = authentication.controllers.routes.Auth.createAccount(userId).absoluteURL(true)
          val emailData = EmailSrv.generateUserInviteEmail(inviteUrl, user.email)
          MandrillSrv.sendEmail(emailData).map { res =>
            Redirect(admin.controllers.routes.Users.details(userId)).flashing("success" -> "Email d'invitation envoyé :)")
          }
        } else {
          Future(Redirect(admin.controllers.routes.Users.details(userId)).flashing("error" -> "L'invitation a déjà été acceptée"))
        }
      }.getOrElse {
        Future(Redirect(admin.controllers.routes.Users.details(userId)).flashing("error" -> s"L'utilisateur $userId n'existe pas"))
      }
    }
  }
}
