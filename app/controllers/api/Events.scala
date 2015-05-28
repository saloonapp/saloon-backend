package controllers.api

import common.models.Page
import common.infrastructure.repository.Repository
import infrastructure.repository.EventRepository
import infrastructure.repository.SessionRepository
import infrastructure.repository.ExponentRepository
import models.Event
import models.EventUI
import models.SessionUI
import models.ExponentUI
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
    repository.findPage(query.getOrElse(""), page.getOrElse(1), Page.defaultSize, sort.getOrElse("-start"), Json.obj("published" -> true)).flatMap { eltPage =>
      eltPage.batchMapAsync(EventSrv.addMetadata _).map { eltUIPage => Ok(Json.toJson(eltUIPage)) }
    }
  }

  def listAll(query: Option[String], sort: Option[String]) = Action.async { implicit req =>
    repository.findAll(query.getOrElse(""), sort.getOrElse("-start"), Json.obj("published" -> true)).flatMap { elts =>
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
            "className" -> EventUI.className,
            "sessions" -> sessions.map(e => SessionUI.fromModel(e)),
            "exponents" -> exponents.map(e => ExponentUI.fromModel(e))))
        }
      }.getOrElse(Future(NotFound))
    }
  }
}
