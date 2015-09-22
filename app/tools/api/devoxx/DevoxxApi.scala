package tools.api.devoxx

import tools.utils.ScraperUtils
import tools.api.devoxx.models.Link
import tools.api.devoxx.models.DevoxxEvent
import tools.api.devoxx.models.DevoxxSpeaker
import tools.api.devoxx.models.DevoxxSession
import scala.util.Try
import scala.util.Success
import scala.util.Failure
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
    fetchEventLinks(cfpUrl).map {
      _ match {
        case Success(value) => Ok(Json.toJson(value))
        case Failure(e) => Ok(Json.obj("error" -> e.getMessage()))
      }
    }
  }

  def getEvent(conferenceUrl: String) = Action.async {
    fetchEvent(conferenceUrl).map {
      _ match {
        case Success(value) => Ok(Json.toJson(value))
        case Failure(e) => Ok(Json.obj("error" -> e.getMessage()))
      }
    }
  }

  def getSpeakers(conferenceUrl: String) = Action.async {
    fetchSpeakers(conferenceUrl).map {
      _ match {
        case Success(value) => Ok(Json.toJson(value))
        case Failure(e) => Ok(Json.obj("error" -> e.getMessage()))
      }
    }
  }

  def getSessions(conferenceUrl: String) = Action.async {
    fetchSessions(conferenceUrl).map {
      _ match {
        case Success(value) => Ok(Json.toJson(value))
        case Failure(e) => Ok(Json.obj("error" -> e.getMessage()))
      }
    }
  }

  def getEventFull(conferenceUrl: String) = Action.async {
    for {
      eventTry <- fetchEvent(conferenceUrl)
      speakersTry <- fetchSpeakers(conferenceUrl)
      sessionsTry <- fetchSessions(conferenceUrl)
    } yield {
      val res = for {
        event <- eventTry
        speakers <- speakersTry
        sessions <- sessionsTry
      } yield DevoxxEvent.toGenericEvent(event, speakers, sessions)
      res match {
        case Success(value) => Ok(Json.toJson(value))
        case Failure(e) => Ok(Json.obj("error" -> e.getMessage()))
      }
    }
  }

  /*
   * Basic methods
   */

  def fetchEventLinks(cfpUrl: String): Future[Try[List[String]]] = {
    ScraperUtils.fetchJson(DevoxxUrl.conferences(cfpUrl)).map {
      _.flatMap { res => Try((res \ "links").as[List[Link]].map(_.href)) }
    }
  }

  def fetchEvent(conferenceUrl: String): Future[Try[DevoxxEvent]] = {
    ScraperUtils.fetchJson(conferenceUrl).map {
      _.flatMap { res => Try(res.as[DevoxxEvent].copy(sourceUrl = Some(conferenceUrl))) }
    }
  }

  def fetchSpeakers(conferenceUrl: String): Future[Try[List[DevoxxSpeaker]]] = {
    fetchSpeakerLinks(conferenceUrl).flatMap { speakerLinksTry =>
      speakerLinksTry.map { speakerLinks =>
        Future.sequence(speakerLinks.map { link => fetchSpeaker(link) }).map { _.collect { case Success(speaker) => speaker } }
      } match {
        case Success(futureList) => futureList.map(list => Success(list))
        case Failure(err) => Future(Failure(err))
      }
    }
  }

  def fetchSpeakerLinks(conferenceUrl: String): Future[Try[List[String]]] = {
    ScraperUtils.fetchJson(DevoxxUrl.speakers(conferenceUrl)).map {
      _.flatMap { res => Try((res \\ "links").map(_.as[List[Link]]).flatMap(identity).map(_.href).toList) }
    }
  }

  def fetchSpeaker(speakerUrl: String): Future[Try[DevoxxSpeaker]] = {
    ScraperUtils.fetchJson(speakerUrl).map {
      _.flatMap { res => Try(res.as[DevoxxSpeaker].copy(sourceUrl = Some(speakerUrl))) }
    }
  }

  def fetchSessions(conferenceUrl: String): Future[Try[List[DevoxxSession]]] = {
    fetchSchedules(conferenceUrl).flatMap { scheduleUrlsTry =>
      scheduleUrlsTry.map { schedules =>
        Future.sequence(schedules.map { scheduleUrl => fetchSchedule(scheduleUrl) }).map { _.collect { case Success(schedule) => schedule }.flatMap(identity) }
      } match {
        case Success(futureList) => futureList.map(list => Success(list))
        case Failure(err) => Future(Failure(err))
      }
    }
  }

  def fetchSchedules(conferenceUrl: String): Future[Try[List[String]]] = {
    ScraperUtils.fetchJson(DevoxxUrl.schedules(conferenceUrl)).map {
      _.flatMap { res => Try((res \ "links").as[List[Link]].map(_.href)) }
    }
  }

  def fetchSchedule(scheduleUrl: String): Future[Try[List[DevoxxSession]]] = {
    ScraperUtils.fetchJson(scheduleUrl).map {
      _.flatMap { res => Try((res \ "slots").as[List[DevoxxSession]].map { session => session.copy(sourceUrl = Some(scheduleUrl)) }) }
    }
  }

}