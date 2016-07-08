package common.services

import com.danielasfregola.twitter4s.TwitterClient
import com.danielasfregola.twitter4s.entities._
import common.Utils
import org.joda.time.DateTime
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

// see https://github.com/DanielaSfregola/twitter4s
object TwitterSrv {
  val client = new TwitterClient()

  def sendTwitt(text: String): Future[Option[Tweet]] = {
    if(Utils.isProd()) {
      Try(client.tweet(text)) match {
        case Success(f) => f.map(Some(_))
        case Failure(e) => play.Logger.warn(s"TwitterSrv - fail to post the twitt <$text> : ${e.getMessage}", e); Future(None)
      }
    } else {
      play.Logger.warn(s"TwitterSrv - you should be in Prod environment to send a twitt ! <$text>")
      Future(None)
    }
  }

  def sendTwitts(twitts: List[String], everyMin: Int = 0): List[DateTime] = {
    if(everyMin > 0){
      twitts.zipWithIndex.map { case (twitt, i) => SchedulerHelper.in(minutes = everyMin * i)(sendTwitt(twitt)) }
    } else {
      twitts.map(sendTwitt).map(_ => new DateTime())
    }
  }
}

case class TwitterCard(
  format: String,
  owner: String,
  title: String,
  description: String,
  imageUrl: String)
