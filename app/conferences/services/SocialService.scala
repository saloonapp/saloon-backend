package conferences.services

import common.Utils
import common.repositories.conference.ConferenceRepository
import common.services.UrlInfo.Category
import common.services.{UrlSrv, SimpleUser, SimpleTweet, TwitterSrv}
import conferences.models.Conference
import org.joda.time.DateTime
import play.api.libs.json.Json
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object SocialService {
  def sendDailyTwitts(): Future[Boolean] = {
    getDailyTwitts(new DateTime()).map { twitts =>
      play.Logger.info(if(twitts.length > 0) twitts.length+" twitts à envoyer :" else "aucun twitt à envoyer")
      twitts.map(t => play.Logger.info("  - "+t))
      TwitterSrv.sendTwitts(twitts, 10)
      true
    }
  }
  def getDailyTwitts(date: DateTime): Future[List[String]] = {
    val today = date.withTime(0, 0, 0, 0)
    val nearClosingCFPsFut = ConferenceRepository.find(Json.obj("$or" -> Json.arr(
      Json.obj("cfp.end" -> Json.obj("$eq" -> today.plusDays(1))), // cfp closes tomorrow
      Json.obj("cfp.end" -> Json.obj("$eq" -> today.plusDays(3))), // cfp closes in 3 days
      Json.obj("cfp.end" -> Json.obj("$eq" -> today.plusDays(7))), // cfp closes in 1 week
      Json.obj("cfp.end" -> Json.obj("$eq" -> today.plusDays(14))) // cfp closes in 2 weeks
    )))
    val nearStartingConfsFut = ConferenceRepository.find(Json.obj("$or" -> Json.arr(
      Json.obj("start" -> Json.obj("$eq" -> today)), // conf starts today
      Json.obj("start" -> Json.obj("$eq" -> today.plusDays(1))), // conf starts tomorrow
      Json.obj("start" -> Json.obj("$eq" -> today.plusDays(7))) // conf starts in 1 week
    )))
    for {
      nearClosingCFPs <- nearClosingCFPsFut
      nearStartingConfs <- nearStartingConfsFut
    } yield {
      val twitts = nearClosingCFPs.map { c =>
        (TwittFactory.closingCFP(c, today), c.cfp.get.end.getMillis)
      } ++ nearStartingConfs.map { c =>
        (TwittFactory.startingConference(c, today), c.start.getMillis)
      }
      twitts.sortBy(_._2).map(_._1)
    }
  }

  def scanTwitterTimeline(): Future[Boolean] = {
    ConferenceRepository.findRunning().flatMap { conferences =>
      Future.sequence(conferences.map { conference =>
        SocialService.getConferenceTwitts(conference).map { twitts =>
          (conference, twitts, SocialService.getUsers(twitts), SocialService.getLinks(twitts))
        }
      }).map { conferencesWithTwitts: List[(Conference, List[SimpleTweet], List[SimpleUser], List[(String, SimpleTweet)])] =>
        conferencesWithTwitts.map { case (conf, _, users, links) =>
          // global twitt to remind to add slides
          TwitterSrv.sendTwitt(TwittFactory.genericRemindToAddSlides(conf))

          // add twitting users to conference list
          TwitterSrv.getOrCreateList(TwitterSrv.account, TwitterSrv.listName(conf.name)).map { _.map { list =>
            users.map(user => TwitterSrv.addToList(list.id_str, user.id))
          }}

          // favorite twitts with links
          links.map { case (link, tweet) => TwitterSrv.favorite(tweet.id) }

          // reply to suggest adding slides to conference list
          Utils.asyncFilter[(String, SimpleTweet)](links, link => UrlSrv.getService(link._1).map { info =>
            info.category == Category.Slides
          }).map { slidesLinks =>
            slidesLinks.map { case (link, tweet) => TwitterSrv.reply(TwittFactory.replySuggestToAddSlides(conf, tweet), tweet.id) }
          }
        }
        true
      }
    }
  }
  def getConferenceTwitts(conference: Conference): Future[List[SimpleTweet]] = {
    val hashtagSearchFut = conference.twitterHashtag.map(hashtag => TwitterSrv.search("#"+hashtag)).getOrElse(Future(List()))
    val accountSearchFut = conference.twitterAccount.map(account => TwitterSrv.search(account)).getOrElse(Future(List()))
    val accountTimelineFut = conference.twitterAccount.map(account => TwitterSrv.timeline(account)).getOrElse(Future(List()))
    for {
      hashtagSearch <- hashtagSearchFut
      accountSearch <- accountSearchFut
      accountTimeline <- accountTimelineFut
    } yield {
      List(
        hashtagSearch,
        accountSearch,
        accountTimeline).flatten.map(SimpleTweet.from).groupBy(_.id).map(_._2.head).toList
    }
  }
  def getUsers(twitts: List[SimpleTweet]): List[SimpleUser] = {
    List(
      twitts.map(_.user).flatten,
      twitts.flatMap(_.entities.map(_.user_mentions)).flatten.map(SimpleUser.from)
    ).flatten.groupBy(_.id).map(_._2.head).toList
  }
  def getLinks(twitts: List[SimpleTweet]): List[(String, SimpleTweet)] = {
    twitts.filter(_.entities.isDefined).flatMap(t => t.entities.get.urls.map(url => (url.expanded_url, t)))
  }
}
