package controllers

import common.Utils
import infrastructure.repository.SessionRepository
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.DateTimeZone

object Application extends Controller {

  def home = Action { implicit req =>
    Ok(views.html.Application.home())
  }
  def sample = Action { implicit req =>
    Ok(views.html.Application.sample())
  }

  def migrate = Action {
    val dateStr = "2015-06-10 13:00:00.0"
    val pattern = "yyyy-MM-dd HH:mm:ss.SSS"
    val f1 = DateTimeFormat.forPattern(pattern)
    val f2 = DateTimeFormat.forPattern(pattern).withLocale(java.util.Locale.FRANCE)
    val f3 = DateTimeFormat.forPattern(pattern).withZone(DateTimeZone.forID("Europe/Paris"))
    val d1 = f1.parseDateTime(dateStr)
    val d2 = f2.parseDateTime(dateStr)
    val d3 = f3.parseDateTime(dateStr)
    Ok(Json.obj(
      "zone" -> DateTimeZone.getDefault().toString,
      "d1" -> Json.obj("str" -> d1.toString(), "iso" -> ISODateTimeFormat.dateTime().print(d1), "json" -> d1),
      "d2" -> Json.obj("str" -> d2.toString(), "iso" -> ISODateTimeFormat.dateTime().print(d2), "json" -> d2),
      "d3" -> Json.obj("str" -> d3.toString(), "iso" -> ISODateTimeFormat.dateTime().print(d3), "json" -> d3)))
  }
  /*def migrate(eventId: String) = Action.async {
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
  }*/

  def corsPreflight(all: String) = Action {
    Ok("").withHeaders(
      "Allow" -> "*",
      "Access-Control-Allow-Origin" -> "*",
      "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers" -> "Origin, X-Requested-With, Content-Type, Accept, Referrer, User-Agent, userId, timestamp");
  }
}
