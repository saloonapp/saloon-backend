package controllers.api

import common.models.Page
import common.infrastructure.repository.Repository
import infrastructure.repository.AttendeeRepository
import infrastructure.repository.ExponentRepository
import models.event.Exponent
import controllers.api.compatibility.Writer
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.libs.json._

object Exponents extends Controller {
  val repository: Repository[Exponent] = ExponentRepository

  def list(eventId: String, query: Option[String], page: Option[Int], sort: Option[String], version: String) = Action.async { implicit req =>
    ExponentRepository.findPageByEvent(eventId, query.getOrElse(""), page.getOrElse(1), Page.defaultSize, sort.getOrElse("name")).flatMap { eltPage =>
      AttendeeRepository.findByUuids(eltPage.items.flatMap(_.info.team).toList).map { attendees =>
        Ok(Json.toJson(eltPage.map(e => Writer.write(e, attendees.filter(a => e.info.team.contains(a.uuid)), version))))
      }
    }
  }

  def listAll(eventId: String, version: String) = Action.async { implicit req =>
    ExponentRepository.findByEvent(eventId).flatMap { elts =>
      AttendeeRepository.findByUuids(elts.flatMap(_.info.team)).map { attendees =>
        Ok(Json.toJson(elts.map(e => Writer.write(e, attendees.filter(a => e.info.team.contains(a.uuid)), version))))
      }
    }
  }

  def details(eventId: String, uuid: String, version: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).flatMap {
      _.map { elt =>
        AttendeeRepository.findByUuids(elt.info.team).map { attendees =>
          Ok(Writer.write(elt, attendees, version))
        }
      }.getOrElse(Future(NotFound))
    }
  }
}
