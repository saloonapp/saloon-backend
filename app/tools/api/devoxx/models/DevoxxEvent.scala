package tools.api.devoxx.models

import common.models.values.Source
import common.models.values.typed.AttendeeRole
import common.models.event.GenericEvent
import common.models.event.GenericEventInfo
import common.models.event.GenericEventStats
import common.models.event.GenericAttendee
import common.models.event.GenericExponent
import common.models.event.GenericSession
import play.api.libs.json.Json

case class DevoxxEvent(
  eventCode: String,
  label: String,
  sourceUrl: Option[String])
object DevoxxEvent {
  implicit val format = Json.format[DevoxxEvent]

  def toGenericEvent(event: DevoxxEvent, speakers: List[DevoxxSpeaker], sessions: List[DevoxxSession]): GenericEvent = {
    val sourceName = "DevoxxApi"
    val gAttendees = speakers.map { speaker =>
      GenericAttendee(
        Source(speaker.uuid, sourceName, speaker.sourceUrl.get),
        speaker.firstName,
        speaker.lastName,
        speaker.avatarURL.getOrElse(""),
        speaker.bio,
        speaker.bioAsHtml,
        AttendeeRole.speaker.unwrap,
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
    val exponentTeam = Map[String, List[String]]()
    val sessionSpeakers = sessions.map { session =>
      session.talk.map { talk =>
        (session.slotId, talk.speakers.map(_.link.href.split("/speakers/")(1)))
      }
    }.flatten.toMap

    GenericEvent(
      List(Source(event.eventCode, sourceName, event.sourceUrl.get)),
      "", // uuid
      event.label,
      GenericEventInfo(
        "", // logo
        Some(gSessions.map(_.start).flatten.minBy(_.getMillis())),
        Some(gSessions.map(_.end).flatten.maxBy(_.getMillis())),
        "", // decription
        "", // decriptionHTML
        None, // venue
        List(), // organizers
        None, // website
        None, // email
        None), // phone
      List(), // tags
      Map(), // socialUrls
      GenericEventStats(None, None, None, None, None),
      GenericEvent.Status.draft,
      gAttendees,
      gExponent,
      gSessions,
      exponentTeam,
      sessionSpeakers)
  }
}
