package tools.scrapers.eventseye

import tools.utils.Scraper
import tools.scrapers.eventseye.models.EventsEyeEvent
import tools.scrapers.eventseye.models.EventsEyeAttendance
import tools.scrapers.eventseye.models.EventsEyeOrganizer
import tools.scrapers.eventseye.models.EventsEyeAddress
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

    val decription = descriptionSection.select("tr:eq(2) td:eq(2)").text()
    val audience = descriptionSection.select("tr:eq(2) td:eq(4)").text()
    val cycle = descriptionSection.select("tr:eq(2) td:eq(6)").text()

    val nextDates = datesSection.select("tr:eq(2) td:eq(0) table tr").map { row => extractDates(row.select("td:eq(0) span").text()) }.toList

    val venueInfo = venueSection.select("tr:eq(0) td:eq(0) table tr:eq(0) td:eq(1)")
    val venue = venueInfo.select(".etb").text()

    val orgas = orgaSection.select("td table").map { orga => extractOrganizer(orga.select("td:eq(1)").html()) }.toList

    val attendance = attendanceSectionOpt.map { attendanceSection =>
      val year = attendanceSection.select("td:eq(0) b").text().replace(" ", "")
      val exponents = attendanceSection.select("td:eq(1) b").text().replace(" ", "")
      val visitors = attendanceSection.select("td:eq(2) b").text().replace(" ", "")
      val exhibitionSpace = attendanceSection.select("td:eq(3) b").text().replace(" ", "")
      EventsEyeAttendance(year, exponents, visitors, exhibitionSpace)
    }

    val more = moreSection.select("tr:eq(2) td:eq(0) a").map { a => a.attr("href").replace("mailto:", "") }.toList
    val (website, email, phone) = extractMore(moreSection.select("tr:eq(2) td:eq(0)").html())

    EventsEyeEvent(logo, name, decription, audience, cycle, nextDates.headOption.getOrElse(""), nextDates.drop(1), venue, orgas, attendance, website, email, phone, pageUrl)
  }

  private val dateRegex1 = "([a-zA-Z.]+) ([0-9]+) - (?:[a-zA-Z.]+ )?([0-9]+), ([0-9]+)".r.unanchored
  private val dateRegex2 = "on ([a-zA-Z.]+) ([0-9]+), ([0-9]+)".r.unanchored
  private val dateRegex3 = "on ([a-zA-Z.]+) ([0-9]+) \\(\\?\\)".r.unanchored
  val parseDate = DateTimeFormat.forPattern("dd MMMM yyyy").withLocale(Locale.ENGLISH)
  val formatDate = DateTimeFormat.forPattern("dd/MM/yyyy").withLocale(Locale.FRENCH)
  private def extractDates(date: String): String = date match {
    case dateRegex1(month, dayStart, dayEnd, year) => parseDate.parseDateTime(s"$dayStart ${monthReplace(month)} $year").toString(formatDate)
    case dateRegex2(month, day, year) => parseDate.parseDateTime(s"$day ${monthReplace(month)} $year").toString(formatDate)
    case dateRegex3(month, year) => parseDate.parseDateTime(s"01 ${monthReplace(month)} $year").toString(formatDate)
    case _ => date
  }
  private def monthReplace(month: String): String = month match {
    case "Jan." => "January"
    case "Feb." => "February"
    case "Sept." => "September"
    case "Oct." => "October"
    case "Nov." => "November"
    case "Dec." => "December"
    case _ => month
  }

  private val rOrgaName = "<a [^<>]+>(.*?)</a>\\s+"
  private val rOrgaAddress = "<br>(?:(.*?)<br>\\s)?(?:(.*?)<br>\\s)?(?:(.*?)<br>\\s)?(?:(.*?)\\s)?<br><b>(.*?)</b>\\s+"
  private val rOrgaPhone = "(?:<br><img src=\"/i/tel.gif\" border=\"0\" alt=\"\">&nbsp;([+ ()0-9]+)\\s)?"
  private val rOrgaWebsite = "<a href=\"([^\"]+)\""
  private val rOrgaEmail = "(?:<a href=\"mailto:([^\"]+)\")?"
  private val orgaRegex = (rOrgaName + rOrgaAddress + rOrgaPhone + ".*?" + rOrgaWebsite + ".*?" + rOrgaEmail).r.unanchored
  private def extractOrganizer(html: String): EventsEyeOrganizer = html match {
    case orgaRegex(name, addressName, addressComplement, addressStreet, addressCity, country, phone, site, email) =>
      EventsEyeOrganizer(Jsoup.parse(name).text(), EventsEyeAddress(notNull(addressName), notNull(addressComplement), notNull(addressStreet), notNull(addressCity), notNull(country)), notNull(phone), notNull(site), notNull(email))
    case _ => EventsEyeOrganizer(html, EventsEyeAddress("", "", "", "", ""), "", "", "")
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
