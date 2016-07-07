package conferences.controllers

import common.repositories.conference.ConferenceRepository
import common.{Utils, Defaults}
import common.services.{TwittScheduler, MailChimpCampaign, MailChimpSrv, NewsletterScheduler}
import conferences.models.Conference
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.mvc.{Controller, Action}
import play.api.Play.current
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Batch extends Controller {
  def testNewsletter(emailOpt: Option[String], date: Option[String]) = Action.async { implicit req =>
    NewsletterScheduler.getNewsletterInfos(date.map(d => DateTime.parse(d, Defaults.dateFormatter)).getOrElse(new DateTime())).flatMap { case (closingCFPs, incomingConferences, newData) =>
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

  def testTwitts(date: Option[String]) = Action.async { implicit req =>
    TwittScheduler.getTwitts(date.map(d => DateTime.parse(d, Defaults.dateFormatter)).getOrElse(new DateTime())).map { twitts =>
      Ok(Json.obj("twitts" -> twitts))
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
