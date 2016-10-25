package tools.scrapers.blendwebmix

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import tools.scrapers.ScraperUtils
import tools.scrapers.blendwebmix.models.{BlendWebMixEvent, BlendWebMixSpeaker, BlendWebMixSession}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object BlendWebMixScraper extends Controller {
  val baseUrl = "http://www.blendwebmix.com"
  val sessionsUrl = baseUrl+"/programme.html"
  val speakersUrl = baseUrl+"/speakers.html"

  def session(url: String) = Action.async {
    fetchSession(url, "name").map { session =>
      Ok(Json.obj("session" -> session))
    }
  }

  def sessions = Action.async {
    fetchSessions(sessionsUrl).map { sessions =>
      Ok(Json.obj("sessions" -> sessions))
    }
  }

  def speaker(url: String) = Action.async {
    fetchSpeaker(url, None).map { speaker =>
      Ok(Json.obj("speaker" -> speaker))
    }
  }

  def speakers = Action.async {
    fetchSpeakers(speakersUrl).map { speakers =>
      Ok(Json.obj("speakers" -> speakers))
    }
  }

  def event = Action.async {
    for {
      sessions <- fetchSessions(sessionsUrl)
      speakers <- fetchSpeakers(speakersUrl)
    } yield ScraperUtils.format(BlendWebMixEvent.from(sessions, speakers))
  }

  /* private methods */

  private def fetchSession(sessionUrl: String, name: String): Future[BlendWebMixSession] =
    ScraperUtils.parseHtml(sessionUrl) { case (html, _) => BlendWebMixSession.extract(sessionUrl, name, html) }
  private def fetchSessions(programmeUrl: String): Future[List[BlendWebMixSession]] =
    ScraperUtils.parseHtml(programmeUrl) { case (html, _) =>
      BlendWebMixSession.extractLinks(html)
    }.flatMap { links =>
      ScraperUtils.sequence[(String, String), BlendWebMixSession](links, { case (sessionUrl, name) =>
        fetchSession(sessionUrl, name)
      })
    }
  private def fetchSpeaker(speakerUrl: String, job: Option[String]): Future[BlendWebMixSpeaker] =
    ScraperUtils.parseHtml(speakerUrl) { case (html, _) => BlendWebMixSpeaker.extract(speakerUrl, job, html) }
  private def fetchSpeakers(speakersUrl: String): Future[List[BlendWebMixSpeaker]] =
    ScraperUtils.parseHtml(speakersUrl) { case (html, _) =>
      BlendWebMixSpeaker.extractLinks(html)
    }.flatMap { links =>
      ScraperUtils.sequence[(String, String), BlendWebMixSpeaker](links, { case (speakerUrl, job) =>
        fetchSpeaker(speakerUrl, Some(job))
      })
    }
}
