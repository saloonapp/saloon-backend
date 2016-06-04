package tools.scrapers

import play.api.libs.json.{JsValue, Json, Writes}
import play.api.libs.ws.{WSResponse, WS}
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.Play.current

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

// scrape = fetch -> parse -> format
object ScraperUtils {
  def fetchHtml(url: String): Future[String] =
    fetch(_.body)(url)

  def fetchJson(url: String): Future[JsValue] =
    fetch(_.json)(url)

  def parseHtml[T](url: String)(parser: (String, String) => T): Future[T] =
    parse(fetchHtml)(url)(parser)

  def parseJson[T](url: String)(parser: (JsValue, String) => T): Future[T] =
    parse(fetchJson)(url)(parser)

  def parseListHtml[T](url: String)(parseLinks: (String, String) => List[String])(parser: (String, String) => T): Future[List[T]] =
    parseList(fetchHtml)(url)(parseLinks)(parser)

  def parseListJson[T](url: String)(parseLinks: (JsValue, String) => List[String])(parser: (JsValue, String) => T): Future[List[T]] =
    parseList(fetchJson)(url)(parseLinks)(parser)

  def scrapeHtml[T](url: String)(parser: (String, String) => T)(implicit w: Writes[T]): Future[Result] =
    scrape(fetchHtml)(url)(parser)

  def scrapeJson[T](url: String)(parser: (JsValue, String) => T)(implicit w: Writes[T]): Future[Result] =
    scrape(fetchJson)(url)(parser)

  def scrapeListHtml[T](url: String)(parseLinks: (String, String) => List[String])(parser: (String, String) => T)(implicit w: Writes[T]): Future[Result] =
    parseListHtml(url)(parseLinks)(parser).map(results => formatList(results))

  def scrapeListJson[T](url: String)(parseLinks: (JsValue, String) => List[String])(parser: (JsValue, String) => T)(implicit w: Writes[T]): Future[Result] =
    parseListJson(url)(parseLinks)(parser).map(results => formatList(results))

  def format[T](result: T)(implicit w: Writes[T]): Result =
    Ok(Json.obj(
      "result" -> result
    )).withHeaders("Content-Type" -> "application/json; charset=utf-8")

  def formatList[T](results: List[T])(implicit w: Writes[T]): Result =
    Ok(Json.obj(
      "result" -> results
    )).withHeaders("Content-Type" -> "application/json; charset=utf-8")


  /*
    Private methods
   */

  private def fetch[U](extract: WSResponse => U)(url: String): Future[U] =
    WS.url(url).get().map { response => extract(response) }

  private def parse[T, U](fetch: String => Future[U])(url: String)(parser: (U, String) => T): Future[T] =
    fetch(url).map(page => parser(page, url))

  private def parseList[T, U](fetch: String => Future[U])(url: String)(parseLinks: (U, String) => List[String])(parser: (U, String) => T): Future[List[T]] =
    fetch(url).map(page => parseLinks(page, url)).flatMap { links =>
      Future.sequence(links.map(link => fetch(link).map(page => parser(page, link))))
    }

  private def scrape[T, U](fetch: String => Future[U])(url: String)(parser: (U, String) => T)(implicit w: Writes[T]): Future[Result] =
    fetch(url)
      .map { value => format(parser(value, url)) }
      .recover {
        case e: Exception =>
          NotFound(Json.obj(
            "message" -> s"Unable to connect to $url",
            "error" -> e.getMessage)
          ).withHeaders("Content-Type" -> "application/json; charset=utf-8")
      }
}
