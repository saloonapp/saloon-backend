package controllers

import common.Utils
import infrastructure.repository.SessionRepository
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.libs.json.Json

object Application extends Controller {

  def home = Action { implicit req =>
    Ok(views.html.Application.home())
  }
  def sample = Action { implicit req =>
    Ok(views.html.Application.sample())
  }

  //def migrate = TODO
  def migrate(eventId: String) = Action.async {
    for {
      m1 <- migrateSessions(eventId)
    } yield {
      Redirect(routes.Application.home).flashing("success" -> "Migrated !")
    }
  }

  private def migrateSessions(eventId: String): Future[List[Option[models.Session]]] = {
    SessionRepository.findByEvent(eventId).flatMap(list => Future.sequence(list.map { e =>
      // sessions : start & end : -7200000
      SessionRepository.update(e.uuid, e.copy(start = e.start.map(_.minusHours(2)), end = e.end.map(_.minusHours(2))))
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
