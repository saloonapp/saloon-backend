package tools.scrapers.rivieradev

import play.api.mvc.{Controller, Action}
import tools.scrapers.ScraperUtils
import tools.scrapers.rivieradev.models.{RivieraDevEvent, RivieraDevSponsor, RivieraDevSpeaker, RivieraDevSession}
import scala.concurrent.ExecutionContext.Implicits.global

object RivieraDevScraper extends Controller {
  def baseUrl(year: Int) = s"http://$year.rivieradev.fr"
  val name = "RivieraDevScraper"

  def event(year: Int) = Action.async {
    val sessionsFut = ScraperUtils.parseHtml(baseUrl(year)+"/sessions")(RivieraDevSession.fromHTML)
    val speakersFut = ScraperUtils.parseListHtml(baseUrl(year)+"/orateurs")(RivieraDevSpeaker.linkList)(RivieraDevSpeaker.fromHTML)
    val sponsorsFut = ScraperUtils.parseHtml(baseUrl(year)+"/sponsors")(RivieraDevSponsor.fromHTML)
    for {
      sessions <- sessionsFut
      speakers <- speakersFut
      sponsors <- sponsorsFut
      aboutPage <- ScraperUtils.fetchHtml(baseUrl(year)+"/a-propos")
    } yield ScraperUtils.format(RivieraDevEvent.build(year, aboutPage, sessions, speakers, sponsors))
  }

  def sessions(year: Int) = Action.async {
    ScraperUtils.scrapeHtml(baseUrl(year)+"/sessions")(RivieraDevSession.fromHTML)
  }

  def speakers(year: Int) = Action.async {
    ScraperUtils.scrapeListHtml(baseUrl(year)+"/orateurs")(RivieraDevSpeaker.linkList)(RivieraDevSpeaker.fromHTML)
  }

  def speaker(year: Int, id: Int) = Action.async {
    ScraperUtils.scrapeHtml(baseUrl(year)+"/orateur/"+id)(RivieraDevSpeaker.fromHTML)
  }

  def sponsors(year: Int) = Action.async {
    ScraperUtils.scrapeHtml(baseUrl(year)+"/sponsors")(RivieraDevSponsor.fromHTML)
  }
}
