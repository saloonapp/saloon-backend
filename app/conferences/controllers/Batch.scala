package conferences.controllers

import common.models.values.typed.{Email, TextHTML}
import common.repositories.conference.ConferenceRepository
import common.{Utils, Defaults}
import common.services._
import conferences.models.Conference
import conferences.services.{BatchDispatcher, SocialService, NewsletterService}
import org.joda.time.{DateTimeConstants, DateTime}
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.mvc.{Controller, Action}
import play.api.Play.current
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Batch extends Controller {
  def testNewsletter(emailOpt: Option[String], date: Option[String]) = Action.async { implicit req =>
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

  def testSocialAnouncements(date: Option[String]) = Action.async { implicit req =>
    SocialService.getTwitts(date.map(d => DateTime.parse(d, Defaults.dateFormatter)).getOrElse(new DateTime())).map { twitts =>
      Ok(Json.obj("twitts" -> twitts))
    }
  }

  // TODO will be replaced with scheduler method
  def sendNewsletter = Action.async { implicit req =>
    if(Utils.isProd()){
      NewsletterService.sendNewsletter().map { success =>
        Ok
      }
    } else {
      Future(Forbidden)
    }
  }

  // TODO will be replaced with scheduler method
  def publishNews = Action.async { implicit req =>
    if(Utils.isProd()){
      SocialService.sendTwitts().map { success =>
        Ok
      }
    } else {
      Future(Forbidden)
    }
  }

  def scheduler = Action { implicit req =>
    // TODO : call a unique endoit which will dispatch execs on required times...
    // BatchDispatcher.isTime("9:15").map(_ => SocialService.sendTwitts())
    //BatchDispatcher.isWeekDay("Monday").isTime("9:15").map(_ => if(Utils.isProd()){ NewsletterService.sendNewsletter() })
    Ok
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
