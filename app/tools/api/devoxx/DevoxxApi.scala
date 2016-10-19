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
import play.api.libs.json.{JsError, JsSuccess, JsArray, Json}

/*
 * This service can connect to Devoxx cfp and load events
 * As this backend is deployed over multiple instances (one by event), you must provide the base url.
 * Ex :
 * 	- http://cfp.bdx.io/api
 *  - http://cfp.codeursenseine.com/api
 */
object DevoxxApi extends Controller {

  /*
   * Play Controller
   */

  def getEventLinks(cfpUrl: String) = Action.async { implicit req =>
    val useCache = req.queryString.get("useCache").contains(Seq("true"))
    fetchEventLinks(cfpUrl, useCache).map {
      _ match {
        case Success(value) => Ok(Json.toJson(value))
        case Failure(e) => Ok(Json.obj("error" -> e.getMessage()))
      }
    }
  }

  def getEvent(conferenceUrl: String) = Action.async { implicit req =>
    val useCache = req.queryString.get("useCache").contains(Seq("true"))
    fetchEvent(conferenceUrl, useCache).map {
      _ match {
        case Success(value) => Ok(Json.toJson(value))
        case Failure(e) => Ok(Json.obj("error" -> e.getMessage()))
      }
    }
  }

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

  def getEventFull(conferenceUrl: String) = Action.async { implicit req =>
    val useCache = req.queryString.get("useCache").contains(Seq("true"))
    for {
      eventTry <- fetchEvent(conferenceUrl, useCache)
      speakersTry <- fetchSpeakers(conferenceUrl, useCache)
      sessionsTry <- fetchSessions(conferenceUrl, useCache)
      otherSpeakers <- sessionsTry.map(sessions => fetchUnlistedSpeakers(sessions, speakersTry.getOrElse(List()), useCache)).getOrElse(Future(List()))
    } yield {
      val res = for {
        event <- eventTry
        speakers <- speakersTry
        sessions <- sessionsTry
      } yield DevoxxEvent.toGenericEvent(event, speakers ++ otherSpeakers, sessions)
      res match {
        case Success(value) => Ok(Json.toJson(value))
        case Failure(e) => Ok(Json.obj("error" -> e.getMessage()))
      }
    }
  }

  /*
   * Basic methods
   */

  // TODO replace Try by JsResult !
  def fetchEventLinks(cfpUrl: String, useCache: Boolean): Future[Try[List[String]]] = {
    ScraperUtils.fetchJson(DevoxxUrl.conferences(cfpUrl), useCache).map {
      _.flatMap { res => Try((res \ "links").as[List[Link]].map(_.href)) }
    }
  }

  // TODO replace Try by JsResult !
  def fetchEvent(conferenceUrl: String, useCache: Boolean): Future[Try[DevoxxEvent]] = {
    ScraperUtils.fetchJson(conferenceUrl, useCache).map {
      _.flatMap { res => Try(res.as[DevoxxEvent].copy(sourceUrl = Some(conferenceUrl))) }
    }
  }

  // TODO replace Try by JsResult !
  def fetchSpeakers(conferenceUrl: String, useCache: Boolean): Future[Try[List[DevoxxSpeaker]]] = {
    fetchSpeakerLinks(conferenceUrl, useCache).flatMap { speakerLinksTry =>
      speakerLinksTry.map { speakerLinks =>
        Future.sequence(speakerLinks.map { link => fetchSpeaker(link, useCache) }).map { _.collect { case Success(speaker) => speaker } }
      } match {
        case Success(futureList) => futureList.map(list => Success(list))
        case Failure(err) => Future(Failure(err))
      }
    }
  }

  // TODO replace Try by JsResult !
  def fetchSpeakerLinks(conferenceUrl: String, useCache: Boolean): Future[Try[List[String]]] = {
    ScraperUtils.fetchJson(DevoxxUrl.speakers(conferenceUrl), useCache).map {
      _.flatMap { res => Try((res \\ "links").map(_.as[List[Link]]).flatMap(identity).map(_.href).toList) }
    }
  }

  // TODO replace Try by JsResult !
  def fetchSpeaker(speakerUrl: String, useCache: Boolean): Future[Try[DevoxxSpeaker]] = {
    ScraperUtils.fetchJson(speakerUrl, useCache).map {
      _.flatMap { res => Try(res.as[DevoxxSpeaker].copy(sourceUrl = Some(speakerUrl))) }
    }
  }

  // TODO replace Try by JsResult !
  def fetchSessions(conferenceUrl: String, useCache: Boolean): Future[Try[List[DevoxxSession]]] = {
    fetchSchedules(conferenceUrl, useCache).flatMap { scheduleUrlsTry =>
      scheduleUrlsTry.map { schedules =>
        Future.sequence(schedules.map { scheduleUrl => fetchSchedule(scheduleUrl, useCache) }).map { _.collect { case Success(schedule) => schedule }.flatMap(identity) }
      } match {
        case Success(futureList) => futureList.map(list => Success(list))
        case Failure(err) => Future(Failure(err))
      }
    }
  }

  // TODO replace Try by JsResult !
  def fetchSchedules(conferenceUrl: String, useCache: Boolean): Future[Try[List[String]]] = {
    ScraperUtils.fetchJson(DevoxxUrl.schedules(conferenceUrl), useCache).map {
      _.flatMap { res => Try((res \ "links").as[List[Link]].map(_.href)) }
    }
  }

  def fetchSchedule(scheduleUrl: String, useCache: Boolean): Future[Try[List[DevoxxSession]]] = {
    ScraperUtils.fetchJson(scheduleUrl, useCache).map {
      _.flatMap { res => Try((res \ "slots").as[List[DevoxxSession]].map { session => session.copy(sourceUrl = Some(scheduleUrl)) }) }
    }
  }

  // fetch speakers asigned to talks but not listed in speaker list...
  def fetchUnlistedSpeakers(sessions: List[DevoxxSession], speakers: List[DevoxxSpeaker], useCache: Boolean): Future[List[DevoxxSpeaker]] = {
    val sessionSpeakers = sessions.flatMap(_.talk).flatMap(_.speakers).map(_.link.href).distinct.flatMap(url => DevoxxUrl.speakerIdFromUrl(url).map((url, _)))
    val otherSpeakers = sessionSpeakers.filter { case (url, id) => speakers.exists(_.uuid == id) }
    Future.sequence(otherSpeakers.map { case (url, id) => fetchSpeaker(url, useCache) }).map { _.collect { case Success(speaker) => speaker } }
  }

}