package api.controllers

import common.models.event.Event
import common.models.event.EventId
import common.models.event.EventItem
import common.models.user.Device
import common.models.user.DeviceId
import common.models.user.UserAction
import common.models.user.UserActionId
import common.models.values.typed._
import common.repositories.Repository
import common.repositories.event.EventRepository
import common.repositories.event.EventItemRepository
import common.repositories.user.DeviceRepository
import common.repositories.user.UserActionRepository
import backend.utils.ControllerHelpers
import org.joda.time.DateTime
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.libs.json._
import reactivemongo.api.commands.WriteResult

object UserActions extends Controller with ControllerHelpers {
  def favorite(eventId: EventId, itemType: ItemType, itemId: GenericId) = Action.async { implicit req =>
    insertActionUnique(eventId, itemType, itemId)(UserActionRepository.getFavorite, UserActionRepository.insertFavorite)
  }

  def unfavorite(eventId: EventId, itemType: ItemType, itemId: GenericId) = Action.async { implicit req =>
    deleteActionUnique(eventId, itemType, itemId)(UserActionRepository.deleteFavorite)
  }

  def done(eventId: EventId, itemType: ItemType, itemId: GenericId) = Action.async { implicit req =>
    insertActionUnique(eventId, itemType, itemId)(UserActionRepository.getDone, UserActionRepository.insertDone)
  }

  def undone(eventId: EventId, itemType: ItemType, itemId: GenericId) = Action.async { implicit req =>
    deleteActionUnique(eventId, itemType, itemId)(UserActionRepository.deleteDone)
  }

  def mood(eventId: EventId, itemType: ItemType, itemId: GenericId) = Action.async(parse.json) { implicit req =>
    bodyWith("rating") { rating =>
      setActionUnique(eventId, itemType, itemId)(UserActionRepository.getMood, UserActionRepository.setMood(rating))
    }
  }

  def deleteMood(eventId: EventId, itemType: ItemType, itemId: GenericId) = Action.async { implicit req =>
    deviceFromHeader() { device =>
      withData(eventId, itemType, itemId) { (event, item) =>
        UserActionRepository.deleteMood(device.uuid, itemType, item.uuid).map { res =>
          if (res.ok) { if (res.n == 0) NotFound(Json.obj("message" -> s"Unable to find mood for item $itemType.$itemId")) else NoContent } else { InternalServerError }
        }
      }
    }
  }

  def createComment(eventId: EventId, itemType: ItemType, itemId: GenericId) = Action.async(parse.json) { implicit req =>
    bodyWith("text") { text =>
      deviceFromHeader() { device =>
        withData(eventId, itemType, itemId) { (event, item) =>
          UserActionRepository.insertComment(device.uuid, itemType, item.uuid, TextMultiline(text), event.uuid, withTime()).map { eltOpt =>
            eltOpt.map(elt => Created(Json.toJson(elt))).getOrElse(InternalServerError)
          }
        }
      }
    }
  }

  def updateComment(eventId: EventId, itemType: ItemType, itemId: GenericId, uuid: UserActionId) = Action.async(parse.json) { implicit req =>
    bodyWith("text") { text =>
      deviceFromHeader() { device =>
        withData(eventId, itemType, itemId) { (event, item) =>
          UserActionRepository.getComment(device.uuid, itemType, item.uuid, uuid).flatMap {
            _.map { userAction =>
              UserActionRepository.updateComment(device.uuid, itemType, item.uuid, uuid, userAction, TextMultiline(text), withTime()).map { eltOpt =>
                eltOpt.map(elt => Ok(Json.toJson(elt))).getOrElse(InternalServerError)
              }
            }.getOrElse(Future(NotFound(Json.obj("message" -> s"Unable to find comment with id $uuid"))))
          }
        }
      }
    }
  }

  def deleteComment(eventId: EventId, itemType: ItemType, itemId: GenericId, uuid: UserActionId) = Action.async { implicit req =>
    deviceFromHeader() { device =>
      withData(eventId, itemType, itemId) { (event, item) =>
        UserActionRepository.deleteComment(device.uuid, itemType, item.uuid, uuid).map { res =>
          if (res.ok) { if (res.n == 0) NotFound(Json.obj("message" -> s"Unable to find comment with id $uuid")) else NoContent } else { InternalServerError }
        }
      }
    }
  }

  def subscribe(eventId: EventId): Action[JsValue] = subscribe(eventId, ItemType.events, eventId)
  def subscribe(eventId: EventId, itemType: ItemType, itemId: GenericId) = Action.async(parse.json) { implicit req =>
    bodyWith("email", "filter") { (email, filter) =>
      setActionUnique(eventId, itemType, itemId)(UserActionRepository.getSubscribe, UserActionRepository.setSubscribe(Email(email), filter))
    }
  }

  def unsubscribe(eventId: EventId): Action[AnyContent] = unsubscribe(eventId, ItemType.events, eventId)
  def unsubscribe(eventId: EventId, itemType: ItemType, itemId: GenericId) = Action.async { implicit req =>
    deleteActionUnique(eventId, itemType, itemId)(UserActionRepository.deleteSubscribe)
  }

  def syncEventActions(deviceId: DeviceId, eventId: EventId) = Action.async(parse.json) { implicit req =>
    (req.body \ "actions").asOpt[List[UserAction]].map { actions =>
      UserActionRepository.deleteByEventUser(eventId, deviceId).flatMap { err =>
        UserActionRepository.bulkInsert(actions).map { count =>
          Ok(Json.obj("message" -> "UserActions sync with client !"))
        }
      }
    }.getOrElse(Future(BadRequest(Json.obj("message" -> "Sync endpoint expects the complete List[UserAction] in field 'actions' of request body !"))))
  }

  /*
   * There must be a max of one action for (itemType, itemId)
   */
  private def insertActionUnique(eventId: EventId, itemType: ItemType, itemId: GenericId)(get: (DeviceId, ItemType, GenericId) => Future[Option[UserAction]], insert: (DeviceId, ItemType, GenericId, EventId, Option[DateTime]) => Future[Option[UserAction]])(implicit req: Request[Any]): Future[Result] = {
    deviceFromHeader() { device =>
      withData(eventId, itemType, itemId) { (event, item) =>
        get(device.uuid, itemType, item.uuid).flatMap {
          _.map { elt => Future(Ok(Json.toJson(elt))) }.getOrElse {
            insert(device.uuid, itemType, item.uuid, event.uuid, withTime()).map { eltOpt =>
              eltOpt.map(elt => Created(Json.toJson(elt))).getOrElse(InternalServerError)
            }
          }
        }
      }
    }
  }

  /*
   * There must be a max of one action for (itemType, itemId) but it can be overriden
   */
  private def setActionUnique(eventId: EventId, itemType: ItemType, itemId: GenericId)(get: (DeviceId, ItemType, GenericId) => Future[Option[UserAction]], set: (DeviceId, ItemType, GenericId, EventId, Option[UserAction], Option[DateTime]) => Future[Option[UserAction]])(implicit req: Request[Any]): Future[Result] = {
    deviceFromHeader() { device =>
      withData(eventId, itemType, itemId) { (event, item) =>
        get(device.uuid, itemType, item.uuid).flatMap { eltOpt =>
          set(device.uuid, itemType, item.uuid, event.uuid, eltOpt, withTime()).map { newEltOpt =>
            newEltOpt.map(elt => Ok(Json.toJson(elt))).getOrElse(InternalServerError)
          }
        }
      }
    }
  }

  /*
   * Delete the unique action
   */
  private def deleteActionUnique(eventId: EventId, itemType: ItemType, itemId: GenericId)(delete: (DeviceId, ItemType, GenericId) => Future[WriteResult])(implicit req: Request[Any]): Future[Result] = {
    deviceFromHeader() { device =>
      withData(eventId, itemType, itemId) { (event, item) =>
        delete(device.uuid, itemType, item.uuid).map { res =>
          if (res.ok) { NoContent } else { InternalServerError }
        }
      }
    }
  }

  private def bodyWith(field: String)(exec: (String) => Future[Result])(implicit req: Request[JsValue]) = {
    (req.body \ field).asOpt[String].map { value =>
      exec(value)
    }.getOrElse(Future(BadRequest(Json.obj("message" -> s"Your request body should have a JSON object with a field '$field' !"))))
  }

  private def bodyWith(field1: String, field2: String)(exec: (String, String) => Future[Result])(implicit req: Request[JsValue]) = {
    (for {
      value1 <- (req.body \ field1).asOpt[String]
      value2 <- (req.body \ field2).asOpt[String]
    } yield {
      exec(value1, value2)
    }).getOrElse(Future(BadRequest(Json.obj("message" -> s"Your request body should have a JSON object with fields '$field1' & '$field2' !"))))
  }

  private def deviceFromHeader()(exec: Device => Future[Result])(implicit req: Request[Any]): Future[Result] = {
    implicit val format: String = "json"
    val res: Option[Future[Result]] = for {
      deviceIdStr <- req.headers.get("userId")
      deviceId <- DeviceId.build(deviceIdStr).right.toOption
      result <- Some(withDevice(deviceId)(exec))
    } yield result
    res.getOrElse(Future(BadRequest(Json.obj("message" -> "You should set 'userId' header !"))))
  }

  private def withTime()(implicit req: Request[Any]): Option[DateTime] = {
    req.headers.get("timestamp").map(t => new DateTime(t.toLong))
  }

  private def withData(eventId: EventId, itemType: ItemType, itemId: GenericId)(exec: (Event, EventItem) => Future[Result])(implicit req: Request[Any]) = {
    val futureData = for {
      event <- EventRepository.getByUuid(eventId)
      item <- EventItemRepository.getByUuid(itemType, itemId)
    } yield (event, item)

    futureData.flatMap {
      case (event, item) =>
        if (event.isDefined && item.isDefined) {
          exec(event.get, item.get)
        } else {
          val notFounds = List(
            event.map(_ => None).getOrElse(Some(s"Event <$eventId>")),
            item.map(_ => None).getOrElse(Some(s"<$itemType> <$itemId>"))).flatten
          Future(NotFound(Json.obj("message" -> s"Not found : ${notFounds.mkString(", ")}")))
        }
    }
  }
}
