package controllers.api

import infrastructure.repository.common.Repository
import infrastructure.repository.EventRepository
import infrastructure.repository.SessionRepository
import infrastructure.repository.ExponentRepository
import models.Event
import models.EventUI
import models.EventData
import services.EventSrv
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.libs.json._

object Events extends Controller {
  val repository: Repository[Event] = EventRepository

  def list(query: Option[String], page: Option[Int], sort: Option[String]) = Action.async { implicit req =>
    repository.findPage(query.getOrElse(""), page.getOrElse(1), sort.getOrElse("-start")).flatMap { eltPage =>
      eltPage.mapSeqAsync(EventSrv.addMetadata _).map { eltUIPage => Ok(Json.toJson(eltUIPage)) }
    }
  }

  def listAll(query: Option[String], sort: Option[String]) = Action.async { implicit req =>
    repository.findAll(query.getOrElse(""), sort.getOrElse("-start")).flatMap { elts =>
      EventSrv.addMetadata(elts).map { eltsUI => Ok(Json.toJson(eltsUI)) }
    }
  }

  def details(uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).flatMap {
      _.map { elt =>
        EventSrv.addMetadata(elt).map { eltUI => Ok(Json.toJson(eltUI)) }
      }.getOrElse(Future(NotFound))
    }
  }

  def detailsFull(uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).flatMap {
      _.map { elt =>
        for {
          sessions <- SessionRepository.findByEvent(elt.uuid)
          exponents <- ExponentRepository.findByEvent(elt.uuid)
        } yield {
          Ok(Json.obj(
            "event" -> elt,
            "sessions" -> sessions,
            "exponents" -> exponents))
        }
      }.getOrElse(Future(NotFound))
    }
  }

  def getSessions(eventId: String, query: Option[String], page: Option[Int], sort: Option[String]) = Action.async { implicit req =>
    SessionRepository.findPageByEvent(eventId, query.getOrElse(""), page.getOrElse(1), sort.getOrElse("-start")).map { eltPage =>
      Ok(Json.toJson(eltPage))
    }
  }

  def getAllSessions(eventId: String) = Action.async { implicit req =>
    SessionRepository.findByEvent(eventId).map { sessions =>
      Ok(Json.toJson(sessions))
    }
  }

  def getSession(eventId: String, uuid: String) = Action.async { implicit req =>
    SessionRepository.getByUuid(uuid).map { session =>
      Ok(Json.toJson(session))
    }
  }

  def getExponents(eventId: String, query: Option[String], page: Option[Int], sort: Option[String]) = Action.async { implicit req =>
    ExponentRepository.findPageByEvent(eventId, query.getOrElse(""), page.getOrElse(1), sort.getOrElse("name")).map { eltPage =>
      Ok(Json.toJson(eltPage))
    }
  }

  def getAllExponents(eventId: String) = Action.async { implicit req =>
    ExponentRepository.findByEvent(eventId).map { exponents =>
      Ok(Json.toJson(exponents))
    }
  }

  def getExponent(eventId: String, uuid: String) = Action.async { implicit req =>
    ExponentRepository.getByUuid(uuid).map { exponent =>
      Ok(Json.toJson(exponent))
    }
  }
}
