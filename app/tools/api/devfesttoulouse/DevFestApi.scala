package tools.api.devfesttoulouse

import play.api.libs.json.{Json, JsObject}
import play.api.mvc.{Action, Controller}
import tools.api.devfesttoulouse.models._
import tools.utils.ScraperUtils

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

object DevFestApi extends Controller {
  // https://devfesttoulouse.fr/data/speakers.json
  // https://devfesttoulouse.fr/data/sessions.json
  // https://devfesttoulouse.fr/data/schedule.json
  // https://devfesttoulouse.fr/data/partners.json

  def getSpeakers(conferenceUrl: String) = Action.async { implicit req =>
    val useCache = req.queryString.get("useCache").contains(Seq("true"))
    fetchSpeakers(conferenceUrl, useCache).map {
      _ match {
        case Success(value) => Ok(Json.toJson(value))
        case Failure(e) => Ok(Json.obj("error" -> e.getMessage()))
      }
    }
  }

  def getSessions(conferenceUrl: String) = Action.async { implicit req =>
    val useCache = req.queryString.get("useCache").contains(Seq("true"))
    fetchSessions(conferenceUrl, useCache).map {
      _ match {
        case Success(value) => Ok(Json.toJson(value))
        case Failure(e) => Ok(Json.obj("error" -> e.getMessage()))
      }
    }
  }

  def getExponents(conferenceUrl: String) = Action.async { implicit req =>
    val useCache = req.queryString.get("useCache").contains(Seq("true"))
    fetchExponents(conferenceUrl, useCache).map {
      _ match {
        case Success(value) => Ok(Json.toJson(value))
        case Failure(e) => Ok(Json.obj("error" -> e.getMessage()))
      }
    }
  }

  def getSchedules(conferenceUrl: String) = Action.async { implicit req =>
    val useCache = req.queryString.get("useCache").contains(Seq("true"))
    fetchSchedules(conferenceUrl, useCache).map {
      _ match {
        case Success(value) => Ok(Json.toJson(value))
        case Failure(e) => Ok(Json.obj("error" -> e.getMessage()))
      }
    }
  }

  def getEvent(conferenceUrl: String) = Action.async { implicit req =>
    val useCache = req.queryString.get("useCache").contains(Seq("true"))
    for {
      speakersTry <- fetchSpeakers(conferenceUrl, useCache)
      sessionsTry <- fetchSessions(conferenceUrl, useCache)
      exponentsTry <- fetchExponents(conferenceUrl, useCache)
      schedulesTry <- fetchSchedules(conferenceUrl, useCache)
    } yield {
      val res = for {
        speakers <- speakersTry
        sessions <- sessionsTry
        exponents <- exponentsTry
        schedules <- schedulesTry
      } yield  DevFestEvent.toGenericEvent(conferenceUrl, speakers, sessions, schedules, exponents)
      res match {
        case Success(value) => Ok(Json.toJson(value))
        case Failure(e) => Ok(Json.obj("error" -> e.getMessage()))
      }
    }
  }

  /* basic methods */

  def fetchSpeakers(conferenceUrl: String, useCache: Boolean): Future[Try[List[DevFestSpeaker]]] = {
    val speakersUrl = DevFestUrl.speakers(conferenceUrl)
    ScraperUtils.fetchJson(speakersUrl, useCache).map { res =>
      res.map { json => json.as[JsObject].values.flatMap(_.asOpt[DevFestSpeaker]).map(_.copy(sourceUrl = Some(speakersUrl))).toList }
    }
  }

  def fetchSessions(conferenceUrl: String, useCache: Boolean): Future[Try[List[DevFestSession]]] = {
    val sessionsUrl = DevFestUrl.sessions(conferenceUrl)
    ScraperUtils.fetchJson(sessionsUrl, useCache).map { res =>
      res.map { json => json.as[JsObject].values.flatMap(_.asOpt[DevFestSession]).map(_.copy(sourceUrl = Some(sessionsUrl))).toList }
    }
  }

  def fetchExponents(conferenceUrl: String, useCache: Boolean): Future[Try[List[PartnerLevel]]] = {
    val exponentsUrl = DevFestUrl.exponents(conferenceUrl)
    ScraperUtils.fetchJson(exponentsUrl, useCache).map { res =>
      res.map { json => json.as[List[PartnerLevel]].map(p => p.copy(logos = p.logos.map(l => l.copy(logoUrl = l.logoUrl.replace("..", conferenceUrl), sourceUrl = Some(exponentsUrl))))) }
    }
  }

  def fetchSchedules(conferenceUrl: String, useCache: Boolean): Future[Try[List[DevFestSchedule]]] = {
    val schedulesUrl = DevFestUrl.schedules(conferenceUrl)
    ScraperUtils.fetchJson(schedulesUrl, useCache).map { res =>
      res.map { json => json.as[List[DevFestSchedule]].map(_.copy(sourceUrl = Some(schedulesUrl))) }
    }
  }
}
