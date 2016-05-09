package tools.scrapers.bestofweb.models

import common.models.event._
import common.models.values.Source
import org.joda.time.DateTime
import org.jsoup.Jsoup
import tools.scrapers.bestofweb.BestOfWebScraper
import tools.utils.{ScraperUtils, TextUtils}
import scala.collection.JavaConversions._

object BestOfWebEvent {
  def fromHTML(html: String): GenericEvent = {
    val doc = Jsoup.parse(html)
    val sourceName = "BestOfWebScraper"
    val source = Source("2016", sourceName, BestOfWebScraper.baseUrl)
    val start = new DateTime(2016, 6, 9, 10, 0)
    val title = doc.select(".brand-title").text()
    val description = ScraperUtils.firstSafe(doc.select("#about p"))
    val organizers = doc.select(".meetups").map { o =>
      val img = o.select("img").attr("src")
      GenericEventOrganizer(
        logo = if(img != null && img.length > 0) BestOfWebScraper.baseUrl+"/"+img else "",
        name = o.select("span").text(),
        address = GenericEventAddress(name = "",complement = "",street = "",zipCode = "",city = "",country = ""),
        website = Some(o.select("a").attr("href")),
        email = None,
        phone = None)
    }.toList
    val speakers = doc.select(".speaker").map { s =>
      val name = s.select(".media-heading a").text()
      val twitter = s.select(".media-heading a").attr("href")
      val img = s.select("img").attr("src")
      GenericAttendee(
        source = Source(twitter, sourceName, BestOfWebScraper.baseUrl),
        firstName = name.split(" ").take(1).headOption.getOrElse(""),
        lastName = name.split(" ").drop(1).mkString(" "),
        avatar = if(img != null && img.length > 0) BestOfWebScraper.baseUrl+img else "",
        description = "",
        descriptionHTML = "",
        role = "speaker",
        siteUrl = None,
        twitterUrl = Some(twitter),
        company = "")
    }.groupBy(_.source.ref).map { case (k, v) => v.head }.toList
    val orgas = doc.select(".organizer").map { o =>
      val name = o.select("h4 a").text()
      val twitter = o.select("h4 a").attr("href")
      val img = o.select("img").attr("src")
      GenericAttendee(
        source = Source(twitter, sourceName, BestOfWebScraper.baseUrl),
        firstName = name.split(" ").take(1).headOption.getOrElse(""),
        lastName = name.split(" ").drop(1).mkString(" "),
        avatar = if(img != null && img.length > 0) BestOfWebScraper.baseUrl+"/"+img else "",
        description = "",
        descriptionHTML = "",
        role = "orga",
        siteUrl = None,
        twitterUrl = Some(twitter),
        company = "")
    }
    val formations = doc.select("#schedule-thursday .media").toList.map(BestOfWebSession.fromMedia)
    val conferencesTmp = doc.select("#schedule-friday .media").toList.map(BestOfWebSession.fromMedia)
    val conferences = conferencesTmp.sliding(2).map(list => list.head.copy(hour = list(0).hour+"-"+list(1).hour)).toList ++ List(conferencesTmp.last.copy(hour = conferencesTmp.last.hour+"-18:00"))
    val sessions = formations.map(_.toGenericSession(sourceName, start, "Formation")) ++ conferences.map(_.toGenericSession(sourceName, start.plusDays(1), "Conference"))
    val sessionSpeakers = (formations ++ conferences).map { s =>
      (s.ref, s.speakers)
    }.toMap
    val exponents = doc.select(".sponsors").map { e =>
      // TODO : add logo / site for exponent !
      val site = e.select("a").attr("href")
      val name = e.select("img").attr("alt")
      val logo = BestOfWebScraper.baseUrl+e.select("img").attr("src")
      GenericExponent(
        source = Source(site, sourceName, BestOfWebScraper.baseUrl),
        name = name,
        description = "",
        descriptionHTML = "",
        logo = logo,
        website = site,
        place = "")
    }.toList
    GenericEvent(
      sources = List(source),
      uuid = TextUtils.tokenify(title),
      name = title,
      info = GenericEventInfo(
        logo = "",
        start = Some(start),
        end = None,//Some(sessions.map(_.end).flatten.max),
        description = description.map(_.text()).getOrElse(""),
        descriptionHTML = description.map(_.html()).getOrElse(""),
        venue = Some(GenericEventVenue(
          logo = "http://bestofweb.paris/images/partners/grandeCrypte.jpg",
          name = "La Grande Crypte",
          address = GenericEventAddress(
            name = "La Grande Crypte",
            complement = "",
            street = "69 bis Rue Boissière",
            zipCode = "75116",
            city = "Paris",
            country = "France"),
          website = Some("http://lagrandecrypte.com/"),
          email = None,
          phone = None)),
        organizers = organizers,
        website = None,
        email = None,
        phone = None),
      tags = List(),
      socialUrls = Map("twitter" -> "https://twitter.com/bestofwebconf"),
      stats = GenericEventStats( year = None, area = None, exponents = None, registration = None, visitors = None),
      status = GenericEvent.Status.draft,
      attendees = speakers ++ orgas,
      exponents = exponents,
      sessions = sessions,
      exponentTeam = Map(),
      sessionSpeakers = sessionSpeakers)
  }
}
