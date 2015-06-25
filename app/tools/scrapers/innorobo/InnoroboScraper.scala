package tools.scrapers.innorobo

import tools.scrapers.innorobo.models.InnoroboExhibitor
import tools.scrapers.innorobo.pages.InnoroboExhibitorListPage
import tools.scrapers.innorobo.pages.InnoroboExhibitorDetailsPage
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current
import play.api.libs.ws._

object InnoroboScraper {
  val baseUrl = "http://innorobo.com"
  val exibitorListUrl = s"$baseUrl/2015-exhibitors/"
  def exibitorUrl(name: String): String = s"$baseUrl/exhibitors/$name/"

  def getExibitors(): Future[List[String]] = {
    WS.url(exibitorListUrl).get().map { response =>
      InnoroboExhibitorListPage.extract(response.body)
    }
  }

  def getExibitor(url: String): Future[InnoroboExhibitor] = {
    WS.url(url).get().map { response =>
      InnoroboExhibitorDetailsPage.extract(response.body, url)
    }
  }
}