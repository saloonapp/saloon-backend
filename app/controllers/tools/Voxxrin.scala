package controllers.tools

import tools.voxxrin.VoxxrinApi
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.libs.json.Json

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

  def getEventDay(eventId: String, dayId: String) = Action.async {
    VoxxrinApi.getEventDay(eventId, dayId).map { res =>
      Ok(Json.toJson(res))
    }
  }

  def getEventSpeaker(eventId: String, speakerId: String) = Action.async {
    VoxxrinApi.getEventSpeaker(eventId, speakerId).map { res =>
      Ok(Json.toJson(res))
    }
  }

}
