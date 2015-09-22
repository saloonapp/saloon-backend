package tools.scrapers.lanyrd.models

import common.models.values.Source
import common.models.event.GenericEvent
import common.models.event.GenericEventVenue
import common.models.event.GenericEventAddress
import common.models.event.GenericEventStats
import play.api.libs.json.Json
import org.joda.time.DateTime
import org.jsoup.Jsoup

case class LanyrdAddress(
  name: String,
  street: String,
  city: String,
  country: String,
  url: String,
  gmap: String) {
  def toGeneric: GenericEventAddress = GenericEventAddress(this.name, "", this.street, "", this.city, this.country)
}
object LanyrdAddress {
  implicit val format = Json.format[LanyrdAddress]
}
case class LanyrdEvent(
  name: String,
  descriptionHTML: String,
  start: Option[DateTime],
  end: Option[DateTime],
  websiteUrl: String,
  scheduleUrl: String,
  address: LanyrdAddress,
  twitterAccountUrl: String,
  twitterHashtagUrl: String,
  tags: List[String],
  url: String)
object LanyrdEvent {
  implicit val format = Json.format[LanyrdEvent]

  val sourceName = "LanyrdScraper"
  def toGenericEvent(event: LanyrdEvent): GenericEvent = {
    GenericEvent(
      List(Source(getRef(event.url), sourceName, event.url)),
      "", // logo
      event.name,
      event.start,
      event.end,
      Jsoup.parse(event.descriptionHTML).text(), // decription
      event.descriptionHTML, // decriptionHTML
      Some(GenericEventVenue("", "", event.address.toGeneric, "", "", "")),
      List(), // orgas
      event.websiteUrl, // website
      "", // email
      "", // phone
      event.tags, // tags
      Map(
        GenericEvent.Social.twitterAccount -> event.twitterAccountUrl,
        GenericEvent.Social.twitterHashtag -> event.twitterHashtagUrl).filter(_._2 != ""), // socialUrls
      GenericEventStats(None, None, None, None, None),
      GenericEvent.Status.draft,
      List(), // attendees
      List(), // exponents
      List(), // sessions
      Map(), // exponentTeam
      Map()) // sessionSpeakers
  }

  private val refRegex = "http://lanyrd.com/([0-9]+)/([^/]+)/".r
  private def getRef(url: String): String = url match {
    case refRegex(year, ref) => s"$ref-$year"
    case _ => url
  }
}
