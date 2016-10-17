package tools.api.devfesttoulouse

import play.api.libs.json.{Json, JsObject}
import play.api.mvc.{Action, Controller}
import tools.api.devfesttoulouse.models.{DevFestEvent, DevFestSchedule, DevFestSession, DevFestSpeaker}
import tools.utils.ScraperUtils

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

object DevFestApi extends Controller {
  val useCache = false
  // https://devfesttoulouse.fr/data/speakers.json
  // https://devfesttoulouse.fr/data/sessions.json
  // https://devfesttoulouse.fr/data/schedule.json
  // https://devfesttoulouse.fr/data/partners.json

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

  def getSchedules(conferenceUrl: String) = Action.async {
    fetchSchedules(conferenceUrl).map {
      _ match {
        case Success(value) => Ok(Json.toJson(value))
        case Failure(e) => Ok(Json.obj("error" -> e.getMessage()))
      }
    }
  }

  def getEvent(conferenceUrl: String) = Action.async {
    for {
      speakersTry <- fetchSpeakers(conferenceUrl)
      sessionsTry <- fetchSessions(conferenceUrl)
      schedulesTry <- fetchSchedules(conferenceUrl)
    } yield {
      val res = for {
        speakers <- speakersTry
        sessions <- sessionsTry
        schedules <- schedulesTry
      } yield  DevFestEvent.toGenericEvent(conferenceUrl, speakers, sessions, schedules)
      res match {
        case Success(value) => Ok(Json.toJson(value))
        case Failure(e) => Ok(Json.obj("error" -> e.getMessage()))
      }
    }
  }

  /* basic methods */

  def fetchSpeakers(conferenceUrl: String): Future[Try[List[DevFestSpeaker]]] = {
    val speakersUrl = DevFestUrl.speakers(conferenceUrl)
    ScraperUtils.fetchJson(speakersUrl, useCache).map { res =>
      res.map { json => json.as[JsObject].values.flatMap(_.asOpt[DevFestSpeaker]).map(_.copy(sourceUrl = Some(speakersUrl))).toList }
    }
  }

  def fetchSessions(conferenceUrl: String): Future[Try[List[DevFestSession]]] = {
    val sessionsUrl = DevFestUrl.sessions(conferenceUrl)
    ScraperUtils.fetchJson(sessionsUrl, useCache).map { res =>
      res.map { json => json.as[JsObject].values.flatMap(_.asOpt[DevFestSession]).map(_.copy(sourceUrl = Some(sessionsUrl))).toList }
    }
  }

  def fetchSchedules(conferenceUrl: String): Future[Try[List[DevFestSchedule]]] = {
    val schedulesUrl = DevFestUrl.schedules(conferenceUrl)
    ScraperUtils.fetchJson(schedulesUrl, useCache).map { res =>
      res.map { json => json.as[List[DevFestSchedule]].map(_.copy(sourceUrl = Some(schedulesUrl))) }
    }
  }
}
