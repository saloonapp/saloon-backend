package tools.api.Devoxx

import tools.utils.WSUtils
import tools.api.Devoxx.models.Link
import tools.api.Devoxx.models.DevoxxEvent
import tools.api.Devoxx.models.DevoxxSpeaker
import tools.api.Devoxx.models.DevoxxSession
import tools.models.GenericEventFull
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.api.libs.json.Json

/*
 * This service can connect to Devoxx cfp and load events
 * As this backend is deployed over multiple instances (one by event), you must provide the base url.
 * Ex :
 * 	- http://cfp.bdx.io/api
 */
object DevoxxApi extends Controller {

  /*
   * Play Controller
   */

  def getEventLinks(cfpUrl: String) = Action.async {
    fetchEventLinks(cfpUrl).map { links =>
      Ok(Json.toJson(links))
    }
  }

  def getEvent(conferenceUrl: String) = Action.async {
    fetchEvent(conferenceUrl).map { event =>
      Ok(Json.toJson(event))
    }
  }

  def getSpeakers(conferenceUrl: String) = Action.async {
    fetchSpeakers(conferenceUrl).map { speakers =>
      Ok(Json.toJson(speakers))
    }
  }

  def getSessions(conferenceUrl: String) = Action.async {
    fetchSessions(conferenceUrl).map { sessions =>
      Ok(Json.toJson(sessions))
    }
  }

  def getEventFull(conferenceUrl: String) = Action.async {
    for {
      event <- fetchEvent(conferenceUrl)
      speakers <- fetchSpeakers(conferenceUrl)
      sessions <- fetchSessions(conferenceUrl)
    } yield {
      Ok(Json.toJson(GenericEventFull.build(event, speakers, sessions)))
    }
  }

  /*
   * Basic methods
   */

  def fetchEventLinks(cfpUrl: String): Future[List[String]] = {
    WSUtils.fetch(DevoxxUrl.conferences(cfpUrl)).map { res =>
      (res.json \ "links").as[List[Link]].map(_.href)
    }
  }

  def fetchEvent(conferenceUrl: String): Future[DevoxxEvent] = {
    WSUtils.fetch(conferenceUrl).map { res =>
      res.json.as[DevoxxEvent].copy(sourceUrl = Some(conferenceUrl))
    }
  }

  def fetchSpeakers(conferenceUrl: String): Future[List[DevoxxSpeaker]] = {
    fetchSpeakerLinks(conferenceUrl).flatMap { speakerLinks =>
      Future.sequence(speakerLinks.map { link => fetchSpeaker(link) })
    }
  }

  def fetchSpeakerLinks(conferenceUrl: String): Future[List[String]] = {
    WSUtils.fetch(DevoxxUrl.speakers(conferenceUrl)).map { res =>
      (res.json \\ "links").map(_.as[List[Link]]).flatMap(identity).map(_.href).toList
    }
  }

  def fetchSpeaker(speakerUrl: String): Future[DevoxxSpeaker] = {
    WSUtils.fetch(speakerUrl).map { res =>
      res.json.as[DevoxxSpeaker].copy(sourceUrl = Some(speakerUrl))
    }
  }

  def fetchSessions(conferenceUrl: String): Future[List[DevoxxSession]] = {
    fetchSchedules(conferenceUrl).flatMap { schedules =>
      Future.sequence(schedules.map { schedule => fetchSchedule(schedule) }).map { _.flatMap(identity) }
    }
  }

  def fetchSchedules(conferenceUrl: String): Future[List[String]] = {
    WSUtils.fetch(DevoxxUrl.schedules(conferenceUrl)).map { res =>
      (res.json \ "links").as[List[Link]].map(_.href)
    }
  }

  def fetchSchedule(scheduleUrl: String): Future[List[DevoxxSession]] = {
    WSUtils.fetch(scheduleUrl).map { res =>
      (res.json \ "slots").as[List[DevoxxSession]].map { session =>
        session.copy(sourceUrl = Some(scheduleUrl))
      }
    }
  }


}