package common

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play
import org.jsoup.Jsoup

object Utils {
  // possible values for env : 'local', 'dev', 'prod', 'undefined'
  def getEnv(): String = Play.current.configuration.getString("application.env").getOrElse("undefined")
  def isProd(): Boolean = "prod".equals(getEnv())

  def transform[A](o: Option[Future[A]]): Future[Option[A]] = o.map(f => f.map(Option(_))).getOrElse(Future.successful(None))

  def htmlToText(html: String): String = Jsoup.parse(html.replaceAll("\\n", "\\\\n")).text().replaceAll("\\\\n", "\n")
  /*
   * HTML to Markdown
   * 	- http://www.lowerelement.com/Geekery/XML/XHTML-to-Markdown.html
   *  	- http://remark.overzealous.com/manual/index.html
   * 	- https://github.com/foursquare/sites-to-markdown/blob/master/src/jon/Convert.java
   */
}
