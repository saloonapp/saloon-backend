package controllers

import common.Utils
import infrastructure.repository.SessionRepository
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

  //def migrate(eventId: String) = TODO
  def migrate(eventId: String) = Action.async {
    for {
      m1 <- migrateSessions(eventId)
    } yield {
      Redirect(routes.Application.home).flashing("success" -> "Migrated !")
    }
  }

  private def migrateSessions(eventId: String): Future[List[Option[models.Session]]] = {
    SessionRepository.findByEvent(eventId).flatMap(list => Future.sequence(list.map { e =>
      SessionRepository.update(e.uuid, e.copy(description = Utils.htmlToText(e.description)))
    }))
  }

  def corsPreflight(all: String) = Action {
    Ok("").withHeaders(
      "Allow" -> "*",
      "Access-Control-Allow-Origin" -> "*",
      "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers" -> "Origin, X-Requested-With, Content-Type, Accept, Referrer, User-Agent, userId, timestamp");
  }
}
