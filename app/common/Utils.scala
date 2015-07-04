package common

import common.services.FileImporter
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play
import play.api.mvc.Request
import play.api.mvc.AnyContent
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
  def parseDate(format: DateTimeFormatter)(date: String): Option[DateTime] = if (date.isEmpty()) None else Some(DateTime.parse(date, format))

  def getFormParam(key: String)(implicit req: Request[AnyContent]): Option[String] = req.body.asFormUrlEncoded.flatMap { _.get(key) }.flatMap { _.headOption }
  def getFormMultiParam(key: String)(implicit req: Request[AnyContent]): Seq[String] = req.body.asFormUrlEncoded.flatMap { _.get(key) }.getOrElse { Seq() }

  def htmlToText(html: String): String = Jsoup.parse(html.replaceAll("\\n", "\\\\n")).text().replaceAll("\\\\n", "\n")
  /*
   * HTML to Markdown
   * 	- http://www.lowerelement.com/Geekery/XML/XHTML-to-Markdown.html
   *  	- http://remark.overzealous.com/manual/index.html
   * 	- https://github.com/foursquare/sites-to-markdown/blob/master/src/jon/Convert.java
   */
}
