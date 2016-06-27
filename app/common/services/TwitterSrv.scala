package common.services

import com.danielasfregola.twitter4s.TwitterClient
import com.danielasfregola.twitter4s.entities._
import common.Utils
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

// see https://github.com/DanielaSfregola/twitter4s
object TwitterSrv {
  val client = new TwitterClient()

  def twitt(text: String): Future[Option[Tweet]] = {
    if(Utils.isProd()) {
      Try(client.tweet(text)) match {
        case Success(f) => f.map(Some(_))
        case Failure(e) => play.Logger.warn(s"Fail to post twitt <$text> : ${e.getMessage}", e); Future(None)
      }
    } else {
      play.Logger.warn(s"You should be in Prod environment to send a twitt ! ($text)")
      Future(None)
    }
  }
}

case class TwitterCard(
  format: String,
  owner: String,
  title: String,
  description: String,
  imageUrl: String)
