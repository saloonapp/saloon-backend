package tools.scrapers.mixit

import play.api.libs.json.Json
import play.api.mvc.{Controller, Action}
import tools.scrapers.ScraperUtils
import tools.scrapers.mixit.models._
import scala.concurrent.ExecutionContext.Implicits.global

object MixitScraper extends Controller {
  val baseUrl = "https://www.mix-it.fr"

  def sessions(year: Int) = Action.async {
    ScraperUtils.scrapeJson(Session.allUrl(year)){ case (json, url) =>
      json.asOpt[List[Session]].getOrElse(List())
    }
  }

  def speakers(year: Int) = Action.async {
    ScraperUtils.scrapeJson(Attendee.allSpeakerUrl(year)){ case (json, url) => json.asOpt[List[Attendee]].getOrElse(List()) }
  }

  def staff(year: Int) = Action.async {
    ScraperUtils.scrapeJson(Attendee.allStaffUrl(year)){ case (json, url) => json.asOpt[List[Attendee]].getOrElse(List()) }
  }

  def event(year: Int) = Action.async {
    for {
      sessions <- ScraperUtils.fetchJson(Session.allUrl(year)).map(_.asOpt[List[Session]].getOrElse(List()))
      speakers <- ScraperUtils.fetchJson(Attendee.allSpeakerUrl(year)).map(_.asOpt[List[Attendee]].getOrElse(List()))
      staff <- ScraperUtils.fetchJson(Attendee.allStaffUrl(year)).map(_.asOpt[List[Attendee]].getOrElse(List()))
    } yield {
      Ok(Json.toJson(Event.toGenericEvent(year, staff, speakers, sessions))).withHeaders("Content-Type" -> "application/json; charset=utf-8")
    }
  }
}
