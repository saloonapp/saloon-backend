package controllers.tools

import tools.scrapers.lanyrd.LanyrdScraper
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.JsValue

object Lanyrd extends Controller {

  def getPlaceEvents(place: String, page: Int = 1, maxPages: Int = 1) = Action.async { implicit req =>
    LanyrdScraper.getEventListMulti(LanyrdScraper.placeUrl(place), page, maxPages).map {
      res => Ok(Json.toJson(res))
    }
  }

  def getEvent(year: String, id: String) = Action.async { implicit req =>
    LanyrdScraper.getEventDetails(LanyrdScraper.eventUrl(year, id)).map {
      res => Ok(Json.toJson(res))
    }
  }

}
