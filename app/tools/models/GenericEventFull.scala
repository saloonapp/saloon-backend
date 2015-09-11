package tools.models

import tools.api.Devoxx.models.DevoxxEvent
import tools.api.Devoxx.models.DevoxxSpeaker
import tools.api.Devoxx.models.DevoxxSession
import play.api.libs.json.Json

case class GenericEventFull(
  event: GenericEvent,
  attendees: List[GenericAttendee],
  exponents: List[GenericExponent],
  sessions: List[GenericSession],
  exponentTeam: Map[String, List[String]],
  sessionSpeakers: Map[String, List[String]])
object GenericEventFull {
  implicit val format = Json.format[GenericEventFull]

  def build(event: DevoxxEvent, speakers: List[DevoxxSpeaker], sessions: List[DevoxxSession]): GenericEventFull = {
    val sourceName = "DevoxxApi"
    val gAttendees = speakers.map { speaker =>
      GenericAttendee(
        Source(speaker.uuid, sourceName, speaker.sourceUrl.get),
        speaker.firstName,
        speaker.lastName,
        speaker.avatarURL.getOrElse(""),
        speaker.bio,
        speaker.bioAsHtml,
        speaker.blog,
        speaker.twitter.map(_.replace("@", "https://twitter.com/")),
        speaker.company.getOrElse(""))
    }
    val gExponent = List()
    val gSessions = sessions.map { session =>
      session.talk.map { talk =>
        GenericSession(
          Source(session.slotId, sourceName, session.sourceUrl.get),
          talk.title,
          talk.summary,
          talk.summaryAsHtml,
          talk.talkType,
          talk.track,
          session.roomName,
          Some(session.fromTimeMillis),
          Some(session.toTimeMillis))
      }.orElse(session.break.map { break =>
        GenericSession(
          Source(session.slotId, sourceName, session.sourceUrl.get),
          break.nameFR,
          "",
          "",
          "break",
          "",
          session.roomName,
          Some(session.fromTimeMillis),
          Some(session.toTimeMillis))
      })
    }.flatten
    val gEvent = GenericEvent(
      Source(event.eventCode, sourceName, event.sourceUrl.get),
      event.label,
      Some(gSessions.map(_.start).flatten.minBy(_.getMillis())),
      Some(gSessions.map(_.end).flatten.maxBy(_.getMillis())))
    val exponentTeam = Map[String, List[String]]()
    val sessionSpeakers = sessions.map { session =>
      session.talk.map { talk =>
        (session.slotId, talk.speakers.map(_.link.href.split("/speakers/")(1)))
      }
    }.flatten.toMap

    GenericEventFull(gEvent, gAttendees, gExponent, gSessions, exponentTeam, sessionSpeakers)
  }
}