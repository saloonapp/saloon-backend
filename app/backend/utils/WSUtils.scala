package backend.utils

import scala.util.Try
import scala.util.Failure
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WS
import play.api.libs.ws.WSResponse
import play.api.Play.current

object WSUtils {
  def fetch(url: String): Future[Try[String]] = {
    WS.url(url).get().map { response =>
      Try(response.body)
    }.recover {
      // http://www.bimeanalytics.com/engineering-blog/retrying-http-request-in-scala/
      case e => Failure(e)
    }
  }
}