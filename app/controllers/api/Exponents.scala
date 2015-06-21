package controllers.api

import common.models.Page
import common.infrastructure.repository.Repository
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

  def list(eventId: String, query: Option[String], page: Option[Int], sort: Option[String]) = Action.async { implicit req =>
    ExponentRepository.findPageByEvent(eventId, query.getOrElse(""), page.getOrElse(1), Page.defaultSize, sort.getOrElse("name")).map { eltPage =>
      Ok(Json.toJson(eltPage.map(Writer.write)))
    }
  }

  def listAll(eventId: String) = Action.async { implicit req =>
    ExponentRepository.findByEvent(eventId).map { elts =>
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
