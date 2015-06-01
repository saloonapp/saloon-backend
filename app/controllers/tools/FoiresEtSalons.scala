package controllers.tools

import tools.scrapers.foiresetsalons.FESScraper
import services.FileExporter
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.JsValue

object FoiresEtSalons extends Controller {

  def getEvent(url: String) = Action.async { implicit req =>
    FESScraper.getEvent(url).map {
      _.map { res => Ok(Json.toJson(res)) }.getOrElse(NotFound)
    }
  }

  // page can be 'catalogue' or 'salon'
  def getEvents(page: String, format: String) = Action.async { implicit req =>
    FESScraper.getEvents(FESScraper.pageUrl(page), page).map { elts =>
      if (format == "json") {
        Ok(Json.toJson(elts))
      } else {
        val filename = page + ".csv"
        val content = FileExporter.makeCsv(elts.map(_.toMap))
        Ok(content)
          .withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + "\""))
          .as("text/csv")
      }
    }
  }
  def getEventsFull(page: String, offset: Int, size: Int, format: String) = Action.async { implicit req =>
    FESScraper.getEventsFull(FESScraper.pageUrl(page), page, offset, size).map { elts =>
      if (format == "json") {
        Ok(Json.toJson(elts))
      } else {
        val filename = page + ".csv"
        val content = FileExporter.makeCsv(elts.map(_.toMap))
        Ok(content)
          .withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + "\""))
          .as("text/csv")
      }
    }
  }

}
