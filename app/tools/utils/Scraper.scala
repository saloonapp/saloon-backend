package tools.utils

import scala.util.Try
import scala.util.Failure
import scala.util.Success
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.Writes
import play.api.libs.ws.WS
import play.api.Play.current
import play.api.mvc._

trait Scraper[T <: CsvElt] extends Controller {
  val baseUrl: String
  def extractLinkList(html: String, baseUrl: String): List[String]
  def extractLinkPages(html: String): List[String] = List()
  def extractDetails(html: String, baseUrl: String, pageUrl: String): T

  /*
   * Play Controller
   */

  def getDetails(detailsUrl: String, format: String)(implicit writer: Writes[T]) = Action.async { implicit req =>
    fetchDetails(detailsUrl).map {
      _ match {
        case Success(value) => format match {
          case "csv" => Ok(CsvUtils.makeCsv(List(value.toCsv))).withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"scraper_export.csv\"")).as("text/csv")
          case _ => Ok(Json.toJson(value))
        }
        case Failure(e) => Ok(Json.obj("error" -> e.getMessage()))
      }
    }
  }

  def getLinkList(eventListUrl: String, format: String) = Action.async { implicit req =>
    fetchLinkList(eventListUrl).map {
      _ match {
        case Success(urls) => format match {
          case "csv" => Ok(CsvUtils.makeCsv(urls.map(url => Map("url" -> url)))).withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"scraper_export.csv\"")).as("text/csv")
          case _ => Ok(Json.toJson(urls))
        }
        case Failure(e) => Ok(Json.obj("error" -> e.getMessage()))
      }
    }
  }

  def getEventListDetails(eventListUrl: String, offset: Int, limit: Int, format: String)(implicit writer: Writes[T]) = Action.async { implicit req =>
    fetchLinkList(eventListUrl).flatMap {
      _ match {
        case Success(urls) => Future.sequence(urls.drop(offset).take(limit).map { url => fetchDetails(url).map(r => (url, r)) }).map { list =>
          val (successList, errorList) = list.partition { case (url, elt) => elt.isSuccess }
          val successResult = successList.map { case (url, elt) => elt.toOption }.flatten
          val errorResult = errorList.map {
            case (url, elt) => elt match {
              case Failure(e) => Some(Json.obj("error" -> e.getMessage(), "url" -> url))
              case _ => None
            }
          }.flatten
          format match {
            case "csv" => Ok(CsvUtils.makeCsv(successResult.map(_.toCsv))).withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"scraper_export.csv\"")).as("text/csv")
            case _ => Ok(Json.obj("results" -> successResult, "errors" -> errorResult, "nbElts" -> urls.length, "offset" -> offset, "limit" -> limit))
          }
        }
        case Failure(e) => Future(Ok(Json.obj("error" -> e.getMessage())))
      }
    }
  }

  /*
   * Basic methods
   */

  def fetchLinkList(listUrl: String): Future[Try[List[String]]] = {
    WS.url(listUrl).get().flatMap { response =>
      val page1 = Try(extractLinkList(response.body, baseUrl))
      Future.sequence(extractLinkPages(response.body).map { otherPageUrl => fetchLinkListPage(otherPageUrl) }).map { otherPages =>
        Try(page1.get ++ otherPages.flatMap(_.get))
      }
    }.recover {
      case e => Failure(e)
    }
  }

  private def fetchLinkListPage(listUrl: String): Future[Try[List[String]]] = {
    WS.url(listUrl).get().map { response =>
      Try(extractLinkList(response.body, baseUrl))
    }.recover {
      case e => Failure(e)
    }
  }

  def fetchDetails(detailsUrl: String): Future[Try[T]] = {
    WS.url(detailsUrl).get().map { response =>
      Try(extractDetails(response.body, baseUrl, detailsUrl))
    }.recover {
      case e => Failure(e)
    }
  }

}
