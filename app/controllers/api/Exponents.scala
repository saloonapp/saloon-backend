package controllers.api

import infrastructure.repository.common.Repository
import infrastructure.repository.ExponentRepository
import infrastructure.repository.EventRepository
import infrastructure.repository.UserRepository
import infrastructure.repository.UserActionRepository
import models.Event
import models.Exponent
import models.User
import models.UserAction
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.libs.json._

object Exponents extends Controller {
  val repository: Repository[Exponent] = ExponentRepository
  val itemType = "exponent"

  def list(eventId: String, query: Option[String], page: Option[Int], sort: Option[String]) = Action.async { implicit req =>
    ExponentRepository.findPageByEvent(eventId, query.getOrElse(""), page.getOrElse(1), sort.getOrElse("name")).map { eltPage =>
      Ok(Json.toJson(eltPage))
    }
  }

  def listAll(eventId: String) = Action.async { implicit req =>
    ExponentRepository.findByEvent(eventId).map { elts =>
      Ok(Json.toJson(elts))
    }
  }

  def details(eventId: String, uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).map { elt =>
      Ok(Json.toJson(elt))
    }
  }

  def actions(eventId: String, uuid: String) = Action.async { implicit req =>
    UserActionRepository.findByItem(itemType, uuid).map { actions =>
      val res: Map[String, List[UserAction]] = actions.groupBy(_.actionType)
      Ok(Json.obj("users" -> res))
    }
  }

  def favorite(eventId: String, uuid: String) = Action.async { implicit req =>
    checkData(eventId, uuid) { (event, exponent, user) =>
      UserActionRepository.getFavorite(user.uuid, itemType, exponent.uuid).flatMap { fav =>
        fav.map { _ => Future(Ok) }.getOrElse {
          UserActionRepository.insert(UserAction.fav(user.uuid, itemType, exponent.uuid, event.uuid)).map { elt =>
            if (elt.isDefined) { Created } else { InternalServerError }
          }
        }
      }
    }
  }

  def unfavorite(eventId: String, uuid: String) = Action.async { implicit req =>
    checkData(eventId, uuid) { (event, exponent, user) =>
      UserActionRepository.deleteFavorite(user.uuid, itemType, exponent.uuid).map { lastError =>
        if (lastError.ok) { NoContent } else { InternalServerError }
      }
    }
  }

  private def checkData(eventId: String, exponentId: String)(exec: (Event, Exponent, User) => Future[Result])(implicit req: Request[AnyContent]) = {
    req.headers.get("userId").map { userId =>
      val futureData = for {
        event <- EventRepository.getByUuid(eventId)
        exponent <- ExponentRepository.getByUuid(exponentId)
        user <- UserRepository.getByUuid(userId)
      } yield (event, exponent, user)

      futureData.flatMap {
        case (event, exponent, user) =>
          if (event.isDefined && exponent.isDefined && user.isDefined) {
            exec(event.get, exponent.get, user.get)
          } else {
            val notFounds = List(
              event.map(_ => None).getOrElse(Some(s"Event <$eventId>")),
              exponent.map(_ => None).getOrElse(Some(s"Exponent <$exponentId>")),
              user.map(_ => None).getOrElse(Some(s"User <$userId>"))).flatten
            Future(NotFound(Json.obj("message" -> s"Not found : ${notFounds.mkString(", ")}")))
          }
      }
    }.getOrElse(Future(BadRequest(Json.obj("message" -> "You should set 'userId' header !"))))
  }
}
