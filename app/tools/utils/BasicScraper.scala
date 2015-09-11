package tools.utils

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.api.Play.current
import play.api.libs.ws._

import play.api.libs.json._

object BasicScraper extends Controller {

  def getPage(url: String) = Action.async { implicit req =>
    WS.url(url).get().map { response =>
      Ok(response.body)
    }.recover {
      case e => BadRequest("error: " + e.getMessage())
    }
  }

}