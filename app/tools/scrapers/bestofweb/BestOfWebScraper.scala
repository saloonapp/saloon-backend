package tools.scrapers.bestofweb

import play.api.mvc.{Controller, Action}
import tools.scrapers.ScraperUtils
import tools.scrapers.bestofweb.models.BestOfWebEvent

object BestOfWebScraper extends Controller {
  val baseUrl = "http://bestofweb.paris"

  def event() = Action.async {
    ScraperUtils.scrapeHtml(baseUrl)(BestOfWebEvent.fromHTML)
  }
}
