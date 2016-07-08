package conferences.controllers

import common.models.values.typed.{Email, TextHTML}
import common.repositories.conference.ConferenceRepository
import common.{Utils, Defaults}
import common.services._
import conferences.models.Conference
import conferences.services.{NewsService, NewsletterService}
import org.joda.time.{DateTimeConstants, DateTime}
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.mvc.{Controller, Action}
import play.api.Play.current
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Batch extends Controller {
  // this endpoint is used to wake up the app
  def ping = Action { implicit req =>
    emailNotif("ping")
    Ok
  }

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

  def sendNewsletter = Action.async { implicit req =>
    emailNotif("sendNewsletter")
    if(Utils.isProd()){
      NewsletterService.sendNewsletter().map { success =>
        Ok
      }
    } else {
      Future(Forbidden)
    }
  }

  def testNews(date: Option[String]) = Action.async { implicit req =>
    NewsService.getTwitts(date.map(d => DateTime.parse(d, Defaults.dateFormatter)).getOrElse(new DateTime())).map { twitts =>
      Ok(Json.obj("twitts" -> twitts))
    }
  }

  def publishNews = Action.async { implicit req =>
    emailNotif("publishNews")
    if(Utils.isProd()){
      NewsService.sendTwitts().map { success =>
        Ok
      }
    } else {
      Future(Forbidden)
    }
  }

  def testScheduler = Action.async { implicit req =>
    if(Utils.isProd()){
      emailNotif("testScheduler").map { success =>
        if(success) Ok else InternalServerError
      }
    } else {
      Future(Forbidden)
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

  private def emailNotif(endpoint: String): Future[Boolean] = {
    val now = new DateTime()
    val content = TextHTML(s"$endpoint endpoint is called at "+now+" (getDayOfWeek: "+now.getDayOfWeek+", getHourOfDay: "+now.getHourOfDay+")")
    EmailSrv.sendEmail(EmailData("Temporize Scheduler", Defaults.adminEmail, Email("loicknuchel@gmail.com"), s"[${Utils.getEnv()}] $endpoint", content, content.toPlainText))
  }
}
