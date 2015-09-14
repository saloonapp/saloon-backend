package tools.controllers

import tools.scrapers.innorobo.InnoroboScraper
import common.services.FileExporter
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.JsValue

object Innorobo extends Controller {

  def getExibitors() = Action.async { implicit req =>
    InnoroboScraper.getExibitors().map { exhibitorUrls =>
      Ok(Json.toJson(exhibitorUrls))
    }
  }

  def getExibitor(name: String) = Action.async { implicit req =>
    InnoroboScraper.getExibitor(InnoroboScraper.exibitorUrl(name)).map { exhibitor =>
      Ok(Json.toJson(exhibitor))
    }
  }

  def getFullExibitors(offset: Int, limit: Int, format: String) = Action.async { implicit req =>
    InnoroboScraper.getExibitors().flatMap { exhibitorUrls =>
      val futureExhibitors = exhibitorUrls.drop(offset).take(limit).map(url => InnoroboScraper.getExibitor(url))
      Future.sequence(futureExhibitors).map { exhibitors =>
        if (format == "csv") {
          val filename = "Innorobo_exhibitors.csv"
          val content = FileExporter.makeCsv(exhibitors.map(_.toMap))
          Ok(content)
            .withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + "\""))
            .as("text/csv")
        } else {
          Ok(Json.toJson(exhibitors))
        }
      }
    }
  }


}
