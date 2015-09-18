package tools.utils

import scala.util.Try
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.Writes
import play.api.mvc._
import org.joda.time.DateTime

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

  def getEventListDetails(eventListUrl: String, offset: Int, limit: Int, sequentially: Boolean = false, format: String)(implicit writer: Writes[T]) = Action.async { implicit req =>
    val start = new DateTime()
    fetchLinkList(eventListUrl).flatMap {
      _ match {
        case Success(urls) => fetchDetailsList(urls.drop(offset).take(limit), sequentially).map { list =>
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
            case _ => Ok(Json.obj("results" -> successResult, "errors" -> errorResult, "nbElts" -> urls.length, "offset" -> offset, "limit" -> limit, "duration" -> (new DateTime().getMillis() - start.getMillis()) / 1000))
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
    ScraperUtils.fetch(listUrl).flatMap { responseTry =>
      responseTry match {
        case Success(response) => {
          val page1 = Try(extractLinkList(response, baseUrl))
          Future.sequence(extractLinkPages(response).map { otherPageUrl => fetchLinkListPage(otherPageUrl) }).map { otherPages =>
            Try(page1.get ++ otherPages.flatMap(_.get))
          }
        }
        case Failure(e) => Future(Failure(e))
      }
    }
  }

  private def fetchLinkListPage(listUrl: String): Future[Try[List[String]]] = {
    ScraperUtils.fetch(listUrl).map { responseTry =>
      responseTry.flatMap { response => Try(extractLinkList(response, baseUrl)) }
    }
  }

  def fetchDetails(detailsUrl: String): Future[Try[T]] = {
    ScraperUtils.fetch(detailsUrl).map { responseTry =>
      responseTry.flatMap { response => Try(extractDetails(response, baseUrl, detailsUrl)) }
    }
  }

  def fetchDetailsList(detailsUrls: List[String], sequentially: Boolean = false): Future[List[(String, Try[T])]] = {
    if (sequentially) {
      fetchListSequentially(detailsUrls, List())
    } else {
      Future.sequence(detailsUrls.map(url => fetchDetails(url).map(r => (url, r))))
    }
  }

  private def fetchListSequentially(urls: List[String], results: List[(String, Try[T])]): Future[List[(String, Try[T])]] = {
    if (urls.length > 0) {
      fetchDetails(urls.head).flatMap { r =>
        fetchListSequentially(urls.tail, results ++ List((urls.head, r)))
      }
    } else {
      Future(results)
    }
  }

}
