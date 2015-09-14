package tools.scrapers.lanyrd

import tools.utils.Scraper
import tools.utils.ScraperUtils
import tools.scrapers.lanyrd.models.LanyrdEvent
import tools.scrapers.lanyrd.models.LanyrdAddress
import scala.collection.JavaConversions._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.util.Locale

/*
 * List page ex :
 *  - http://lanyrd.com/places/7153319/
 * Details page ex :
 *  - http://lanyrd.com/2015/agilefrance/
 */
object LanyrdScraper extends Scraper[LanyrdEvent] {
  val baseUrl = "http://lanyrd.com"

  override def extractLinkList(html: String, baseUrl: String): List[String] = {
    val doc = Jsoup.parse(html)
    doc.select("li.conference.vevent").toList.map { event =>
      baseUrl + event.select("h4 a.url").attr("href")
    }
  }

  override def extractLinkPages(html: String): List[String] = {
    val doc = Jsoup.parse(html)
    val pagesElts = doc.select(".pagination li a")
    if (pagesElts.size > 0) {
      val lastPage = pagesElts.last()
      val pageUrl = baseUrl + lastPage.attr("href")
      val maxPages = lastPage.text().toInt
      (2 to maxPages).map { p => pageUrl.replace("?page=" + maxPages, "?page=" + p) }.toList
    } else {
      List()
    }
  }

  val venueStreetRegex = "(.*?) \\(map\\)".r
  override def extractDetails(html: String, baseUrl: String, pageUrl: String): LanyrdEvent = {
    val doc = Jsoup.parse(html)
    val name = doc.select("h1.summary").text()
    val description = doc.select("#event-description").html()
    val tagline = doc.select("h2.tagline").html()
    val descriptionHTML = if (description.isEmpty()) tagline else description
    val start = ScraperUtils.parseDate(doc.select(".date .dtstart").attr("title"))
    val end = ScraperUtils.parseDate(doc.select(".date .dtend").attr("title")).orElse(start)
    val websiteUrl = ScraperUtils.getLink(doc.select("a.website")).map(_._2).getOrElse("")
    val scheduleUrl = ScraperUtils.getLink(doc.select("a.official")).map(_._2).getOrElse("")
    val eventCity = doc.select("p.prominent-place a.sub-place").text()
    val eventCountry = doc.select("p.prominent-place .place-context a").text()
    val address = doc.select("#venues .venue-list li").map { venue =>
      val (name, url) = ScraperUtils.getLink(venue.select("a.venuename"), baseUrl).getOrElse(("", ""))
      val gmap = venue.select(".not-in-infowindow a").attr("href")
      val street = venue.select("p").first().text() match {
        case venueStreetRegex(street) => street
        case _ => ""
      }
      LanyrdAddress(name, street, eventCity, eventCountry, url, gmap)
    }.toList.headOption.orElse {
      doc.select("#venues").map { venue =>
        val (name, url) = ScraperUtils.getLink(venue.select("h3 a"), baseUrl).getOrElse(("", ""))
        val gmap = venue.select("a.map-icon").attr("href")
        val street = venue.select("p")(1).text()
        val place = venue.select("p.primary-place a")
        val country = ScraperUtils.getSafe(place, 1).map(_.text()).getOrElse(eventCountry)
        val city = ScraperUtils.getSafe(place, 2).map(_.text()).getOrElse(eventCity)
        LanyrdAddress(name, street, city, country, url, gmap)
      }.toList.headOption
    }.getOrElse {
      LanyrdAddress("", "", eventCity, eventCountry, "", "")
    }
    val twitterAccountUrl = ScraperUtils.firstSafe(doc.select("a.twitter")).map(_.attr("href")).getOrElse("")
    val twitterHashtagUrl = ScraperUtils.firstSafe(doc.select("a.twitter-search")).map(_.attr("href")).getOrElse("")
    val tags = doc.select("#tagblock .tags a").toList.map { _.text() }

    LanyrdEvent(name, descriptionHTML, start, end, websiteUrl, scheduleUrl, address, twitterAccountUrl, twitterHashtagUrl, tags, pageUrl)
  }
}
