package controllers.api

import common.models.Page
import common.infrastructure.repository.Repository
import infrastructure.repository.SessionRepository
import models.event.Session
import controllers.api.compatibility.Writer
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.libs.json._

object Sessions extends Controller {
  val repository: Repository[Session] = SessionRepository

  def list(eventId: String, query: Option[String], page: Option[Int], sort: Option[String]) = Action.async { implicit req =>
    SessionRepository.findPageByEvent(eventId, query.getOrElse(""), page.getOrElse(1), Page.defaultSize, sort.getOrElse("-info.start")).map { eltPage =>
      Ok(Json.toJson(eltPage.map(Writer.write)))
    }
  }

  def listAll(eventId: String) = Action.async { implicit req =>
    SessionRepository.findByEvent(eventId).map { elts =>
      Ok(Json.toJson(elts.map(Writer.write)))
    }
  }

  def details(eventId: String, uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).map {
      _.map { elt =>
        Ok(Writer.write(elt))
      }.getOrElse(NotFound)
    }
  }
}
