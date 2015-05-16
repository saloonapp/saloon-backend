package controllers.api

import infrastructure.repository.common.Repository
import infrastructure.repository.EventRepository
import infrastructure.repository.SessionRepository
import infrastructure.repository.ExponentRepository
import models.common.Page
import models.Event
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
    repository.findPage(query.getOrElse(""), page.getOrElse(1), Page.defaultSize, sort.getOrElse("-start")).flatMap { eltPage =>
      eltPage.batchMapAsync(EventSrv.addMetadata _).map { eltUIPage => Ok(Json.toJson(eltUIPage)) }
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
          Ok(Json.toJson(elt).as[JsObject] ++ Json.obj(
            "sessions" -> sessions,
            "exponents" -> exponents))
        }
      }.getOrElse(Future(NotFound))
    }
  }
}
