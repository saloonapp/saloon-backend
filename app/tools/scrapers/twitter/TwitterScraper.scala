package tools.scrapers.twitter

import play.api.mvc.{Action, Controller}
import tools.scrapers.ScraperUtils
import tools.scrapers.twitter.models.TwitterProfil

object TwitterScraper extends Controller {
  val baseUrl = "https://twitter.com/"
  def profil(account: String) = Action.async {
    ScraperUtils.scrapeHtml(baseUrl+"/"+account.trim.replace("@", "").replaceAll("https?://twitter.com/", ""))(TwitterProfil.fromHTML)
  }
}
