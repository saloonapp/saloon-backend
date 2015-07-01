package admin.controllers

import common.repositories.Repository
import common.models.utils.Page
import common.models.user.Device
import common.models.user.DeviceData
import common.models.user.UserAction
import common.repositories.user.DeviceRepository
import common.repositories.user.UserActionRepository
import common.services.DeviceSrv
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import reactivemongo.core.commands.LastError

object Devices extends Controller {
  val form: Form[DeviceData] = Form(DeviceData.fields)
  val repository: Repository[Device] = DeviceRepository
  val mainRoute = routes.Devices
  val viewList = admin.views.html.Devices.list
  val viewDetails = admin.views.html.Devices.details
  val viewCreate = admin.views.html.Devices.create
  val viewUpdate = admin.views.html.Devices.update
  def createElt(data: DeviceData): Device = DeviceData.toModel(data)
  def toData(elt: Device): DeviceData = DeviceData.fromModel(elt)
  def updateElt(elt: Device, data: DeviceData): Device = DeviceData.merge(elt, data)
  def successCreateFlash(elt: Device) = s"Device '${elt.info.uuid}' has been created"
  def errorCreateFlash(elt: DeviceData) = s"Device '${elt.info.uuid}' can't be created"
  def successUpdateFlash(elt: Device) = s"Device '${elt.info.uuid}' has been modified"
  def errorUpdateFlash(elt: Device) = s"Device '${elt.info.uuid}' can't be modified"
  def successDeleteFlash(elt: Device) = s"Device '${elt.info.uuid}' has been deleted"

  def list(query: Option[String], page: Option[Int], pageSize: Option[Int], sort: Option[String]) = Action.async { implicit req =>
    val curPage = page.getOrElse(1)
    repository.findPage(query.getOrElse(""), curPage, pageSize.getOrElse(Page.defaultSize), sort.getOrElse("-meta.created")).map { eltPage =>
      if (curPage > 1 && eltPage.totalPages < curPage)
        Redirect(mainRoute.list(query, Some(eltPage.totalPages), pageSize, sort))
      else
        Ok(viewList(eltPage))
    }
  }

  def create = Action { implicit req =>
    Ok(viewCreate(form))
  }

  def doCreate = Action.async { implicit req =>
    form.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(viewCreate(formWithErrors))),
      formData => repository.insert(createElt(formData)).map {
        _.map { elt =>
          Redirect(mainRoute.list()).flashing("success" -> successCreateFlash(elt))
        }.getOrElse(InternalServerError(viewCreate(form.fill(formData))).flashing("error" -> errorCreateFlash(formData)))
      })
  }

  def details(uuid: String) = Action.async { implicit req =>
    for {
      deviceOpt <- repository.getByUuid(uuid)
      actions <- DeviceSrv.getActionsForUser(uuid)
    } yield {
      deviceOpt.map { elt =>
        Ok(viewDetails(elt, actions))
      }.getOrElse(NotFound(admin.views.html.error404()))
    }
  }

  def update(uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).map {
      _.map { elt =>
        Ok(viewUpdate(form.fill(toData(elt)), elt))
      }.getOrElse(NotFound(admin.views.html.error404()))
    }
  }

  def doUpdate(uuid: String) = Action.async { implicit req =>
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

  def delete(uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).map {
      _.map { elt =>
        repository.delete(uuid)
        Redirect(mainRoute.list()).flashing("success" -> successDeleteFlash(elt))
      }.getOrElse(NotFound(admin.views.html.error404()))
    }
  }

  def deleteAction(deviceId: String, itemType: String, itemId: String, actionType: String, actionId: String) = Action.async { implicit req =>
    repository.getByUuid(deviceId).flatMap {
      _.map { elt =>
        val res: Future[LastError] =
          if (actionType == "favorite") UserActionRepository.deleteFavorite(deviceId, itemType, itemId)
          else if (actionType == "mood") UserActionRepository.deleteMood(deviceId, itemType, itemId)
          else if (actionType == "comment") UserActionRepository.deleteComment(deviceId, itemType, itemId, actionId)
          else Future.successful(LastError(false, Some("Unknown actionType <" + actionType + ">"), None, None, None, 0, false))

        res.map(err => Redirect(mainRoute.details(deviceId)))
      }.getOrElse(Future(NotFound(admin.views.html.error404())))
    }
  }
}
