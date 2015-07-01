package tools.controllers

import tools.scrapers.voxxrin.VoxxrinApi
import infrastructure.repository.EventRepository
import infrastructure.repository.SessionRepository
import common.models._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.libs.json._

object Voxxrin extends Controller {

  def getEvents() = Action.async {
    VoxxrinApi.getEvents().map { res =>
      Ok(Json.toJson(res))
    }
  }

  def getEvent(eventId: String) = Action.async {
    VoxxrinApi.getEvent(eventId).map { res =>
      Ok(Json.toJson(res))
    }
  }

  def getFullEvent(eventId: String) = Action.async {
    VoxxrinApi.getFullEvent(eventId).map { res =>
      Ok(Json.toJson(res))
    }
  }

  def getFullEventFormated(eventId: String) = Action.async {
    VoxxrinApi.getFullEvent(eventId).map { res =>
      val (event, sessions) = res.toEvent()
      Ok(Json.toJson(event).as[JsObject] ++ Json.obj(
        "sessions" -> sessions,
        "exponents" -> Json.arr()))
    }
  }

  def getEventDay(eventId: String, dayId: String) = Action.async {
    VoxxrinApi.getEventDay(eventId, dayId).map { res =>
      Ok(Json.toJson(res.schedule.map(_.toSession("eventId"))))
    }
  }

  def getEventSpeaker(eventId: String, speakerId: String) = Action.async {
    VoxxrinApi.getEventSpeaker(eventId, speakerId).map { res =>
      Ok(Json.toJson(res))
    }
  }

  def saveEvent(eventId: String) = Action.async {
    VoxxrinApi.getFullEvent(eventId).flatMap { res =>
      var (event, sessions) = res.toEvent()
      for {
        eventSaved <- EventRepository.insert(event)
        sessionsSaved <- SessionRepository.bulkInsert(sessions)
      } yield {
        Ok(Json.obj("message" -> s"Event $eventId saved in DB (${sessionsSaved} sessions)"))
      }
    }
  }

  def updateEvent(eventId: String, saloonId: String) = Action.async {
    VoxxrinApi.getFullEvent(eventId).flatMap { res =>
      var (event, sessions) = res.toEvent(saloonId)
      for {
        eventSaved <- EventRepository.update(saloonId, event)
        sessionsSaved <- SessionRepository.deleteByEvent(saloonId).flatMap(err => SessionRepository.bulkInsert(sessions))
      } yield {
        Ok(Json.obj("message" -> s"Event $eventId saved in DB (${sessionsSaved} sessions)"))
      }
    }
  }

}
