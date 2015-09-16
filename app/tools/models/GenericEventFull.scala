package tools.models

import tools.api.devoxx.models.DevoxxEvent
import tools.api.devoxx.models.DevoxxSpeaker
import tools.api.devoxx.models.DevoxxSession
import tools.scrapers.salonreunir.models.SalonReunirSession
import tools.scrapers.salonreunir.models.SalonReunirExponent
import common.models.values.typed.AttendeeRole
import play.api.libs.json.Json
import org.jsoup.Jsoup

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

  def build(name: String, code: String, url: String, exponents: List[SalonReunirExponent], sessions: List[SalonReunirSession]): GenericEventFull = {
    val sourceName = "SalonReunirScraper"
    val gAttendees = List()
    val gExponent = exponents.map { exponent =>
      val addressDesc = if (exponent.address.isEmpty) { "" } else { "<p>" + exponent.address + "</p>" }
      val contactDesc = if (exponent.contactName.isEmpty && exponent.contactPhone.isEmpty && exponent.contactEmail.isEmpty) { "" } else {
        "<p>" +
          notEmpty(exponent.contactName).map(_ + "<br>").getOrElse("") +
          notEmpty(exponent.contactPhone).map(_ + "<br>").getOrElse("") +
          notEmpty(exponent.contactEmail).map(_ + "<br>").getOrElse("") +
          "</p>"
      }
      val descHTML = addressDesc + contactDesc + exponent.descriptionHTML
      GenericExponent(
        Source(exponent.ref, sourceName, exponent.url),
        exponent.name,
        Jsoup.parse(descHTML).text(),
        descHTML,
        exponent.place)
    }
    val gSessions = sessions.map { session =>
      GenericSession(
        Source(session.ref, sourceName, session.url),
        session.name,
        session.animator,
        session.animator,
        session.format,
        "",
        session.place,
        Some(session.start),
        Some(session.end))
    }
    val gEvent = GenericEvent(
      Source(code, sourceName, url),
      name,
      Some(gSessions.map(_.start).flatten.minBy(_.getMillis())),
      Some(gSessions.map(_.end).flatten.maxBy(_.getMillis())))
    val exponentTeam = Map[String, List[String]]()
    val sessionSpeakers = Map[String, List[String]]()
    GenericEventFull(gEvent, gAttendees, gExponent, gSessions, exponentTeam, sessionSpeakers)
  }

  private def notEmpty(str: String): Option[String] = if (str == null || str.isEmpty) None else Some(str)
}