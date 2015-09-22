package tools.utils

import common.models.event.GenericEvent
import common.models.event.GenericEventOrganizer
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.Writes
import play.api.mvc._
import java.util.Locale
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

trait Scraper[T] extends Controller {
  val baseUrl: String
  def toCsv(value: T): Map[String, String]
  def toCsv(value: GenericEvent): Map[String, String] = CsvUtils.jsonToCsv(Json.toJson(value), 4)
  def toGenericEvent(value: T): List[GenericEvent]
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
          case "csv" => Ok(CsvUtils.makeCsv(List(toCsv(value)))).withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"scraper_export.csv\"")).as("text/csv")
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
    withDetailsList(eventListUrl, offset, limit, sequentially) { (urls, elts, errors) =>
      val successResult = elts.map(_._2)
      val errorResult = errors.map { case (url, e) => Json.obj("error" -> e.getMessage(), "url" -> url) }
      format match {
        case "csv" => Future(CsvUtils.OkCsv(successResult.map(toCsv), "scraper_export.csv"))
        case _ => Future(Ok(Json.obj("results" -> successResult, "errors" -> errorResult, "nbElts" -> urls.length, "offset" -> offset, "limit" -> limit, "duration" -> (new DateTime().getMillis() - start.getMillis()) / 1000)))
      }
    }
  }

  def getGenericDetails(detailsUrl: String, format: String)(implicit writer: Writes[T]) = Action.async { implicit req =>
    fetchDetails(detailsUrl).map {
      _ match {
        case Success(value) => {
          val genericEvents = toGenericEvent(value)
          format match {
            case "csv" => CsvUtils.OkCsv(genericEvents.map(toCsv), "scraper_export.csv")
            case _ => Ok(Json.toJson(genericEvents))
          }
        }
        case Failure(e) => Ok(Json.obj("error" -> e.getMessage()))
      }
    }
  }

  def getGenericEventListDetails(eventListUrl: String, offset: Int, limit: Int, sequentially: Boolean = false, format: String) = Action.async { implicit req =>
    withDetailsList(eventListUrl, offset, limit, sequentially) { (urls, elts, errors) =>
      val genericEvents = elts.flatMap { case (url, elt) => toGenericEvent(elt) }
      format match {
        case "csv" => Future(CsvUtils.OkCsv(genericEvents.map(toCsv), "scraper_export.csv"))
        case _ => Future(Ok(Json.toJson(genericEvents)))
      }
    }
  }

  def getContactList(eventListUrl: String, offset: Int, limit: Int, sequentially: Boolean = false, format: String) = Action.async { implicit req =>
    val start = new DateTime()
    withDetailsList(eventListUrl, offset, limit, sequentially) { (urls, elts, errors) =>
      val genericEvents = elts.flatMap { case (url, elt) => toGenericEvent(elt) }
      val contacts = genericEvents
        .filter(e => e.info.end.map(d => isInNextMonths(d, 1, 10)).getOrElse(false)) // keep only upcoming events
        .flatMap(e => List(toMap(e)) ++ e.info.organizers.map(o => toMap(e, o))) // expand events to all contacts
        .groupBy(_.get("email").getOrElse("")) // group contacts by emails
        .filter { case (email, events) => !email.isEmpty } // remove empty email
        .map { case (email, events) => events.sortWith(dateSort).head } // keep only the first event (by date) for each email
        .toList.sortWith(dateSort) // sort contacts by event date
      format match {
        case "csv" => Future(CsvUtils.OkCsv(contacts, "scraper_export.csv"))
        case _ => Future(Ok(Json.obj("contacts" -> contacts, "nbElts" -> urls.length, "offset" -> offset, "limit" -> limit, "duration" -> (new DateTime().getMillis() - start.getMillis()) / 1000)))
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

  def withDetailsList(eventListUrl: String, offset: Int, limit: Int, sequentially: Boolean)(block: (List[String], List[(String, T)], List[(String, Throwable)]) => Future[Result]): Future[Result] = {
    fetchLinkList(eventListUrl).flatMap {
      _ match {
        case Success(urls) => fetchDetailsList(urls.drop(offset).take(limit), sequentially).flatMap { results =>
          val elts = results.collect { case (url, Success(elt)) => (url, elt) }
          val errors = results.collect { case (url, Failure(e)) => (url, e) }
          block(urls, elts, errors)
        }
        case Failure(e) => Future(Ok(Json.obj("error" -> e.getMessage())))
      }
    }
  }

  /*
   * Utils methods
   */

  private def isInNextMonths(date: DateTime, start: Int, end: Int): Boolean = {
    val now = new DateTime()
    return date.isAfter(now.plusMonths(start)) && date.isBefore(now.plusMonths(end))
  }
  private def toMap(e: GenericEvent, o: GenericEventOrganizer): Map[String, String] = Map(
    "eventUrl" -> e.sources.headOption.map(_.url).getOrElse(""),
    "eventDate" -> formatDate(e.info.start),
    "eventName" -> e.name,
    "contactName" -> o.name,
    "contactSite" -> o.website.getOrElse(""),
    "email" -> o.email.getOrElse(""),
    "contactPhone" -> formatPhone(o.phone.getOrElse("")))
  private def toMap(e: GenericEvent): Map[String, String] = Map(
    "eventUrl" -> e.sources.headOption.map(_.url).getOrElse(""),
    "eventDate" -> formatDate(e.info.start),
    "eventName" -> e.name,
    "contactName" -> "",
    "contactSite" -> e.info.website.getOrElse(""),
    "email" -> e.info.email.getOrElse(""),
    "contactPhone" -> formatPhone(e.info.phone.getOrElse("")))
  private def dateSort(e1: Map[String, String], e2: Map[String, String]): Boolean = {
    val d1 = e1.get("eventDate").get
    val d2 = e2.get("eventDate").get
    if (d1 == d2) {
      e1.get("eventName").get < e2.get("eventName").get
    } else {
      parseDate(d1).isBefore(parseDate(d2))
    }
  }
  private val dateFormat = DateTimeFormat.forPattern("dd/MM/yyyy").withLocale(Locale.FRENCH)
  private def parseDate(d: String): DateTime = DateTime.parse(d, dateFormat)
  private def formatDate(d: Option[DateTime]): String = d.map(_.toString(dateFormat)).getOrElse("")
  private def formatPhone(str: String): String = str.replace("+33 (0)", "0").replace("-", " ").replace(".", " ")

}
