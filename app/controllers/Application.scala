package controllers

import infrastructure.repository.UserActionRepository
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

  // def migrate = TODO
  def migrate = Action.async {
    for {
      m1 <- migrateUserActions()
    } yield {
      Redirect(routes.Application.home).flashing("success" -> "Migrated !")
    }
  }

  private def migrateUserActions(): Future[List[Option[models.UserAction]]] = {
    UserActionRepository.findAll().flatMap(list => Future.sequence(list.map { e =>
      UserActionRepository.update(e.uuid, e.copy(itemType = e.itemType.toLowerCase()))
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
