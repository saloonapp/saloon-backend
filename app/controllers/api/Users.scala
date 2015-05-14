package controllers.api

import infrastructure.repository.common.Repository
import infrastructure.repository.UserRepository
import infrastructure.repository.UserActionRepository
import models.User
import models.UserAction
import models.UserActionConent
import models.Device
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.libs.json._

object Users extends Controller {
  val repository: Repository[User] = UserRepository

  def find(deviceId: String) = Action.async { implicit req =>
    UserRepository.getByDevice(deviceId).map { eltOpt =>
      eltOpt.map(elt => Ok(Json.toJson(elt))).getOrElse(NotFound)
    }
  }

  def create() = Action.async(parse.json) { implicit req =>
    req.body.validate[Device].map { device =>
      UserRepository.getByDevice(device.uuid).flatMap {
        _.map {
          user => Future(Ok(Json.toJson(user)))
        }.getOrElse {
          repository.insert(User.fromDevice(device)).map {
            _.map { user => Created(Json.toJson(user)) }.getOrElse(InternalServerError)
          }
        }
      }
    }.getOrElse(Future(BadRequest))
  }

  def details(uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).map {
      _.map { elt => Ok(Json.toJson(elt)) }.getOrElse(NotFound)
    }
  }

  def actions(uuid: String) = Action.async { implicit req =>
    UserActionRepository.findByUser(uuid).map { actions =>
      //val res: Map[String, List[UserAction]] = actions.groupBy(_.action).map { case (key, value) => (key.getType(), value) }
      val res: Map[String, List[UserAction]] = actions.groupBy(_.eventId.getOrElse("unknown"))
      Ok(Json.toJson(res))
    }
  }

  def eventActions(uuid: String, eventId: String) = Action.async { implicit req =>
    UserActionRepository.findByUserEvent(uuid, eventId).map { actions =>
      //val res: Map[String, List[UserAction]] = actions.groupBy(_.action).map { case (key, value) => (key.getType(), value) }
      Ok(Json.toJson(actions))
    }
  }
}
