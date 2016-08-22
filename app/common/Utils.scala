package common

import java.util.concurrent.TimeUnit
import akka.actor.Scheduler
import akka.pattern.after
import play.api.libs.ws.WS
import play.api.Play.current
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{Duration, FiniteDuration}
import play.api.Play
import play.api.mvc.Request
import play.api.mvc.AnyContent
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.jsoup.Jsoup

object Utils {
  // possible values for env : 'local', 'dev', 'prod', 'undefined'
  def getEnv(): String = Play.current.configuration.getString("application.env").getOrElse("undefined")
  def isProd(): Boolean = "prod".equals(getEnv())

  def retry[T](retries: Int, delay: FiniteDuration)(f: => Future[T])(implicit ec: ExecutionContext, s: Scheduler): Future[T] =
    f recoverWith { case _ if retries > 0 => after(delay, s)(retry(retries - 1, delay)(f)(ec, s))(ec) }
  def retryOnce[T](f: => Future[T])(implicit ec: ExecutionContext, s: Scheduler): Future[T] =
    retry(1, Duration(1, TimeUnit.SECONDS))(f)

  def transform[A](o: Option[Future[A]])(implicit ec: ExecutionContext): Future[Option[A]] = o.map(f => f.map(Option(_))).getOrElse(Future.successful(None))
  def toList(str: String): List[String] = str.split(",").toList.map(_.trim())
  def fromList(tags: List[String]): String = tags.mkString(", ")
  def toTwitterHashtag(str: String): String = if (str.startsWith("#")) str.substring(1) else str
  def toTwitterAccount(str: String): String = if (str.startsWith("@")) str.substring(1) else str
  def parseDate(format: DateTimeFormatter)(date: String): Option[DateTime] = if (date.isEmpty()) None else Some(DateTime.parse(date, format))
  def toOpt(str: String): Option[String] = if (str == null || str == "") None else Some(str)

  def getFormParam(key: String)(implicit req: Request[AnyContent]): Option[String] = req.body.asFormUrlEncoded.flatMap { _.get(key) }.flatMap { _.headOption }
  def getFormMultiParam(key: String)(implicit req: Request[AnyContent]): Seq[String] = req.body.asFormUrlEncoded.flatMap { _.get(key) }.getOrElse { Seq() }

  def htmlToText(html: String): String = Jsoup.parse(html.replaceAll("\\n", "\\\\n")).text().replaceAll("\\\\n", "\n")
  /*
   * HTML to Markdown
   * 	- http://www.lowerelement.com/Geekery/XML/XHTML-to-Markdown.html
   *  	- http://remark.overzealous.com/manual/index.html
   * 	- https://github.com/foursquare/sites-to-markdown/blob/master/src/jon/Convert.java
   */

  def asyncFilter[A](list: List[A], predicate: A => Future[Boolean])(implicit ec: ExecutionContext): Future[List[A]] = {
    Future.sequence(list.map(item => predicate(item).map(result => (item, result)))).map { l =>
      l.filter(_._2).map(_._1)
    }
  }

  def resolveUrl(url: String)(implicit ec: ExecutionContext): Future[String] = {
    WS.url(url).withFollowRedirects(false).get().flatMap { response =>
      response.header("Location").map { redirect =>
        resolveUrl(redirect)
      }.getOrElse {
        Future(url)
      }
    }
  }
}
