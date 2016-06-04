package tools.scrapers.rivieradev.models

import common.models.event._
import common.models.values.Source
import org.jsoup.Jsoup
import tools.scrapers.rivieradev.RivieraDevScraper
import tools.utils.TextUtils

object RivieraDevEvent {
  def build(year: Int, aboutPage: String, sessions: List[RivieraDevSession], speakers: List[RivieraDevSpeaker], sponsors: List[RivieraDevSponsor]): GenericEvent = {
    val doc = Jsoup.parse(aboutPage)
    val title = doc.select(".rd-header h1").text()
    val start = sessions.map(_.start).flatten.sortBy(_.getMillis).headOption
    val end = sessions.map(_.end).flatten.sortBy(-_.getMillis).headOption
    val sessionSpeakers = sessions.filter(_.speakers.length > 0).map { s =>
      (s.source.ref, s.speakers)
    }.toMap
    GenericEvent(
      sources = List(Source(year.toString, RivieraDevScraper.name, RivieraDevScraper.baseUrl(year))),
      uuid = TextUtils.tokenify(title),
      name = title,
      info = GenericEventInfo(
        logo = "",
        start = start,
        end = end,
        description = "",
        descriptionHTML = "",
        venue = None,
        organizers = List(),
        website = Some(RivieraDevScraper.baseUrl(year)),
        email = Some("info@rivieradev.fr"),
        phone = None),
      tags = List(),
      socialUrls = Map("twitter" -> "https://twitter.com/RivieraDEV"),
      stats = GenericEventStats(None, None, None, None, None),
      status = GenericEvent.Status.draft,
      attendees = speakers.map(_.toGeneric),
      exponents = sponsors.map(_.toGeneric),
      sessions = sessions.map(_.toGeneric),
      exponentTeam = Map(),
      sessionSpeakers = sessionSpeakers)
  }
}
