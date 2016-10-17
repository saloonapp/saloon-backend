package tools.api.devfesttoulouse.models

import common.models.event._
import common.models.values.Source
import common.models.values.typed.AttendeeRole
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.jsoup.Jsoup
import tools.api.devfesttoulouse.DevFestUrl

object DevFestEvent {
  def toGenericEvent(conferenceUrl: String, speakers: List[DevFestSpeaker], sessions: List[DevFestSession], schedules: List[DevFestSchedule]): GenericEvent = {
    val sourceName = "DevFestToulouseApi"
    val year = "2016"
    val gAttendees: List[GenericAttendee] = speakers.map { speaker =>
      GenericAttendee(
        source = Source(speaker.id.toString, sourceName, speaker.sourceUrl.get),
        firstName = speaker.name.split(" ").head,
        lastName = speaker.name.split(" ").drop(1).mkString(" "),
        avatar = speaker.photoUrl,
        description = Jsoup.parse(speaker.bio).text(),
        descriptionHTML = speaker.bio,
        role = AttendeeRole.speaker.unwrap,
        siteUrl = None,
        twitterUrl = None,
        company = speaker.company)
    }
    val dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd H:mm")
    val sessionSlot: Map[Int, (DateTime, DateTime)] = schedules.flatMap { schedule =>
      schedule.timeslots.flatMap { slot =>
        val start = DateTime.parse(schedule.date+" "+slot.startTime, dateTimeFormat)
        val end = DateTime.parse(schedule.date+" "+slot.endTime, dateTimeFormat)
        slot.sessions.flatten.map { id =>
          (id, (start, end))
        }
      }
    }.toMap
    val gSessions: List[GenericSession] = sessions.map { session =>
      GenericSession(
        source = Source(session.id.toString, sourceName, session.sourceUrl.get),
        name = session.title,
        description = Jsoup.parse(session.description).text(),
        descriptionHTML = session.description,
        format = "",
        theme = session.tags.flatMap(_.headOption).getOrElse(""),
        place = session.track.map(_.title).getOrElse(""),
        start = sessionSlot.get(session.id).map(_._1),
        end = sessionSlot.get(session.id).map(_._2))
    }
    val gExponent: List[GenericExponent] = List()
    val exponentTeam: Map[String, List[String]] = Map()
    val sessionSpeakers: Map[String, List[String]] = sessions.map { session =>
      (session.id.toString, session.speakers.map(_.map(_.toString)).getOrElse(List()))
    }.toMap
      GenericEvent(
      sources = List(Source("DevFestToulouse"+year, sourceName, conferenceUrl)),
      uuid = "",
      name = "DevFest Toulouse "+year,
      info = GenericEventInfo(
        logo = "",
        start = Some(gSessions.flatMap(_.start).minBy(_.getMillis())),
        end = Some(gSessions.flatMap(_.end).maxBy(_.getMillis())),
        description = "",
        descriptionHTML = "",
        venue = None,
        organizers = List(),
        website = Some(DevFestUrl.baseUrl),
        email = None,
        phone = None),
      tags = List(),
      socialUrls = Map(),
      stats = GenericEventStats(None, None, None, None, None),
      status = GenericEvent.Status.draft,
      attendees = gAttendees,
      exponents = gExponent,
      sessions = gSessions,
      exponentTeam = exponentTeam,
      sessionSpeakers = sessionSpeakers)
  }
}
