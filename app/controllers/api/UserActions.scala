package controllers.api

import infrastructure.repository.common.Repository
import infrastructure.repository.EventRepository
import infrastructure.repository.EventItemRepository
import infrastructure.repository.UserRepository
import infrastructure.repository.UserActionRepository
import models.Event
import models.EventItem
import models.User
import org.joda.time.DateTime
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.libs.json._

object UserActions extends Controller {
  def favorite(eventId: String, itemType: String, itemId: String) = Action.async { implicit req =>
    withUser() { user =>
      withData(eventId, itemType, itemId) { (event, item) =>
        UserActionRepository.getFavorite(user.uuid, itemType, item.uuid).flatMap {
          _.map { elt => Future(Ok(Json.toJson(elt))) }.getOrElse {
            UserActionRepository.insertFavorite(user.uuid, itemType, item.uuid, event.uuid, withTime()).map { eltOpt =>
              eltOpt.map(elt => Created(Json.toJson(elt))).getOrElse(InternalServerError)
            }
          }
        }
      }
    }
  }

  def unfavorite(eventId: String, itemType: String, itemId: String) = Action.async { implicit req =>
    withUser() { user =>
      withData(eventId, itemType, itemId) { (event, item) =>
        UserActionRepository.deleteFavorite(user.uuid, itemType, item.uuid).map { lastError =>
          if (lastError.ok) { NoContent } else { InternalServerError }
        }
      }
    }
  }

  def done(eventId: String, itemType: String, itemId: String) = Action.async { implicit req =>
    withUser() { user =>
      withData(eventId, itemType, itemId) { (event, item) =>
        UserActionRepository.getDone(user.uuid, itemType, item.uuid).flatMap {
          _.map { elt => Future(Ok(Json.toJson(elt))) }.getOrElse {
            UserActionRepository.insertDone(user.uuid, itemType, item.uuid, event.uuid, withTime()).map { eltOpt =>
              eltOpt.map(elt => Created(Json.toJson(elt))).getOrElse(InternalServerError)
            }
          }
        }
      }
    }
  }

  def undone(eventId: String, itemType: String, itemId: String) = Action.async { implicit req =>
    withUser() { user =>
      withData(eventId, itemType, itemId) { (event, item) =>
        UserActionRepository.deleteDone(user.uuid, itemType, item.uuid).map { lastError =>
          if (lastError.ok) { NoContent } else { InternalServerError }
        }
      }
    }
  }

  def mood(eventId: String, itemType: String, itemId: String) = Action.async(parse.json) { implicit req =>
    (req.body \ "rating").asOpt[String].map { rating =>
      withUser() { user =>
        withData(eventId, itemType, itemId) { (event, item) =>
          UserActionRepository.getMood(user.uuid, itemType, item.uuid).flatMap { moodOpt =>
            UserActionRepository.setMood(user.uuid, itemType, item.uuid, eventId, moodOpt, rating, withTime()).map { eltOpt =>
              eltOpt.map(elt => Ok(Json.toJson(elt))).getOrElse(InternalServerError)
            }
          }
        }
      }
    }.getOrElse(Future(BadRequest(Json.obj("message" -> "Your request body should have a JSON object with a field 'rating' !"))))
  }

  def createComment(eventId: String, itemType: String, itemId: String) = Action.async(parse.json) { implicit req =>
    (req.body \ "text").asOpt[String].map { text =>
      withUser() { user =>
        withData(eventId, itemType, itemId) { (event, item) =>
          UserActionRepository.insertComment(user.uuid, itemType, item.uuid, text, event.uuid, withTime()).map { eltOpt =>
            eltOpt.map(elt => Created(Json.toJson(elt))).getOrElse(InternalServerError)
          }
        }
      }
    }.getOrElse(Future(BadRequest(Json.obj("message" -> "Your request body should have a JSON object with a field 'text' !"))))
  }

  def updateComment(eventId: String, itemType: String, itemId: String, uuid: String) = Action.async(parse.json) { implicit req =>
    (req.body \ "text").asOpt[String].map { text =>
      withUser() { user =>
        withData(eventId, itemType, itemId) { (event, item) =>
          UserActionRepository.getComment(user.uuid, itemType, item.uuid, uuid).flatMap {
            _.map { userAction =>
              UserActionRepository.updateComment(user.uuid, itemType, item.uuid, uuid, userAction, text, withTime()).map { eltOpt =>
                eltOpt.map(elt => Ok(Json.toJson(elt))).getOrElse(InternalServerError)
              }
            }.getOrElse(Future(NotFound(Json.obj("message" -> s"Unable to find comment with id $uuid"))))
          }
        }
      }
    }.getOrElse(Future(BadRequest(Json.obj("message" -> "Your request body should have a JSON object with a field 'text' !"))))
  }

  def deleteComment(eventId: String, itemType: String, itemId: String, uuid: String) = Action.async { implicit req =>
    withUser() { user =>
      withData(eventId, itemType, itemId) { (event, item) =>
        UserActionRepository.deleteComment(user.uuid, itemType, item.uuid, uuid).map { lastError =>
          if (lastError.ok) { if (lastError.n == 0) NotFound else NoContent } else { InternalServerError }
        }
      }
    }
  }

  private def withUser()(exec: (User) => Future[Result])(implicit req: Request[Any]) = {
    req.headers.get("userId").map { userId =>
      UserRepository.getByUuid(userId).flatMap {
        _.map { user => exec(user) }.getOrElse(Future(NotFound(Json.obj("message" -> s"User <$userId> not found !"))))
      }
    }.getOrElse(Future(BadRequest(Json.obj("message" -> "You should set 'userId' header !"))))
  }

  private def withTime()(implicit req: Request[Any]): Option[DateTime] = {
    req.headers.get("timestamp").map(t => new DateTime(t.toLong))
  }

  private def withData(eventId: String, itemType: String, itemId: String)(exec: (Event, EventItem) => Future[Result])(implicit req: Request[Any]) = {
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
