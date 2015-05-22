package common

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play

object Utils {
  // possible values for env : 'local', 'dev', 'prod', 'undefined'
  def getEnv(): String = Play.current.configuration.getString("application.env").getOrElse("undefined")
  def isProd(): Boolean = "prod".equals(getEnv())

  def transform[A](o: Option[Future[A]]): Future[Option[A]] = o.map(f => f.map(Option(_))).getOrElse(Future.successful(None))
}
