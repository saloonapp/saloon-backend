package tools.scrapers.salonreunir.models

import common.models.values.Source
import common.models.event.GenericEvent
import common.models.event.GenericAttendee
import common.models.event.GenericExponent
import common.models.event.GenericSession
import org.jsoup.Jsoup

object SalonReunirEvent {

  def toGenericEvent(name: String, code: String, url: String, exponents: List[SalonReunirExponent], sessions: List[SalonReunirSession]): GenericEvent = {
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
    val exponentTeam = Map[String, List[String]]()
    val sessionSpeakers = Map[String, List[String]]()

    GenericEvent(
      Source(code, sourceName, url),
      name,
      Some(gSessions.map(_.start).flatten.minBy(_.getMillis())),
      Some(gSessions.map(_.end).flatten.maxBy(_.getMillis())),
      gAttendees,
      gExponent,
      gSessions,
      exponentTeam,
      sessionSpeakers)
  }

  private def notEmpty(str: String): Option[String] = if (str == null || str.isEmpty) None else Some(str)
}