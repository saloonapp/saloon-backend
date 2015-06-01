package tools.scrapers.foiresetsalons

import tools.scrapers.foiresetsalons.models.FESEventItem
import tools.scrapers.foiresetsalons.models.FESEvent
import tools.scrapers.foiresetsalons.pages.FESListPage
import tools.scrapers.foiresetsalons.pages.FESDetailsPage
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current
import play.api.libs.ws._
import scala.concurrent.duration._
import scala.concurrent.Await

object FESScraper {
  val baseUrl = "https://www.foiresetsalons.entreprises.gouv.fr"
  def pageUrl(page: String): String = s"$baseUrl/$page.php"

  def getEvents(url: String, page: String): Future[List[FESEventItem]] = {
    WS.url(url).get().map { response =>
      FESListPage.extract(fixEncodage(response.body), page)
    }
  }

  def getEventsFull(url: String, page: String, offset: Int, size: Int): Future[List[FESEvent]] = {
    /*WS.url(url).get().flatMap { response =>
      val listFuture = FESListPage.extract(fixEncodage(response.body), page).map(_.url).drop(offset).take(size).map(url => getEvent(url))
      Future.sequence(listFuture).map(_.flatten)
    }*/
    WS.url(url).get().map { response =>
      FESListPage.extract(response.body, page).map(_.url).drop(offset).take(size).map(url => Await.result(getEvent(url), 10 seconds)).flatten
    }
  }

  def getEvent(url: String): Future[Option[FESEvent]] = {
    WS.url(url).get().map { response =>
      Some(FESDetailsPage.extract(fixEncodage(response.body), url))
    }.recover {
      case e => {
        play.Logger.info("error for url: " + url, e)
        None
      }
    }
  }

  private def fixEncodage(str: String): String = new String(str.getBytes("iso-8859-1"), "utf8")
}