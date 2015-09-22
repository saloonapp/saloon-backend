package tools.scrapers.salonreunir

import tools.scrapers.salonreunir.models.SalonReunirEvent
import tools.scrapers.salonreunir.models.SalonReunirSession
import tools.scrapers.salonreunir.models.SalonReunirExponent
import scala.util.Try
import scala.util.Success
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.api.libs.json.Json

object SalonReunirScraper extends Controller {
  val baseUrl = "http://salon.reunir.com"
  val exponentsUrl = s"$baseUrl/liste-des-exposants/"
  val sessionsUrl = s"$baseUrl/conferences/"

  def getEventFull(useCache: Boolean) = Action.async {
    val sequentially = true
    for {
      exponentLinksTry: Try[List[String]] <- SalonReunirExponentScraper.fetchLinkList(exponentsUrl, useCache)
      exponents: List[SalonReunirExponent] <- exponentLinksTry match {
        case Success(urls) => SalonReunirExponentScraper.fetchDetailsList(urls, sequentially, useCache).map(_.map { case (url, res) => res.toOption }.flatten)
        case _ => Future(List())
      }
      sessions: List[SalonReunirSession] <- SalonReunirSessionScraper.fetchListDetails(sessionsUrl, useCache).map(_.toOption.getOrElse(List()))
    } yield {
      Ok(Json.toJson(SalonReunirEvent.toGenericEvent("21ème Salon Réunir", "SalonReunir2015", baseUrl, exponents, sessions)))
    }
  }
}