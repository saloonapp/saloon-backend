package tools.scrapers.salonreunir

import tools.scrapers.salonreunir.models.SalonReunirSession
import tools.utils.ScraperUtils
import tools.utils.CsvUtils
import scala.collection.JavaConversions._
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.Play.current
import play.api.mvc._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

/*
 * List url:
 * 	- http://salon.reunir.com/conferences/
 */
object SalonReunirSessionScraper extends Controller {

  def getListDetails(listUrl: String, format: String) = Action.async { implicit req =>
    fetchListDetails(listUrl).map {
      _ match {
        case Success(elts) => format match {
          case "csv" => Ok(CsvUtils.makeCsv(elts.map(_.toCsv))).withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"scraper_export.csv\"")).as("text/csv")
          case _ => Ok(Json.obj("results" -> elts))
        }
        case Failure(e) => Ok(Json.obj("error" -> e.getMessage()))
      }
    }
  }

  def fetchListDetails(listUrl: String): Future[Try[List[SalonReunirSession]]] = {
    WS.url(listUrl).get().map { response =>
      Try(extractListDetails(response.body, listUrl))
    }.recover {
      case e => Failure(e)
    }
  }

  private def extractListDetails(html: String, pageUrl: String): List[SalonReunirSession] = {
    val doc = Jsoup.parse(fixEncodage(html))
    doc.select("table.conferences").toList.zipWithIndex.flatMap {
      case (conferences, listIndex) =>
        conferences.select("tr").toList.zipWithIndex.map {
          case (session, itemIndex) =>
            val cells = session.select("td")
            ScraperUtils.getSafe(cells, 0).flatMap { first =>
              val day = if ((listIndex % 2 == 0 && listIndex < 4) || (listIndex == 4 && itemIndex < 6)) { "17/09/2015" } else { "18/09/2015" }
              getDates(first.text(), day).map {
                case (start, end) =>
                  val (name, animator, format) = ScraperUtils.getSafe(cells, 1).map { cell =>
                    val format = cell.select(".category").text()
                    cell.select(".category").remove()
                    val lines = cell.html().split("<br>").toList
                    val name = lines.headOption.map(c => Jsoup.parse(c).text()).getOrElse("")
                    val animator = Jsoup.parse(lines.drop(1).mkString(" ")).text()
                    (name, animator, format)
                  }.getOrElse(("", "", ""))
                  val place = ScraperUtils.getSafe(cells, 3).map(_.text()).getOrElse("")
                  SalonReunirSession(name, parseDate(start), parseDate(end), name, animator, format, place, pageUrl)
              }
            }
        }.flatten
    }
  }

  private def fixEncodage(str: String): String = new String(str.getBytes("iso-8859-1"), "utf8")
  private def orElse(str: String, default: String): String = if (str == null) default else str
  private val dateRegex = "([0-9]+)[hH]([0-9]+)? [-à] ([0-9]+)[hH]([0-9]+)?".r.unanchored
  private def getDates(input: String, day: String): Option[(String, String)] = input match {
    case dateRegex(hStart, mStart, hEnd, mEnd) => Some((day + " " + hStart + ":" + orElse(mStart, "00"), day + " " + hEnd + ":" + orElse(mEnd, "00")))
    case _ => None
  }
  private def parseDate(date: String): DateTime = DateTime.parse(date, DateTimeFormat.forPattern("dd/MM/yyyy HH:mm"))
}