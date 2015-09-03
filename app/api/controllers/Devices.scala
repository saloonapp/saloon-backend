package api.controllers

import common.models.event.EventId
import common.models.user.Device
import common.models.user.DeviceInfo
import common.repositories.Repository
import common.repositories.user.DeviceRepository
import common.repositories.user.UserActionRepository
import api.controllers.compatibility.Writer
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.libs.json._

object Devices extends Controller {
  val repository: Repository[Device, String] = DeviceRepository

  def find(deviceId: String) = Action.async { implicit req =>
    DeviceRepository.getByDeviceId(deviceId).map { eltOpt =>
      eltOpt.map(elt => Ok(Writer.write(elt))).getOrElse(NotFound)
    }
  }

  def create() = Action.async(parse.json) { implicit req =>
    req.body.validate[DeviceInfo].map { info =>
      DeviceRepository.getByDeviceId(info.uuid).flatMap {
        _.map {
          device => Future(Ok(Writer.write(device)))
        }.getOrElse {
          repository.insert(Device.fromInfo(info)).map {
            _.map { device => Created(Writer.write(device)) }.getOrElse(InternalServerError)
          }
        }
      }
    }.getOrElse(Future(BadRequest))
  }

  def details(uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).map {
      _.map { elt => Ok(Writer.write(elt)) }.getOrElse(NotFound)
    }
  }

  def actions(uuid: String) = Action.async { implicit req =>
    UserActionRepository.findByUser(uuid).map { actions =>
      val res = actions.groupBy(_.eventId.map(_.unwrap).getOrElse("unknown")).map { case (key, value) => (key, Json.obj("actions" -> value)) }
      Ok(Json.toJson(res))
    }
  }

  def eventActions(uuid: String, eventId: EventId) = Action.async { implicit req =>
    UserActionRepository.findByUserEvent(uuid, eventId).map { actions =>
      Ok(Json.obj("actions" -> actions))
    }
  }
}
