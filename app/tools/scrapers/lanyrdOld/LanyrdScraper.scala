package tools.scrapers.lanyrdOld

import tools.scrapers.lanyrdOld.models._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current
import play.api.libs.ws._
import scala.collection.JavaConversions._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object LanyrdScraper {
  val baseUrl = "http://lanyrd.com"
  def placeUrl(place: String): String = s"$baseUrl/places/$place/"
  def eventUrl(year: String, id: String): String = s"$baseUrl/$year/$id/"

  // TODO : really deep scraping, ex : http://lanyrd.com/2015/agilefrance/ (speakers, sessions)
  def getEventDetails(url: String): Future[LanyrdEvent] = {
    WS.url(url).get().map { response =>
      val doc = Jsoup.parse(response.body)
      val name = doc.select("h1.summary").text()
      val places = doc.select(".prominent-place a").toList.map { a => if (a.hasClass("flag-large")) None else Some(getLink(a, baseUrl)) }.flatten.flatten
      val start = parseDetailsDate(doc.select(".date .dtstart").attr("title")) // May 29, 2015
      val end = parseDetailsDate(doc.select(".date .dtend").attr("title")) // May 31, 2015
      val tags = doc.select(".tags a").toList.map { a => getLink(a, baseUrl) }.flatten

      val baseLine = doc.select("h2.tagline").text()
      val description = doc.select("#event-description").html()
      val website = getLink(doc.select("a.website").first())
      val schedule = getLink(doc.select("a.official").first())
      val twitterAccount = getLink(doc.select("a.twitter").first())
      val twitterHashtag = getLink(doc.select("a.hashtag").first())

      val venueElt = doc.select(".venue")
      val venue = if (venueElt.size > 0) {
        val venueNameElt = venueElt.select("h3 a")
        val venueName = venueNameElt.text()
        val venueUrl = baseUrl + venueNameElt.attr("href")
        val venueAddress = venueElt.select("p").get(1).text()
        val venueMap = venueElt.select("p .map-icon").attr("href")
        Some(LanyrdVenue(venueName, venueAddress, venueUrl, venueMap))
      } else {
        None
      }

      val details = LanyrdEventDetails(baseLine, description, venue, website, schedule, twitterAccount, twitterHashtag)
      LanyrdEvent(name, url, places, start, end, tags, Some(details))
    }
  }

  def getEventListMulti(url: String, page: Int, maxPages: Int): Future[LanyrdListPage] = {
    getEventList(url, page).flatMap { lanyrdFirstPage =>
      val lastPage = if (page + maxPages < lanyrdFirstPage.maxPages + 1) page + maxPages else lanyrdFirstPage.maxPages + 1
      Future.sequence((page + 1 until lastPage).map { n => getEventList(url, n) }).map { nextPages =>
        lanyrdFirstPage.copy(
          events = lanyrdFirstPage.events ++ nextPages.map(_.events).flatten,
          loadedPages = lanyrdFirstPage.loadedPages ++ nextPages.map(_.loadedPages).flatten)
      }
    }
  }

  def getEventList(url: String, page: Int): Future[LanyrdListPage] = {
    val paginatedUrl = url + s"?page=$page"
    WS.url(paginatedUrl).get().map { response =>
      val doc = Jsoup.parse(response.body)
      val events = doc.select("li.conference.vevent").toList.map { event =>
        val title = event.select("h4 a")
        val name = title.text()
        val url = baseUrl + title.attr("href")
        val places = event.select(".location a").toList.map { a => if (a.hasClass("flag-small")) None else Some(getLink(a, baseUrl)) }.flatten.flatten
        val start = parseListDate(event.select(".date .dtstart").attr("title")) // 2014-05-16
        val end = parseListDate(event.select(".date .dtend").attr("title")) // 2014-05-17
        val tags = event.select(".tags a").toList.map { a => getLink(a, baseUrl) }.flatten
        LanyrdEvent(name, url, places, start, end, tags, None)
      }
      val nbPagesElt = doc.select(".pagination li")
      val maxPages = if (nbPagesElt.size > 0) nbPagesElt.last().text().toInt else 1
      LanyrdListPage(paginatedUrl, page, maxPages, events, List(page))
    }
  }

  // utils

  private def getLink(elt: Element, baseUrl: String = ""): Option[LanyrdLink] = if (elt == null) None else Some(LanyrdLink(elt.text(), baseUrl + elt.attr("href")))
  def parseDetailsDate(date: String) = if (date.isEmpty()) None else Some(DateTime.parse(date, DateTimeFormat.forPattern("MMM dd, yyyy").withLocale(java.util.Locale.ENGLISH))) // May 29, 2015
  def parseListDate(date: String) = if (date.isEmpty()) None else Some(DateTime.parse(date, DateTimeFormat.forPattern("yyyy-MM-dd").withLocale(java.util.Locale.ENGLISH))) // 2014-05-17
}
