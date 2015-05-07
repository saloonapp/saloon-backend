package controllers.api

import infrastructure.repository.common.Repository
import infrastructure.repository.SessionRepository
import models.Session
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.libs.json._

object Sessions extends Controller {
  val repository: Repository[Session] = SessionRepository

  def list(eventId: String, query: Option[String], page: Option[Int], sort: Option[String]) = Action.async { implicit req =>
    SessionRepository.findPageByEvent(eventId, query.getOrElse(""), page.getOrElse(1), sort.getOrElse("-start")).map { eltPage =>
      Ok(Json.toJson(eltPage))
    }
  }

  def listAll(eventId: String) = Action.async { implicit req =>
    SessionRepository.findByEvent(eventId).map { elts =>
      Ok(Json.toJson(elts))
    }
  }

  def details(eventId: String, uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).map { elt =>
      Ok(Json.toJson(elt))
    }
  }
}
