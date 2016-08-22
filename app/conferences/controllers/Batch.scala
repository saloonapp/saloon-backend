package conferences.controllers

import common.repositories.conference.ConferenceRepository
import common.{Utils, Defaults}
import common.services._
import conferences.models.Conference
import conferences.services.{TimeChecker, SocialService, NewsletterService}
import org.joda.time.{DateTimeConstants, DateTime}
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.mvc.{Controller, Action}
import play.api.Play.current
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Batch extends Controller {
  def testSendNewsletter(emailOpt: Option[String], date: Option[String]) = Action.async { implicit req =>
    NewsletterService.getNewsletterInfos(date.map(d => DateTime.parse(d, Defaults.dateFormatter)).getOrElse(new DateTime())).flatMap { case (closingCFPs, incomingConferences, newData) =>
      emailOpt.map { email =>
        MailChimpSrv.createAndTestCampaign(MailChimpCampaign.conferenceListNewsletterTest(closingCFPs, incomingConferences, newData), List(email)).map { url =>
          Ok(Json.obj(
            "newsletterUrl" -> url,
            "closingCFPs" -> closingCFPs,
            "incomingConferences" -> incomingConferences,
            "newData" -> newData.map{case (c, d) => Json.obj("conference" -> c, "data" -> d)}
          ))
        }
      }.getOrElse {
        Future(Ok(MailChimpCampaign.conferenceListNewsletterTest(closingCFPs, incomingConferences, newData).contentHtml.unwrap).as(HTML))
      }
    }
  }

  def testSendDailyTwitts(date: Option[String]) = Action.async { implicit req =>
    SocialService.getDailyTwitts(date.map(d => DateTime.parse(d, Defaults.dateFormatter)).getOrElse(new DateTime())).map { twitts =>
      Ok(Json.obj("twitts" -> twitts))
    }
  }

  def testScanTwitterTimeline(date: Option[String]) = Action.async { implicit req =>
    ConferenceRepository.findRunning(date.map(d => DateTime.parse(d, Defaults.dateFormatter)).getOrElse(new DateTime())).flatMap { conferences =>
      Future.sequence(conferences.map { conference =>
        SocialService.getConferenceTwitts(conference).map { twitts =>
          (conference, twitts, SocialService.getUsers(twitts), SocialService.getLinks(twitts))
        }
      }).map { conferencesWithTwitts: List[(Conference, List[SimpleTweet], List[SimpleUser], List[(String, SimpleTweet)])] =>
        val addToLists = conferencesWithTwitts.map { case (conf, _, users, _) => (TwitterSrv.listName(conf.name), users) }.toMap[String, List[SimpleUser]]
        val links = conferencesWithTwitts.map { case (conf, _, _, links) => (conf.name, links.toMap[String, SimpleTweet]) }.toMap[String, Map[String, SimpleTweet]]
        Ok(Json.obj(
          "addToLists" -> addToLists,
          "links" -> links,
          "twitts" -> conferencesWithTwitts.map { case (conf, tweets, _, _) => (conf.name, tweets) }.toMap[String, List[SimpleTweet]],
          "conferences" -> conferences))
      }
    }
  }

  def scheduler = Action { implicit req =>
    if(Utils.isProd()){
      // TODO add cache (last exec) to prevent multiple execution if called multiple times in the valid period
      TimeChecker("sendNewsletter").isTime("9:15").isWeekDay(DateTimeConstants.MONDAY).run(() => NewsletterService.sendNewsletter())
      TimeChecker("sendDailyTwitts").isTime("9:15").run(() => SocialService.sendDailyTwitts())
      TimeChecker("scanTwitterTimeline").isTime("8:15", "12:15", "16:15", "19:15").run(() => SocialService.scanTwitterTimeline())
      Ok("Ok")
    } else {
      Forbidden("Forbidden")
    }
  }

  def importFromProd = Action.async { implicit req =>
    if(Utils.isProd()){ throw new IllegalStateException("You can't import data in prod !!!") }
    WS.url("http://saloonapp.herokuapp.com/api/conferences").get().flatMap { response =>
      val conferences = (response.json \ "result").as[List[Conference]]
      ConferenceRepository.importData(conferences).map { res =>
        Ok(Json.obj(
          "nbImported" -> res.n,
          "result" -> conferences
        ))
      }
    }
  }
}
