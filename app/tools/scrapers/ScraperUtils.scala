package tools.scrapers

import play.api.libs.json.{JsValue, Json, Writes}
import play.api.libs.ws.{WSResponse, WS}
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.Play.current

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object ScraperUtils {
  def fetchHtml(url: String): Future[String] =
    fetch(_.body)(url)

  def fetchJson(url: String): Future[JsValue] =
    fetch(_.json)(url)

  def scrapeHtml[T](url: String)(parser: String => T)(implicit w: Writes[T]): Future[Result] =
    scrape(fetchHtml)(url)(parser)

  def scrapeJson[T](url: String)(parser: JsValue => T)(implicit w: Writes[T]): Future[Result] =
    scrape(fetchJson)(url)(parser)

  def write[T](result: T)(implicit w: Writes[T]): Result =
    Ok(Json.obj(
      "result" -> result
    )).withHeaders("Content-Type" -> "application/json; charset=utf-8")


  private def fetch[U](extract: WSResponse => U)(url: String): Future[U] =
    WS.url(url).get().map { response => extract(response) }

  private def scrape[T, U](fetch: String => Future[U])(url: String)(parser: U => T)(implicit w: Writes[T]): Future[Result] =
    fetch(url)
      .map { value => write(parser(value)) }
      .recover {
        case e: Exception =>
          NotFound(Json.obj(
            "message" -> s"Unable to connect to $url",
            "error" -> e.getMessage)
          ).withHeaders("Content-Type" -> "application/json; charset=utf-8")
      }
}
