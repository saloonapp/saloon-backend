package controllers.api

import infrastructure.repository.common.Repository
import infrastructure.repository.ExponentRepository
import infrastructure.repository.EventRepository
import infrastructure.repository.UserRepository
import infrastructure.repository.UserActionRepository
import models.common.Page
import models.Event
import models.Exponent
import models.ExponentUI
import models.User
import org.joda.time.DateTime
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.libs.json._

object Exponents extends Controller {
  val repository: Repository[Exponent] = ExponentRepository
  val itemType = ExponentUI.className

  def list(eventId: String, query: Option[String], page: Option[Int], sort: Option[String]) = Action.async { implicit req =>
    ExponentRepository.findPageByEvent(eventId, query.getOrElse(""), page.getOrElse(1), Page.defaultSize, sort.getOrElse("name")).map { eltPage =>
      Ok(Json.toJson(eltPage.map(e => ExponentUI.fromModel(e))))
    }
  }

  def listAll(eventId: String) = Action.async { implicit req =>
    ExponentRepository.findByEvent(eventId).map { elts =>
      Ok(Json.toJson(elts.map(e => ExponentUI.fromModel(e))))
    }
  }

  def details(eventId: String, uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).map {
      _.map { elt =>
        Ok(Json.toJson(ExponentUI.fromModel(elt)))
      }.getOrElse(NotFound)
    }
  }

  def favorite(eventId: String, itemId: String) = Action.async { implicit req =>
    withUser() { user =>
      withData(eventId, itemId) { (event, item) =>
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

  def unfavorite(eventId: String, itemId: String) = Action.async { implicit req =>
    withUser() { user =>
      withData(eventId, itemId) { (event, item) =>
        UserActionRepository.deleteFavorite(user.uuid, itemType, item.uuid).map { lastError =>
          if (lastError.ok) { NoContent } else { InternalServerError }
        }
      }
    }
  }

  def mood(eventId: String, itemId: String) = Action.async(parse.json) { implicit req =>
    (req.body \ "rating").asOpt[String].map { rating =>
      withUser() { user =>
        withData(eventId, itemId) { (event, item) =>
          UserActionRepository.getMood(user.uuid, itemType, item.uuid).flatMap { moodOpt =>
            UserActionRepository.setMood(user.uuid, itemType, item.uuid, eventId, moodOpt, rating, withTime()).map { eltOpt =>
              eltOpt.map(elt => Ok(Json.toJson(elt))).getOrElse(InternalServerError)
            }
          }
        }
      }
    }.getOrElse(Future(BadRequest(Json.obj("message" -> "Your request body should have a JSON object with a field 'rating' !"))))
  }

  def createComment(eventId: String, itemId: String) = Action.async(parse.json) { implicit req =>
    (req.body \ "text").asOpt[String].map { text =>
      withUser() { user =>
        withData(eventId, itemId) { (event, item) =>
          UserActionRepository.insertComment(user.uuid, itemType, item.uuid, text, event.uuid, withTime()).map { eltOpt =>
            eltOpt.map(elt => Created(Json.toJson(elt))).getOrElse(InternalServerError)
          }
        }
      }
    }.getOrElse(Future(BadRequest(Json.obj("message" -> "Your request body should have a JSON object with a field 'text' !"))))
  }

  def updateComment(eventId: String, itemId: String, uuid: String) = Action.async(parse.json) { implicit req =>
    (req.body \ "text").asOpt[String].map { text =>
      withUser() { user =>
        withData(eventId, itemId) { (event, item) =>
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

  def deleteComment(eventId: String, itemId: String, uuid: String) = Action.async { implicit req =>
    withUser() { user =>
      withData(eventId, itemId) { (event, item) =>
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

  private def withData(eventId: String, exponentId: String)(exec: (Event, Exponent) => Future[Result])(implicit req: Request[Any]) = {
    val futureData = for {
      event <- EventRepository.getByUuid(eventId)
      exponent <- ExponentRepository.getByUuid(exponentId)
    } yield (event, exponent)

    futureData.flatMap {
      case (event, exponent) =>
        if (event.isDefined && exponent.isDefined) {
          exec(event.get, exponent.get)
        } else {
          val notFounds = List(
            event.map(_ => None).getOrElse(Some(s"Event <$eventId>")),
            exponent.map(_ => None).getOrElse(Some(s"Exponent <$exponentId>"))).flatten
          Future(NotFound(Json.obj("message" -> s"Not found : ${notFounds.mkString(", ")}")))
        }
    }
  }
}
