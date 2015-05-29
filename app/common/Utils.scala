package common

import services.FileImporter
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play
import org.jsoup.Jsoup

object Utils {
  // possible values for env : 'local', 'dev', 'prod', 'undefined'
  def getEnv(): String = Play.current.configuration.getString("application.env").getOrElse("undefined")
  def isProd(): Boolean = "prod".equals(getEnv())

  def transform[A](o: Option[Future[A]]): Future[Option[A]] = o.map(f => f.map(Option(_))).getOrElse(Future.successful(None))
  def toList(str: String): List[String] = str.split(",").toList.map(_.trim())
  def fromList(tags: List[String]): String = tags.mkString(", ")
  def toTwitterHashtag(str: String): String = if (str.startsWith("#")) str.substring(1) else str
  def toTwitterAccount(str: String): String = if (str.startsWith("@")) str.substring(1) else str
  def parseDate(format: DateTimeFormatter)(date: String): Option[DateTime] = if (date.isEmpty()) None else Some(DateTime.parse(date, FileImporter.dateFormat))

  def htmlToText(html: String): String = Jsoup.parse(html.replaceAll("\\n", "\\\\n")).text().replaceAll("\\\\n", "\n")
  /*
   * HTML to Markdown
   * 	- http://www.lowerelement.com/Geekery/XML/XHTML-to-Markdown.html
   *  	- http://remark.overzealous.com/manual/index.html
   * 	- https://github.com/foursquare/sites-to-markdown/blob/master/src/jon/Convert.java
   */
}
