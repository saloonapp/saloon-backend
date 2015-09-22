package tools.scrapers.eventseye

import common.models.event.GenericEvent
import tools.utils.Scraper
import tools.utils.ScraperUtils
import tools.utils.CsvUtils
import tools.scrapers.eventseye.models.EventsEyeEvent
import tools.scrapers.eventseye.models.EventsEyeAttendance
import tools.scrapers.eventseye.models.EventsEyeOrganizer
import tools.scrapers.eventseye.models.EventsEyeVenue
import tools.scrapers.eventseye.models.EventsEyeAddress
import tools.scrapers.eventseye.models.EventsEyePeriod
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.api.libs.json.Json
import scala.collection.JavaConversions._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.Locale
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

/*
 * List page ex :
 *  - http://www.eventseye.com/fairs/cy1_trade-shows-paris.html
 * Details page ex :
 *  - http://www.eventseye.com/fairs/f-expobois-865-1.html
 *  - http://www.eventseye.com/fairs/f-sitl-solutions-logistiques-2515-1.html
 *  - http://www.eventseye.com/fairs/f-congres-international-de-metrologie-503-1.html (no phone number for orga)
 *  - http://www.eventseye.com/fairs/f-aps-alarmes-protection-securite-65-1.html (add attendence)
 */
object EventsEyeScraper extends Scraper[EventsEyeEvent] {
  val baseUrl = "http://www.eventseye.com"
  override def toCsv(value: EventsEyeEvent): Map[String, String] = CsvUtils.jsonToCsv(Json.toJson(value), 4)
  override def toGenericEvent(value: EventsEyeEvent): List[GenericEvent] = (List(value) ++ value.otherDates.map { p => value.copy(start = p.start, end = p.end) }).zipWithIndex.map { case (e, i) => EventsEyeEvent.toGenericEvent(e, i) }

  /*
   * Scraper
   */

  override def extractLinkList(html: String, baseUrl: String): List[String] = {
    val doc = Jsoup.parse(html)
    val tables = doc.select("table.mt")
    tables.map { table =>
      table.select("tr.mt").map { row =>
        baseUrl + "/fairs/" + row.select("td")(0).select("a").attr("href")
      }.toList
    }.toList.flatten
  }

  override def extractDetails(html: String, baseUrl: String, pageUrl: String): EventsEyeEvent = {
    val doc = Jsoup.parse(html)
    val sections = doc.select("body > form > table > tbody > tr:eq(2) > td:eq(0) > table")
    val headerSection = sections(0)
    val descriptionSection = sections(1)
    val datesSection = sections(2)
    val venueSection = sections(5)
    val orgaSection = sections(7)
    val hasAttendance = sections(8).text().contains("Attendance")
    val attendanceSectionOpt = if (hasAttendance) { Some(sections(9)) } else { None }
    val moreSection = if (hasAttendance) { sections(10) } else { sections(8) }

    val logo = baseUrl + headerSection.select("tr:eq(0) td:eq(0) img").attr("src")
    val name = headerSection.select("tr:eq(1) td:eq(0) h1").text()

    val industries = descriptionSection.select("a.otherlink-bold").map(_.text()).toList
    val decription = descriptionSection.select("tr:eq(2) td:eq(2)").text()
    val audience = descriptionSection.select("tr:eq(2) td:eq(4)").text()
    val cycle = descriptionSection.select("tr:eq(2) td:eq(6)").text()

    val nextDates = datesSection.select("tr:eq(2) td:eq(0) table tr").map { row => extractDates(row.select("td:eq(0) span").text()) }.toList

    val venue = extractVenue(venueSection.select("tr:eq(0) td:eq(0) table").html())

    val orgas = orgaSection.select("td table").map { orga => extractOrganizer(orga.html()) }.toList

    val attendance = attendanceSectionOpt.map { attendanceSection =>
      val year = attendanceSection.select("td:eq(0) b").text().replace(" ", "")
      val exponents = attendanceSection.select("td:eq(1) b").text().replace(" ", "")
      val visitors = attendanceSection.select("td:eq(2) b").text().replace(" ", "")
      val exhibitionSpace = attendanceSection.select("td:eq(3) b").text().replace(" ", "")
      EventsEyeAttendance(year, exponents, visitors, exhibitionSpace)
    }

    val more = moreSection.select("tr:eq(2) td:eq(0) a").map { a => a.attr("href").replace("mailto:", "") }.toList
    val (website, email, phone) = extractMore(moreSection.select("tr:eq(2) td:eq(0)").html())

    val (start, end) = nextDates.headOption.getOrElse((None, None))
    val otherDates = nextDates.drop(1).map { case (start, end) => EventsEyePeriod(start, end) }
    EventsEyeEvent(logo, name, industries, decription, audience, cycle, start, end, otherDates, venue, orgas, attendance, website, email, phone, pageUrl)
  }

  private val dateRegex1 = "([a-zA-Z.]+) ([0-9]+) - ([a-zA-Z.]+)? ?([0-9]+), ([0-9]+)".r.unanchored
  private val dateRegex2 = "on ([a-zA-Z.]+) ([0-9]+), ([0-9]+)".r.unanchored
  private val dateRegex3 = "on ([a-zA-Z.]+) ([0-9]+) \\(\\?\\)".r.unanchored
  private def extractDates(date: String): (Option[DateTime], Option[DateTime]) = date match {
    case dateRegex1(monthStart, dayStart, monthEnd, dayEnd, year) => {
      val realMonthEnd = if (monthEnd != null) monthEnd else monthStart
      (ScraperUtils.parseDate(s"$monthStart $dayStart, $year"), ScraperUtils.parseDate(s"$realMonthEnd $dayEnd, $year"))
    }
    case dateRegex2(month, day, year) => (ScraperUtils.parseDate(s"$month $day, $year"), None)
    case dateRegex3(month, year) => (ScraperUtils.parseDate(s"$month 01, $year"), None)
    case _ => (None, None)
  }

  private val rVenueLogo = "<img src=\"([^\"]+)\" width=\"".r.unanchored
  private val rVenueAddress = "<br>\\s+(?:([^<]+)<br>)?([^<]+)<br>\\s+<a[^>]*><b>([^<]+)</b></a><br>".r.unanchored
  private val rVenuePhone = "<img src=\"/i/tel.gif\"[^>]*>&nbsp;([^<]+)".r.unanchored
  private val rVenueWebsite = "<a href=\"([^\"]+)\"[^>]*><img src=\"/i/web.gif\"[^>]*>&nbsp;Web Site</a>".r.unanchored
  private val rVenueEmail = "<a href=\"mailto:([^\"]+)\"[^>]*><img src=\"/i/mail.gif\"[^>]*>&nbsp;E-mail</a>".r.unanchored
  private def extractVenue(html: String): EventsEyeVenue = {
    val elt = Jsoup.parse(html)
    val logo = ScraperUtils.get(html, rVenueLogo).map(baseUrl + _).getOrElse("")
    val name = elt.select("a .etb").text()
    val address = html match {
      case rVenueAddress(name, street, country) => EventsEyeAddress(notNull(name), "", notNull(street), "", notNull(country))
      case _ => EventsEyeAddress("", "", "", "", "")
    }
    val phone = ScraperUtils.get(html, rVenuePhone).getOrElse("")
    val site = ScraperUtils.get(html, rVenueWebsite).getOrElse("")
    val email = ScraperUtils.get(html, rVenueEmail).getOrElse("")
    EventsEyeVenue(logo, name, address, phone, site, email)
  }

  private val rOrganizerLogo = rVenueLogo
  private val rOrganizerAddress = "<br>(?:(.*?)<br>\\s)?(?:(.*?)<br>\\s)?(?:(.*?)<br>\\s)?(?:(.*?)\\s)?<br><b>(.*?)</b>\\s+".r.unanchored
  private val rOrganizerPhone = rVenuePhone
  private val rOrganizerWebsite = rVenueWebsite
  private val rOrganizerEmail = rVenueEmail
  private def extractOrganizer(html: String): EventsEyeOrganizer = {
    val elt = Jsoup.parse(html)
    val logo = ScraperUtils.get(html, rOrganizerLogo).map(baseUrl + _).getOrElse("")
    val name = elt.select("a .et").text().replace("\u00a0", "")
    val address = html match {
      case rOrganizerAddress(name, complement, street, city, country) => EventsEyeAddress(notNull(name), notNull(complement), notNull(street), notNull(city), notNull(country))
      case _ => EventsEyeAddress("", "", "", "", "")
    }
    val phone = ScraperUtils.get(html, rOrganizerPhone).getOrElse("").trim
    val site = ScraperUtils.get(html, rOrganizerWebsite).getOrElse("")
    val email = ScraperUtils.get(html, rOrganizerEmail).getOrElse("")
    EventsEyeOrganizer(logo, name, address, phone, site, email)
  }

  private val contactWebsiteRegex = "<a href=\"([^\"]+)\" rel=\"nofollow\"".r.unanchored
  private val contactEmailRegex = "<a href=\"mailto:([^\"]+)\" title=\"".r.unanchored
  private val contactPhoneRegex = "<img src=\"/i/tel.gif\" alt=\"\" border=\"0\">&nbsp;([+ ()0-9]+)<br>".r.unanchored
  private def extractMore(html: String): (String, String, String) = {
    val website = html match {
      case contactWebsiteRegex(website) => notNull(website)
      case _ => ""
    }
    val email = html match {
      case contactEmailRegex(email) => notNull(email)
      case _ => ""
    }
    val phone = html match {
      case contactPhoneRegex(phone) => notNull(phone)
      case _ => ""
    }
    (website, email, phone)
  }

  private def notNull(str: String): String = if (str == null) "" else str
}
