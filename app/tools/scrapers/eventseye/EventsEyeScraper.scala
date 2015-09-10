package tools.scrapers.eventseye

import tools.utils.Scraper
import tools.scrapers.eventseye.models.EventsEyeEvent
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
    val moreSection = sections(8)

    val logo = baseUrl + headerSection.select("tr:eq(0) td:eq(0) img").attr("src")
    val name = headerSection.select("tr:eq(1) td:eq(0) h1").text()

    val decription = descriptionSection.select("tr:eq(2) td:eq(2)").text()
    val audience = descriptionSection.select("tr:eq(2) td:eq(4)").text()
    val cycle = descriptionSection.select("tr:eq(2) td:eq(6)").text()

    val nextDates = datesSection.select("tr:eq(2) td:eq(0) table tr").map { row => extractDates(row.select("td:eq(0) span").text()) }.toList

    val venueInfo = venueSection.select("tr:eq(0) td:eq(0) table tr:eq(0) td:eq(1)")
    val venue = venueInfo.select(".etb").text()

    val orgas = orgaSection.select("td table").map { orga => extractOrganizer(orga.select("td:eq(1)").html()) }.toList

    val more = moreSection.select("tr:eq(2) td:eq(0) a").map { a => a.attr("href").replace("mailto:", "") }.toList
    val (website, email) = extractMore(moreSection.select("tr:eq(2) td:eq(0)").html())

    EventsEyeEvent(logo, name, decription, audience, cycle, nextDates.headOption.getOrElse(""), nextDates.drop(1), venue, orgas, website, email, pageUrl)
  }

  private val datePeriodRegex = "([a-zA-Z.]+) ([0-9]+) - (?:[a-zA-Z.]+ )?([0-9]+), ([0-9]+)".r.unanchored
  private val dateUnknownRegex = "on ([a-zA-Z.]+) ([0-9]+) \\(\\?\\)".r.unanchored
  val parseDate = DateTimeFormat.forPattern("dd MMMM yyyy").withLocale(Locale.ENGLISH)
  val formatDate = DateTimeFormat.forPattern("dd/MM/yyyy").withLocale(Locale.FRENCH)
  private def extractDates(date: String): String = date match {
    case datePeriodRegex(month, dayStart, dayEnd, year) => parseDate.parseDateTime(s"$dayStart ${monthReplace(month)} $year").toString(formatDate)
    case dateUnknownRegex(month, year) => parseDate.parseDateTime(s"01 ${monthReplace(month)} $year").toString(formatDate)
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
  private val rOrgaAddress = "<br>(?:(.*?)<br>\\s)?(?:(.*?)<br>\\s)?(.*?)<br>\\s(.*?)\\s<br><b>(.*?)</b>\\s+"
  private val rOrgaPhone = "(?:<br><img src=\"/i/tel.gif\" border=\"0\" alt=\"\">&nbsp;([+ ()0-9]+)\\s)?"
  private val rOrgaWebsite = "<a href=\"([^\"]+)\""
  private val rOrgaEmail = "<a href=\"mailto:([^\"]+)\""
  private val orgaRegex = (rOrgaName + rOrgaAddress + rOrgaPhone + ".*?" + rOrgaWebsite + ".*?" + rOrgaEmail).r.unanchored
  private def extractOrganizer(html: String): EventsEyeOrganizer = html match {
    case orgaRegex(name, addressName, addressComplement, addressStreet, addressCity, country, phone, site, email) =>
      EventsEyeOrganizer(Jsoup.parse(name).text(), EventsEyeAddress(addressName, addressComplement, addressStreet, addressCity, country), phone, site, email)
    case _ => EventsEyeOrganizer(html, EventsEyeAddress("", "", "", "", ""), "", "", "")
  }

  private val contactRegex = "<a href=\"([^\"]+)\".*?<a href=\"mailto:([^\"]+)\"".r.unanchored
  private def extractMore(html: String): (String, String) = html match {
    case contactRegex(website, email) => (website, email)
    case _ => ("", "")
  }

}
