package controllers

import infrastructure.repository.EventRepository
import infrastructure.repository.SessionRepository
import infrastructure.repository.ExponentRepository
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._

object Application extends Controller {

  def home = Action { implicit req =>
    Ok(views.html.Application.home())
  }
  def sample = Action { implicit req =>
    Ok(views.html.Application.sample())
  }

  def migrate = Action.async {
    for {
      events <- migrateEvents()
      exponents <- migrateExponents()
      sessions <- migrateSessions()
    } yield {
      Redirect(routes.Application.home).flashing("success" -> "Migrated !")
    }
  }

  private def migrateEvents(): Future[List[Option[models.Event]]] = {
    EventRepository.findAll().flatMap(list => Future.sequence(list.map { e =>
      EventRepository.update(e.uuid, e.copy(
        image = Some(e.logo.orElse(e.image).getOrElse("")),
        published = Some(true)))
    }))
  }

  private def migrateExponents(): Future[List[Option[models.Exponent]]] = {
    ExponentRepository.findAll().flatMap(list => Future.sequence(list.map { e =>
      ExponentRepository.update(e.uuid, e.copy(
        image = Some("")))
    }))
  }

  private def migrateSessions(): Future[List[Option[models.Session]]] = {
    SessionRepository.findAll().flatMap(list => Future.sequence(list.map { e =>
      SessionRepository.update(e.uuid, e.copy(
        image = Some(""),
        name = Some(e.title.orElse(e.name).getOrElse("")),
        description = Some(e.summary.orElse(e.description).getOrElse("")),
        title = None,
        summary = None))
    }))
  }

  def corsPreflight(all: String) = Action {
    Ok("").withHeaders(
      "Allow" -> "*",
      "Access-Control-Allow-Origin" -> "*",
      "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers" -> "Origin, X-Requested-With, Content-Type, Accept, Referrer, User-Agent, userId");
  }
}
