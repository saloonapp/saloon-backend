package tools.utils

import tools.scrapers.meta.MetaScraper
import scala.collection.JavaConversions._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.api.Play.current
import play.api.libs.ws._
import play.api.libs.json._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

case class Meta(name: String, value: String)

object GenericScraper extends Controller {

  def getPage(url: String) = Action.async { implicit req =>
    WS.url(url).get().map { response =>
      Ok(response.body)
    }.recover {
      case e => BadRequest("error: " + e.getMessage())
    }
  }

  def getMetas(url: String) = Action.async { implicit req =>
    MetaScraper.get(url).map {
      _.map {
        res => Ok(Json.toJson(res))
      }
    }.getOrElse(Future(BadRequest(Json.obj("message" -> s"Url <$url> seems malformed !"))))
  }
}