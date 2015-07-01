package api.controllers

import common.models.utils.Page
import common.repositories.Repository
import common.repositories.event.AttendeeRepository
import common.repositories.event.SessionRepository
import common.models.event.Session
import api.controllers.compatibility.Writer
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.libs.json._

object Sessions extends Controller {
  val repository: Repository[Session] = SessionRepository

  def list(eventId: String, query: Option[String], page: Option[Int], sort: Option[String], version: String) = Action.async { implicit req =>
    SessionRepository.findPageByEvent(eventId, query.getOrElse(""), page.getOrElse(1), Page.defaultSize, sort.getOrElse("-info.start")).flatMap { eltPage =>
      AttendeeRepository.findByUuids(eltPage.items.flatMap(_.info.speakers).toList).map { attendees =>
        Ok(Json.toJson(eltPage.map(e => Writer.write(e, attendees.filter(a => e.info.speakers.contains(a.uuid)), version))))
      }
    }
  }

  def listAll(eventId: String, version: String) = Action.async { implicit req =>
    SessionRepository.findByEvent(eventId).flatMap { elts =>
      AttendeeRepository.findByUuids(elts.flatMap(_.info.speakers)).map { attendees =>
        Ok(Json.toJson(elts.map(e => Writer.write(e, attendees.filter(a => e.info.speakers.contains(a.uuid)), version))))
      }
    }
  }

  def details(eventId: String, uuid: String, version: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).flatMap {
      _.map { elt =>
        AttendeeRepository.findByUuids(elt.info.speakers).map { attendees =>
          Ok(Writer.write(elt, attendees, version))
        }
      }.getOrElse(Future(NotFound))
    }
  }
}
