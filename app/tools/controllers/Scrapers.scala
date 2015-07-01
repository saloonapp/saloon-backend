package tools.controllers

import tools.scrapers.meta.MetaScraper
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.JsValue

object Scrapers extends Controller {

  def getMetas(url: String) = Action.async { implicit req =>
    MetaScraper.get(url).map {
      _.map {
        res => Ok(Json.toJson(res))
      }
    }.getOrElse(Future(BadRequest(Json.obj("message" -> s"Url <$url> seems malformed !"))))
  }

}
