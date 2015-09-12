package tools.utils

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws._
import play.api.Play.current

object WSUtils {
  def fetch(url: String): Future[WSResponse] = WS.url(url).get()
}