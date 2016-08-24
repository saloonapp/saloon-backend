package common.services

import java.util.Date
import com.danielasfregola.twitter4s.TwitterClient
import com.danielasfregola.twitter4s.entities._
import com.danielasfregola.twitter4s.entities.enums.{ResultType, Mode}
import common.Utils
import org.joda.time.DateTime
import play.api.libs.json.Json
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

// see https://github.com/DanielaSfregola/twitter4s
object TwitterSrv {
  val account = play.api.Play.current.configuration.getString("twitter.account").getOrElse("Account Not Found !")
  val client = new TwitterClient()

  private def allowEdit(): Boolean = Utils.isProd()
  private def twitt(text: String, in_reply_to: Option[String] = None): Future[Option[Tweet]] = {
    if(allowEdit()) {
      Try(client.tweet(status = text, in_reply_to_status_id = in_reply_to.map(_.toLong))) match {
        case Success(f) => f.map(Some(_))
        case Failure(e) => play.Logger.warn(s"TwitterSrv - fail to post the twitt <$text> : ${e.getMessage}", e); Future(None)
      }
    } else {
      play.Logger.warn(s"TwitterSrv - you should be in Prod environment to send a twitt ! <$text>")
      Future(None)
    }
  }
  def sendTwitt(text: String): Future[Option[Tweet]] =
    twitt(text)
  def reply(text: String, inReplyTo: String): Future[Option[Tweet]] =
    twitt(text, Some(inReplyTo))
  def favorite(tweetId: String): Future[Option[Tweet]] = {
    if(allowEdit()) {
      client.favoriteStatus(tweetId.toLong, false).map(tweet => Some(tweet))
    } else {
      Future(None)
    }
  }

  //client.followUser()
  //client.followUserId()
  def getUserById(userId: String): Future[User] = {
    client.getUserById(userId.toLong)
  }
  def getUserByName(userScreenName: String): Future[User] = {
    client.getUser(userScreenName)
  }
  def listName(str: String): String = str.replace(".", "").take(25).replace(" ", "-")
  def getList(userScreenName: String, listName: String): Future[Option[TwitterList]] = {
    client.getListsForUser(userScreenName).map { lists =>
      lists
        .filter(_.name == listName)
        .sortBy(-_.member_count)
        .headOption
    }
  }
  def createList(name: String, description: Option[String] = None): Future[Option[TwitterList]] = {
    if(allowEdit()) {
      client.createList(name, Mode.Public, description).map(list => Some(list))
    } else {
      Future(None)
    }
  }
  def getOrCreateList(userScreenName: String, listName: String): Future[Option[TwitterList]] = {
    getList(userScreenName, listName).flatMap { listOpt =>
      listOpt.map(l => Future(Some(l))).getOrElse(createList(listName))
    }.recoverWith {
      case e: Throwable => createList(listName)
    }
  }
  def addToList(listId: String, userId: String): Future[Unit] = {
    if(allowEdit()) {
      client.addListMemberIdByListId(listId.toLong, userId.toLong).recover {
        case _: Throwable => ()
      }
    } else {
      Future()
    }
  }

  def search(query: String, limit: Int = 100): Future[List[Tweet]] = {
    client.searchTweet(
      query = query,
      count = limit,
      include_entities = true,
      result_type = ResultType.Recent).map { result =>
      result.statuses
    }.recover {
      case _: Throwable => List()
    }
  }
  def timeline(user: String): Future[List[Tweet]] = {
    client.getUserTimelineForUser(
      screen_name = user,
      count = 100).map(_.toList)
  }
}

case class TwitterCard(
  format: String,
  owner: String,
  title: String,
  description: String,
  imageUrl: String)


case class SimpleUser(
  id: String,
  name: String,
  screen_name: String,
  description: Option[String],
  email: Option[String],
  lang: Option[String])
object SimpleUser {
  implicit val format = Json.format[SimpleUser]
  def from(user: User) = SimpleUser(
    id = user.id_str,
    name = user.name,
    screen_name = user.screen_name,
    description = user.description,
    email = user.email,
    lang = Some(user.lang))
  def from(mention: UserMention) = SimpleUser(
    id = mention.id_str,
    name = mention.name,
    screen_name = mention.screen_name,
    description = None,
    email = None,
    lang = None)
}
case class SimpleTweet(
  id: String,
  text: String,
  entities: Option[Entities],
  user: Option[SimpleUser],
  created_at: Date) {
  def isRetweet(): Boolean = text.startsWith("RT ")
}
object SimpleTweet {
  implicit val formatSize = Json.format[Size]
  implicit val formatUserMention = Json.format[UserMention]
  implicit val formatUrlDetails = Json.format[UrlDetails]
  implicit val formatUrls = Json.format[Urls]
  implicit val formatMedia = Json.format[Media]
  implicit val formatHashTag = Json.format[HashTag]
  implicit val formatEntities = Json.format[Entities]
  implicit val format = Json.format[SimpleTweet]
  def from(tweet: Tweet) = SimpleTweet(
    id = tweet.id_str,
    text = tweet.text,
    entities = tweet.entities,
    user = tweet.user.map(SimpleUser.from),
    created_at = tweet.created_at)
}
case class SimpleList(
  id: String,
  slug: String,
  name: String,
  full_name: String,
  description: String,
  uri: String,
  mode: String,
  created_at: Date,
  subscriber_count: Int,
  member_count: Int,
  user: SimpleUser)
object SimpleList {
  implicit val format = Json.format[SimpleList]
  def from(list: TwitterList) = SimpleList(
    id = list.id_str,
    slug = list.slug,
    name = list.name,
    full_name = list.full_name,
    description = list.description,
    uri = list.uri,
    mode = list.mode,
    created_at = list.created_at,
    subscriber_count = list.subscriber_count,
    member_count = list.member_count,
    user = SimpleUser.from(list.user))

}
