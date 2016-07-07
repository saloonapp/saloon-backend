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
    val content = TextHTML("ping endpoint is called")
    EmailSrv.sendEmail(EmailData("Temporize Scheduler", Defaults.adminEmail, Email("loicknuchel@gmail.com"), "["+Utils.getEnv()+"] ping", content, content.toPlainText))
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
    val content = TextHTML("sendNewsletter endpoint is called")
    EmailSrv.sendEmail(EmailData("Temporize Scheduler", Defaults.adminEmail, Email("loicknuchel@gmail.com"), "["+Utils.getEnv()+"] sendNewsletter", content, content.toPlainText))
    val now = new DateTime()
    if(Utils.isProd() && DateTimeConstants.MONDAY == now.getDayOfWeek && 8 < now.getHourOfDay && now.getHourOfDay < 10){
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
    val content = TextHTML("publishNews endpoint is called")
    EmailSrv.sendEmail(EmailData("Temporize Scheduler", Defaults.adminEmail, Email("loicknuchel@gmail.com"), "["+Utils.getEnv()+"] publishNews", content, content.toPlainText))
    val now = new DateTime()
    if(Utils.isProd() && 8 < now.getHourOfDay && now.getHourOfDay < 10){
      NewsService.sendTwitts().map { success =>
        Ok
      }
    } else {
      Future(Forbidden)
    }
  }

  def testScheduler = Action.async { implicit req =>
    if(Utils.isProd()){
      val content = TextHTML("testScheduler endpoint is called")
      EmailSrv.sendEmail(EmailData("Temporize Scheduler", Defaults.adminEmail, Email("loicknuchel@gmail.com"), "["+Utils.getEnv()+"] testScheduler", content, content.toPlainText)).map { success =>
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
}
