package tools.utils

import scala.util.Try
import scala.util.Failure
import scala.util.Success
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.JsString
import play.api.mvc._

trait ScraperTest[T <: CsvElt] extends Controller {
  val scraper: Scraper[T]
  val tests: Map[String, T]

  /*
   * Play Controller
   */

  def test()(implicit format: Format[T]) = Action.async { implicit req =>
    Future.sequence(tests.map { case (url, expected) => scraper.fetchDetails(url).map { result => (url, expected, result) } }).map { results =>
      val res = results.map {
        case (url, expected, result) =>
          val check = result match {
            case Success(value) => if (value == expected) { Json.obj("status" -> "Ok") } else { Json.obj("status" -> "KO", "expected" -> expected, "result" -> value) }
            case Failure(e) => Json.obj("status" -> "KO", "error" -> e.getMessage())
            case _ => Json.obj("status" -> "unknown")
          }
          (url, check)
      }.toMap
      Ok(Json.toJson(res))
    }
  }
}