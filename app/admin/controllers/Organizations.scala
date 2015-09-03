package admin.controllers

import common.models.utils.Page
import common.models.user.Organization
import common.models.user.OrganizationData
import common.repositories.Repository
import common.repositories.user.OrganizationRepository
import common.repositories.user.UserRepository
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import reactivemongo.core.commands.LastError

object Organizations extends SilhouetteEnvironment {
  val form: Form[OrganizationData] = Form(OrganizationData.fields)
  val repository: Repository[Organization, String] = OrganizationRepository
  val mainRoute = routes.Organizations
  val viewList = admin.views.html.Organizations.list
  val viewDetails = admin.views.html.Organizations.details
  val viewCreate = admin.views.html.Organizations.create
  val viewUpdate = admin.views.html.Organizations.update
  def createElt(data: OrganizationData): Organization = OrganizationData.toModel(data)
  def toData(elt: Organization): OrganizationData = OrganizationData.fromModel(elt)
  def updateElt(elt: Organization, data: OrganizationData): Organization = OrganizationData.merge(elt, data)
  def successCreateFlash(elt: Organization) = s"Organization '${elt.name}' has been created"
  def errorCreateFlash(elt: OrganizationData) = s"Organization '${elt.name}' can't be created"
  def successUpdateFlash(elt: Organization) = s"Organization '${elt.name}' has been modified"
  def errorUpdateFlash(elt: Organization) = s"Organization '${elt.name}' can't be modified"
  def successDeleteFlash(elt: Organization) = s"Organization '${elt.name}' has been deleted"

  def list(query: Option[String], page: Option[Int], pageSize: Option[Int], sort: Option[String]) = SecuredAction.async { implicit req =>
    val curPage = page.getOrElse(1)
    repository.findPage(query.getOrElse(""), curPage, pageSize.getOrElse(Page.defaultSize), sort.getOrElse("name")).map { eltPage =>
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
      organizationOpt <- repository.getByUuid(uuid)
      users <- UserRepository.findOrganizationMembers(uuid)
    } yield {
      organizationOpt.map { elt =>
        Ok(viewDetails(elt, users))
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
}
