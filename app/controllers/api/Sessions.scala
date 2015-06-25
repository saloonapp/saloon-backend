package controllers.api

import common.models.Page
import common.infrastructure.repository.Repository
import infrastructure.repository.AttendeeRepository
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
    SessionRepository.findPageByEvent(eventId, query.getOrElse(""), page.getOrElse(1), Page.defaultSize, sort.getOrElse("-info.start")).flatMap { eltPage =>
      AttendeeRepository.findByUuids(eltPage.items.flatMap(_.info.speakers).toList).map { attendees =>
        Ok(Json.toJson(eltPage.map(e => Writer.write(e, attendees.filter(a => e.info.speakers.contains(a.uuid))))))
      }
    }
  }

  def listAll(eventId: String) = Action.async { implicit req =>
    SessionRepository.findByEvent(eventId).flatMap { elts =>
      AttendeeRepository.findByUuids(elts.flatMap(_.info.speakers)).map { attendees =>
        Ok(Json.toJson(elts.map(e => Writer.write(e, attendees.filter(a => e.info.speakers.contains(a.uuid))))))
      }
    }
  }

  def details(eventId: String, uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).flatMap {
      _.map { elt =>
        AttendeeRepository.findByUuids(elt.info.speakers).map { attendees =>
          Ok(Writer.write(elt, attendees))
        }
      }.getOrElse(Future(NotFound))
    }
  }
}
