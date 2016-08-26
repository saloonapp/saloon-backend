package conferences.controllers

import common.repositories.conference.ConferenceRepository
import common.Config
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
    val realDate = date.map(d => DateTime.parse(d, Config.Application.dateFormatter)).getOrElse(new DateTime())
    NewsletterService.getNewsletterInfos(realDate).flatMap { case (closingCFPs, incomingConferences, newData) =>
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
    val realDate = date.map(d => DateTime.parse(d, Config.Application.dateFormatter)).getOrElse(new DateTime())
    SocialService.getDailyTwitts(realDate).map { twittsToSend =>
      Ok(Json.obj("twittsToSend" -> twittsToSend))
    }
  }

  def testScanTwitterTimeline(date: Option[String]) = Action.async { implicit req =>
    val realDate = date.map(d => DateTime.parse(d, Config.Application.dateFormatter)).getOrElse(new DateTime())
    SocialService.getTwitterTimelineActions(realDate).map { case (twittsToSend: List[String], repliesToUsers: List[(String, SimpleTweet)], usersToAddInList: List[(String, SimpleUser)], twittsToFav: List[SimpleTweet]) =>
      Ok(Json.obj(
        "date" -> realDate,
        "twittsToSend" -> twittsToSend,
        "repliesToUsers" -> Json.toJson(repliesToUsers.toMap),
        "usersToAddInList" -> usersToAddInList.groupBy(_._1).map { case (list, users) => (list, users.map(_._2))},
        "twittsToFav" -> twittsToFav
      ))
    }
  }

  private var schedulerLastCall: Option[DateTime] = None
  def scheduler = Action { implicit req =>
    def isDuplicate(lastCall: Option[DateTime]): Boolean = lastCall.map(_.plusMinutes(2*TimeChecker.timeInterval).isAfterNow()).getOrElse(false)
    play.Logger.info("scheduler called")
    if(!Config.Application.isProd) {
      play.Logger.info("scheduler called in "+Config.Application.env+" env (not prod, no exec) !")
      Forbidden("403 Forbidden")
    } else if(isDuplicate(schedulerLastCall)) {
      play.Logger.info("scheduler call duplicate (ignored) !")
      TooManyRequest("429 TooManyRequest")
    } else {
      play.Logger.info("scheduler execution...")
      schedulerLastCall = Some(new DateTime())
      TimeChecker("sendNewsletter").isTime("9:15").isWeekDay(DateTimeConstants.MONDAY).run(() => NewsletterService.sendNewsletter())
      TimeChecker("sendDailyTwitts").isTime("9:15").run(() => SocialService.sendDailyTwitts())
      TimeChecker("scanTwitterTimeline").isTime("8:15", "12:15", "16:15", "19:15").run(() => SocialService.scanTwitterTimeline())
      Accepted("202 Accepted")
    }
  }

  def importFromProd = Action.async { implicit req =>
    if(Config.Application.isProd){ throw new IllegalStateException("You can't import data in prod !!!") }
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
