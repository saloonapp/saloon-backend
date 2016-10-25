package tools.scrapers.blendwebmix.models

import common.models.event._
import common.models.values.Source
import tools.scrapers.blendwebmix.BlendWebMixScraper

object BlendWebMixEvent {
  def from(sessions: List[BlendWebMixSession], speakers: List[BlendWebMixSpeaker]): GenericEvent = {
    val sourceName = "BlendWebMixScraper"
    val year = "2016"
    val source = Source(year, sourceName, BlendWebMixScraper.baseUrl)
    val gAttendees = speakers.map { speaker =>
      GenericAttendee(
        source = Source(speaker.url, sourceName, speaker.url),
        firstName = speaker.firstName,
        lastName = speaker.lastName,
        avatar = speaker.avatar,
        description = speaker.description,
        descriptionHTML = speaker.descriptionHTML,
        role = "speaker",
        siteUrl = speaker.siteUrl,
        twitterUrl = speaker.twitterUrl,
        company = speaker.company.getOrElse(""))
    }
    val gSessions = sessions.map { session =>
      GenericSession(
        source = Source(session.url, sourceName, session.url),
        name = session.name,
        description = session.description,
        descriptionHTML = session.descriptionHTML,
        format = session.level,
        theme = session.theme,
        place = session.place,
        start = session.start,
        end = session.end)
    }
    val sessionSpeakers = sessions.map { session =>
      (session.url, session.speakers)
    }.filter(_._2.nonEmpty).toMap
    GenericEvent(
      sources = List(source),
      uuid = "",
      name = "Blend Web Mix "+year,
      info = GenericEventInfo(
        logo = "http://www.blendwebmix.com/static/apps/blend_templates/images/logo.png",
        start = sessions.flatMap(_.start).sortBy(_.getMillis).headOption,
        end = sessions.flatMap(_.end).sortBy(-_.getMillis).headOption,
        description = "",
        descriptionHTML = "",
        venue = None,
        organizers = List(),
        website = Some(BlendWebMixScraper.baseUrl),
        email = None,
        phone = None),
      tags = List(),
      socialUrls = Map(),
      stats = GenericEventStats(year = None, area = None, exponents = None, registration = None, visitors = None),
      status = GenericEvent.Status.draft,
      attendees = gAttendees,
      exponents = List(),
      sessions = gSessions,
      exponentTeam = Map(),
      sessionSpeakers = sessionSpeakers)
  }
}
