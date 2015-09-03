package api.controllers

import common.models.utils.Page
import common.models.event.Event
import common.models.event.EventId
import common.models.event.Session
import common.models.event.Exponent
import common.repositories.Repository
import common.repositories.event.EventRepository
import common.repositories.event.AttendeeRepository
import common.repositories.event.SessionRepository
import common.repositories.event.ExponentRepository
import common.services.EventSrv
import api.controllers.compatibility.Writer
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.libs.json._

object Events extends Controller {
  val repository: Repository[Event, EventId] = EventRepository

  def list(query: Option[String], page: Option[Int], sort: Option[String], version: String) = Action.async { implicit req =>
    repository.findPage(query.getOrElse(""), page.getOrElse(1), Page.defaultSize, sort.getOrElse("-info.start"), Json.obj("config.published" -> true)).flatMap { eltPage =>
      eltPage
        .batchMapAsync(EventSrv.addMetadata _)
        .map { page => Ok(Json.toJson(page.map(e => Writer.write(e, version)))) }
    }
  }

  def listAll(query: Option[String], sort: Option[String], version: String) = Action.async { implicit req =>
    repository.findAll(query.getOrElse(""), sort.getOrElse("-info.start"), Json.obj("config.published" -> true)).flatMap { elts =>
      EventSrv.addMetadata(elts).map { list => Ok(Json.toJson(list.map(e => Writer.write(e, version)))) }
    }
  }

  def details(uuid: EventId, version: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).flatMap {
      _.map { elt =>
        EventSrv.addMetadata(elt).map { eltUI => Ok(Writer.write(eltUI, version)) }
      }.getOrElse(Future(NotFound))
    }
  }

  def detailsFull(uuid: EventId, version: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).flatMap {
      _.map { elt =>
        for {
          attendees <- AttendeeRepository.findByEvent(elt.uuid)
          sessions <- SessionRepository.findByEvent(elt.uuid)
          exponents <- ExponentRepository.findByEvent(elt.uuid)
        } yield Ok(Writer.write(elt, attendees, sessions, exponents, version))
      }.getOrElse(Future(NotFound(Json.obj("message" -> s"Event $uuid not found !"))))
    }
  }
}
