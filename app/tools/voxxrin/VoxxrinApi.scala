package tools.voxxrin

import common.Utils
import tools.voxxrin.models.VoxxrinEvent
import tools.voxxrin.models.VoxxrinDay
import tools.voxxrin.models.VoxxrinSession
import tools.voxxrin.models.VoxxrinSpeaker
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current
import play.api.libs.ws._

object VoxxrinApi {
  var baseUrl = "http://app.voxxr.in/r"
  def eventsUrl(): String = baseUrl + "/events"
  def eventUrl(eventId: String): String = baseUrl + "/events/" + eventId
  def eventDayUrl(eventId: String, dayId: String): String = baseUrl + "/events/" + eventId + "/day/" + dayId
  def eventSpeakerUrl(eventId: String, speakerId: String): String = baseUrl + "/events/" + eventId + "/speakers/" + speakerId

  def getEvents(): Future[List[VoxxrinEvent]] = getEventsByUrl(eventsUrl())
  def getEventsByUrl(url: String): Future[List[VoxxrinEvent]] = {
    WS.url(url).get().map { res =>
      res.json.as[List[VoxxrinEvent]]
    }
  }

  def getEvent(eventId: String): Future[VoxxrinEvent] = getEventByUrl(eventUrl(eventId))
  def getEventByUrl(url: String): Future[VoxxrinEvent] = {
    WS.url(url).get().map { res =>
      res.json.as[VoxxrinEvent]
    }
  }

  def getEventDay(eventId: String, dayId: String): Future[VoxxrinDay] = getEventDayByUrl(eventDayUrl(eventId, dayId))
  def getEventDayByUrl(url: String): Future[VoxxrinDay] = {
    WS.url(url).get().map { res =>
      res.json.as[VoxxrinDay]
    }
  }

  def getEventSpeaker(eventId: String, speakerId: String): Future[VoxxrinSpeaker] = getEventSpeakerByUrl(eventSpeakerUrl(eventId, speakerId))
  def getEventSpeakerByUrl(url: String): Future[VoxxrinSpeaker] = {
    WS.url(url).get().map { res =>
      res.json.as[VoxxrinSpeaker]
    }
  }

  def getFullEvent(eventId: String): Future[VoxxrinEvent] = getFullEventByUrl(eventUrl(eventId))
  def getFullEventByUrl(url: String): Future[VoxxrinEvent] = {
    getEventByUrl(url).flatMap(withSchedule).flatMap(withSessionSpeakers)
  }

  private def withSchedule(event: VoxxrinEvent): Future[VoxxrinEvent] = {
    getEventSchedule(event).map { schedule =>
      event.copy(schedule = Some(schedule))
    }
  }
  private def getEventSchedule(event: VoxxrinEvent): Future[List[VoxxrinSession]] = {
    val schedules: List[Future[List[VoxxrinSession]]] = event.days.map { day =>
      getEventDayByUrl(baseUrl + day.uri).map { voxxrinDay => voxxrinDay.schedule }
    }
    Future.sequence(schedules).map(_.flatten)
  }

  private def withSessionSpeakers(event: VoxxrinEvent): Future[VoxxrinEvent] = {
    event.schedule.map { sessions =>
      Future.sequence(sessions.map(withSpeakers)).map { sessionsWithSpeakers =>
        event.copy(schedule = Some(sessionsWithSpeakers))
      }
    }.getOrElse(Future(event))
  }
  private def withSpeakers(session: VoxxrinSession): Future[VoxxrinSession] = {
    getSessionSpeakers(session).map { speakers =>
      session.copy(speakers = speakers)
    }
  }
  private def getSessionSpeakers(session: VoxxrinSession): Future[Option[List[VoxxrinSpeaker]]] = {
    val speakersOpt: Option[List[Future[VoxxrinSpeaker]]] = session.speakers.map {
      _.map { speaker =>
        speaker.uri.map { uri =>
          getEventSpeakerByUrl(baseUrl + uri).map { _.merge(speaker) }
        }.getOrElse(Future(speaker))
      }
    }
    Utils.transform(speakersOpt.map(speakers => Future.sequence(speakers)))
  }
}
