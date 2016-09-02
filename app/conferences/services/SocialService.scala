package conferences.services

import common.{Config, Utils}
import common.repositories.conference.ConferenceRepository
import common.services.UrlInfo.Category
import common.services._
import conferences.models.Conference
import org.joda.time.LocalDate
import play.api.libs.json.Json
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object SocialService {
  def sendDailyTwitts(): Future[Boolean] = {
    if(!Config.Application.isProd){ throw new IllegalAccessException("Method sendDailyTwitts should be called only on 'prod' environment !") }
    play.Logger.info("SocialService.sendDailyTwitts called")
    getDailyTwitts(new LocalDate()).map { twittsToSend =>
      play.Logger.info("SocialService.sendDailyTwitts: "+twittsToSend.length+" twittsToSend")
      twittsToSend.map(t => play.Logger.info("  - TWEET: "+t))
      SchedulerHelper.delay(twittsToSend, TwitterSrv.sendTwitt, 10)
      true
    }
  }
  def getDailyTwitts(today: LocalDate): Future[List[String]] = {
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
      val twittsWithCfpTimestamp = nearClosingCFPs.map { c =>
        (TwittFactory.closingCFP(c, today), c.cfp.get.end.toDateTimeAtStartOfDay.getMillis)
      } ++ nearStartingConfs.map { c =>
        (TwittFactory.startingConference(c, today), c.start.toDateTimeAtStartOfDay.getMillis)
      }
      twittsWithCfpTimestamp.sortBy(_._2).map(_._1)
    }
  }

  def scanTwitterTimeline(): Future[Boolean] = {
    if(!Config.Application.isProd){ throw new IllegalAccessException("Method scanTwitterTimeline should be called only on 'prod' environment !") }
    play.Logger.info("SocialService.scanTwitterTimeline called")
    getTwitterTimelineActions(new LocalDate()).map { case (twittsToSend: List[String], repliesToUsers: List[(String, SimpleTweet)], usersToAddInList: List[(String, SimpleUser)], twittsToFav: List[SimpleTweet]) =>
      play.Logger.info("SocialService.scanTwitterTimeline: "+twittsToSend.length+" twittsToSend, "+repliesToUsers.length+" repliesToUsers, "+usersToAddInList.length+" usersToAddInList, "+twittsToFav.length+" twittsToFav")
      twittsToSend.map(t => play.Logger.info("  - TWEET: "+t))
      repliesToUsers.map(r => play.Logger.info("  - REPLY: "+r._1))
      usersToAddInList.map(u => play.Logger.info("  - LIST: "+u._1+"/"+u._2.screen_name))
      twittsToFav.map(f => play.Logger.info("  - FAV: "+f.text))

      SchedulerHelper.delay(twittsToSend, TwitterSrv.sendTwitt, 10)
      repliesToUsers.map { case (tweet, inReply) => TwitterSrv.reply(tweet, inReply.id) }
      usersToAddInList.groupBy(_._1).map { case (listName, users) =>
        TwitterSrv.getOrCreateList(TwitterSrv.account, listName).map { _.map { list =>
          users.map(_._2).map(user => TwitterSrv.addToList(list.id_str, user.id))
        }}
      }
      twittsToFav.map(tweet => TwitterSrv.favorite(tweet.id))
      true
    }
  }
  def getTwitterTimelineActions(date: LocalDate): Future[(List[String], List[(String, SimpleTweet)], List[(String, SimpleUser)], List[SimpleTweet])] = {
    ConferenceRepository.findRunning(date).flatMap { conferences =>
      Future.sequence(conferences.map { conference =>
        SocialService.getConferenceTwitts(conference).map { twitts =>
          (conference, twitts, SocialService.getUsers(twitts), SocialService.getLinks(twitts))
        }
      }).flatMap { conferencesWithTwitts: List[(Conference, List[SimpleTweet], List[SimpleUser], List[(String, SimpleTweet)])] =>
        Future.sequence(conferencesWithTwitts.map { case (conf: Conference, _, users: List[SimpleUser], links: List[(String, SimpleTweet)]) =>
          val linksWithoutRT = links.filter(!_._2.isRetweet())
          Utils.asyncFilter[(String, SimpleTweet)](linksWithoutRT, link => UrlSrv.getService(link._1).map(_.category == Category.Slides)).map { slidesLinks =>
            val twittsToSend = List(TwittFactory.genericRemindToAddSlides(conf)) // global twitt to remind to add slides
            val repliesToUsers = slidesLinks.map { case (_, tweet) => (TwittFactory.replySuggestToAddSlides(conf, tweet), tweet) } // reply to suggest adding slides to conference list
            val usersToAddInList = users.map(user => (TwitterSrv.listName(conf.name), user)) // add twitting users to conference list
            val twittsToFav = links.map(_._2) // favorite twitts with links
            (twittsToSend, repliesToUsers, usersToAddInList, twittsToFav)
          }
        }).map { results =>
          (results.flatMap(_._1), results.flatMap(_._2), results.flatMap(_._3), results.flatMap(_._4))
        }
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
