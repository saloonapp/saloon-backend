package tools.scrapers.mixit.models

import common.models.event._
import common.models.values.Source
import common.models.values.typed.AttendeeRole
import tools.scrapers.mixit.MixitScraper

object Event {
  def url(): String = MixitScraper.baseUrl

  def toGenericEvent(year: Int, staff: List[Attendee], speakers: List[Attendee], sessions: List[Session]): GenericEvent = {
    val sourceName = "MixitApi"
    GenericEvent(
      sources = List(Source(year.toString, sourceName, url())),
      uuid = s"mixit-$year",
      name = s"Mix-IT $year",
      GenericEventInfo(
        logo = "https://www.mix-it.fr/img/logo-mixit-transparent.svg",
        start = None,
        end = None,
        description = "La conférence lyonnaise à ne pas manquer ! Deux jours de découvertes et de rencontres dans une ambiance conviviale, dédiée à la mixité des sujets, des technologies mais aussi des participants.",
        descriptionHTML = "<p>La conférence lyonnaise à ne pas manquer ! Deux jours de découvertes et de rencontres dans une ambiance conviviale, dédiée à la mixité des sujets, des technologies mais aussi des participants.</p>",
        venue = None,
        organizers = List(),
        website = Some("https://www.mix-it.fr"),
        email = None,
        phone = None),
      tags = List("Agilité", "Développement", "Innovation"),
      socialUrls = Map(),
      stats = GenericEventStats(
        year = None,
        area = None,
        exponents = None,
        registration = None,
        visitors = None),
      status = GenericEvent.Status.draft,
      attendees = (staff.map(s => (AttendeeRole.staff, s)) ++ speakers.filter(s => !staff.exists(t => s.idMember==t.idMember)).map(s => (AttendeeRole.speaker, s))).map { case (role, speaker) =>
        GenericAttendee(
          source = Source(speaker.idMember.toString, sourceName, Attendee.oneUrl(speaker.idMember)),
          firstName = speaker.firstname,
          lastName = speaker.lastname,
          avatar = speaker.logo.getOrElse(""),
          description = speaker.longDescription,
          descriptionHTML = speaker.longDescription,
          role = role.unwrap,
          siteUrl = speaker.userLinks.find{ case KeyValue(key, _) => List("Site web", "Website", "Blog").contains(key) }.flatMap(_.value),
          twitterUrl = speaker.userLinks.find{ case KeyValue(key, _) => List("Twitter").contains(key) }.flatMap(_.value),
          company = speaker.company.getOrElse(""))
      },
      exponents = List(),
      sessions = sessions.map { session =>
        GenericSession(
          source = Source(session.idSession.toString, sourceName, Session.oneUrl(session.idSession)),
          name = session.title,
          description = session.description,
          descriptionHTML = session.description,
          format = session.format,
          theme = "",
          place = session.room.getOrElse(""),
          start = session.start,
          end = session.end)
      },
      exponentTeam = Map(),
      sessionSpeakers = sessions.map { session =>
        val speakerIds = session.links.filter(_.rel == "speaker").map(_.href.replace("https://www.mix-it.fr/api/member/", "")).toList
        (session.idSession.toString, speakerIds)
      }.toMap)
  }
}
