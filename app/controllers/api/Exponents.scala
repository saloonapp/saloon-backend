package controllers.api

import infrastructure.repository.common.Repository
import infrastructure.repository.ExponentRepository
import models.Exponent
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.libs.json._

object Exponents extends Controller {
  val repository: Repository[Exponent] = ExponentRepository

  def list(eventId: String, query: Option[String], page: Option[Int], sort: Option[String]) = Action.async { implicit req =>
    ExponentRepository.findPageByEvent(eventId, query.getOrElse(""), page.getOrElse(1), sort.getOrElse("name")).map { eltPage =>
      Ok(Json.toJson(eltPage))
    }
  }

  def listAll(eventId: String) = Action.async { implicit req =>
    ExponentRepository.findByEvent(eventId).map { elts =>
      Ok(Json.toJson(elts))
    }
  }

  def details(eventId: String, uuid: String) = Action.async { implicit req =>
    repository.getByUuid(uuid).map { elt =>
      Ok(Json.toJson(elt))
    }
  }
}
