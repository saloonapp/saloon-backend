package common.services

import com.danielasfregola.twitter4s.TwitterClient
import com.danielasfregola.twitter4s.entities._
import common.Utils
import scala.concurrent.Future

// see https://github.com/DanielaSfregola/twitter4s
object TwitterSrv {
  val client = new TwitterClient()

  def twitt(text: String): Future[Tweet] = {
    if(!Utils.isProd()){ throw new IllegalStateException("You should be in Prod environment to send a twitt !") }
    client.tweet(text)
  }
}

case class TwitterCard(
  format: String,
  owner: String,
  title: String,
  description: String,
  imageUrl: String)
