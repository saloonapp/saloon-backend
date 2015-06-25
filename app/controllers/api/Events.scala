package controllers.api

import common.models.Page
import common.infrastructure.repository.Repository
import infrastructure.repository.EventRepository
import infrastructure.repository.AttendeeRepository
import infrastructure.repository.SessionRepository
import infrastructure.repository.ExponentRepository
import models.event.Event
import models.event.Session
import models.event.Exponent
import services.EventSrv
import controllers.api.compatibility.Writer
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.libs.json._

object Events extends Controller {
  val repository: Repository[Event] = EventRepository

  def list(query: Option[String], page: Option[Int], sort: Option[String]) = Action.async { implicit req =>
    repository.findPage(query.getOrElse(""), page.getOrElse(1), Page.defaultSize, sort.getOrElse("-info.start"), Json.obj("config.published" -> true)).flatMap { eltPage =>
      eltPage
        .batchMapAsync(EventSrv.addMetadata _)
        .map { page => Ok(Json.toJson(page.map(Writer.write))) }
    }
  }

  def listAll(query: Option[String], sort: Option[String]) = Action.async { implicit req =>
    repository.findAll(query.getOrElse(""), sort.getOrElse("-info.start"), Json.obj("config.published" -> true)).flatMap { elts =>
      EventSrv.addMetadata(elts).map { list => Ok(Json.toJson(list.map(Writer.write))) }
    }
  }

  def details(uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).flatMap {
      _.map { elt =>
        EventSrv.addMetadata(elt).map { eltUI => Ok(Writer.write(eltUI)) }
      }.getOrElse(Future(NotFound))
    }
  }

  def detailsFull(uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).flatMap {
      _.map { elt =>
        for {
          attendees <- AttendeeRepository.findByEvent(elt.uuid)
          sessions <- SessionRepository.findByEvent(elt.uuid)
          exponents <- ExponentRepository.findByEvent(elt.uuid)
        } yield {
          Ok(Writer.write(elt) ++ Json.obj(
            "sessions" -> sessions.map(e => Writer.write(e, attendees.filter(a => e.info.speakers.contains(a.uuid)))),
            "exponents" -> exponents.map(e => Writer.write(e, attendees.filter(a => e.info.team.contains(a.uuid))))))
        }
      }.getOrElse(Future(NotFound(Json.obj("message" -> s"Event $uuid not found !"))))
    }
  }
}
