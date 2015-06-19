package models

import common.Utils
import common.infrastructure.repository.Repository
import services.FileImporter
import org.joda.time.DateTime
import scala.util.Try
import play.api.data.Forms._
import play.api.libs.json.Json

case class Event(
  uuid: String,
  refreshUrl: Option[String], // a get on this url will scrape original data of this event (used to update program)
  name: String,
  description: String,
  logoUrl: String, // squared logo of event (~ 100x100)
  landingUrl: String, // landscape img for event (in info screen) (~ 400x150)
  siteUrl: String, // event home page
  start: Option[DateTime], // when event starts
  end: Option[DateTime], // when event ends
  address: Address,
  price: String, // event pricing (ex: "15€ - 50€" or "inscription obligatoire")
  priceUrl: String, // where to buy tickets
  twitterHashtag: Option[String],
  twitterAccount: Option[String],
  reportEmailMessageHtml: Option[String],
  tags: List[String],
  published: Boolean,
  source: Option[DataSource], // where the event were fetched (if applies)
  created: DateTime,
  updated: DateTime) extends EventItem {
  def toMap(): Map[String, String] = Event.toMap(this)
  def merge(e: Event): Event = Event(
    this.uuid,
    merge(this.refreshUrl, e.refreshUrl),
    merge(this.name, e.name),
    merge(this.description, e.description),
    merge(this.logoUrl, e.logoUrl),
    merge(this.landingUrl, e.landingUrl),
    merge(this.siteUrl, e.siteUrl),
    merge(this.start, e.start),
    merge(this.end, e.end),
    merge(this.address, e.address),
    merge(this.price, e.price),
    merge(this.priceUrl, e.priceUrl),
    merge(this.twitterHashtag, e.twitterHashtag),
    merge(this.twitterAccount, e.twitterAccount),
    merge(this.reportEmailMessageHtml, e.reportEmailMessageHtml),
    merge(this.tags, e.tags),
    merge(this.published, e.published),
    merge(this.source, e.source),
    this.created,
    e.updated)
  private def merge(s1: String, s2: String): String = if (s2.isEmpty) s1 else s2
  private def merge(b1: Boolean, b2: Boolean): Boolean = b1 || b2
  private def merge[A](d1: Option[A], d2: Option[A]): Option[A] = if (d2.isEmpty) d1 else d2
  private def merge[A](l1: List[A], l2: List[A]): List[A] = if (l2.isEmpty) l1 else l2
  private def merge(a1: Address, a2: Address): Address = if (a2.name.isEmpty && a2.street.isEmpty && a2.zipCode.isEmpty && a2.city.isEmpty) a1 else a2
}
object Event {
  val className = "events"
  implicit val format = Json.format[Event]
  private def parseDate(date: String) = Utils.parseDate(FileImporter.dateFormat)(date)
  def fromMap(d: Map[String, String]): Try[Event] =
    Try(Event(
      d.get("uuid").flatMap(u => if (u.isEmpty) None else Some(u)).getOrElse(Repository.generateUuid()),
      d.get("refreshUrl"),
      d.get("name").get,
      d.get("description").getOrElse(""),
      d.get("logoUrl").getOrElse(""),
      d.get("landingUrl").getOrElse(""),
      d.get("siteUrl").getOrElse(""),
      d.get("start").flatMap(d => parseDate(d)),
      d.get("end").flatMap(d => parseDate(d)),
      Address(
        d.get("address.name").getOrElse(""),
        d.get("address.street").getOrElse(""),
        d.get("address.zipCode").getOrElse(""),
        d.get("address.city").getOrElse("")),
      d.get("price").getOrElse(""),
      d.get("priceUrl").getOrElse(""),
      d.get("twitterHashtag"),
      d.get("twitterAccount"),
      d.get("reportEmailMessageHtml"),
      Utils.toList(d.get("tags").getOrElse("")),
      d.get("published").flatMap(s => if (s.isEmpty) None else Some(s.toBoolean)).getOrElse(false),
      d.get("source.url").map(url => DataSource(d.get("source.ref").getOrElse(""), url)),
      d.get("created").flatMap(d => parseDate(d)).getOrElse(new DateTime()),
      d.get("updated").flatMap(d => parseDate(d)).getOrElse(new DateTime())))

  def toMap(e: Event): Map[String, String] = Map(
    "uuid" -> e.uuid,
    "refreshUrl" -> e.refreshUrl.getOrElse(""),
    "name" -> e.name,
    "description" -> e.description,
    "logoUrl" -> e.logoUrl,
    "landingUrl" -> e.landingUrl,
    "siteUrl" -> e.siteUrl,
    "start" -> e.start.map(_.toString(FileImporter.dateFormat)).getOrElse(""),
    "end" -> e.end.map(_.toString(FileImporter.dateFormat)).getOrElse(""),
    "address.name" -> e.address.name,
    "address.street" -> e.address.street,
    "address.zipCode" -> e.address.zipCode,
    "address.city" -> e.address.city,
    "price" -> e.price,
    "priceUrl" -> e.priceUrl,
    "twitterHashtag" -> e.twitterHashtag.getOrElse(""),
    "twitterAccount" -> e.twitterAccount.getOrElse(""),
    "reportEmailMessageHtml" -> e.reportEmailMessageHtml.getOrElse(""),
    "tags" -> Utils.fromList(e.tags),
    "published" -> e.published.toString,
    "source.ref" -> e.source.map(_.ref).getOrElse(""),
    "source.url" -> e.source.map(_.url).getOrElse(""),
    "created" -> e.created.toString(FileImporter.dateFormat),
    "updated" -> e.updated.toString(FileImporter.dateFormat))
}

// mapping object for Event Form
case class EventData(
  refreshUrl: Option[String],
  name: String,
  description: String,
  logoUrl: String,
  landingUrl: String,
  siteUrl: String,
  start: Option[DateTime],
  end: Option[DateTime],
  address: Address,
  price: String,
  priceUrl: String,
  twitterHashtag: Option[String],
  twitterAccount: Option[String],
  reportEmailMessageHtml: Option[String],
  tags: String,
  published: Boolean)
object EventData {
  implicit val format = Json.format[EventData]
  val fields = mapping(
    "refreshUrl" -> optional(text),
    "name" -> nonEmptyText,
    "description" -> text,
    "logoUrl" -> text,
    "landingUrl" -> text,
    "siteUrl" -> text,
    "start" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
    "end" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
    "address" -> Address.fields,
    "price" -> text,
    "priceUrl" -> text,
    "twitterHashtag" -> optional(text),
    "twitterAccount" -> optional(text),
    "reportEmailMessageHtml" -> optional(text),
    "tags" -> text,
    "published" -> boolean)(EventData.apply)(EventData.unapply)

  def toModel(d: EventData): Event = Event(Repository.generateUuid(), d.refreshUrl, d.name, d.description, d.logoUrl, d.landingUrl, d.siteUrl, d.start, d.end, d.address, d.price, d.priceUrl, d.twitterHashtag.map(Utils.toTwitterHashtag), d.twitterAccount.map(Utils.toTwitterAccount), d.reportEmailMessageHtml, Utils.toList(d.tags), d.published, None, new DateTime(), new DateTime())
  def fromModel(d: Event): EventData = EventData(d.refreshUrl, d.name, d.description, d.logoUrl, d.landingUrl, d.siteUrl, d.start, d.end, d.address, d.price, d.priceUrl, d.twitterHashtag.map(Utils.toTwitterHashtag), d.twitterAccount.map(Utils.toTwitterAccount), d.reportEmailMessageHtml, Utils.fromList(d.tags), d.published)
  def merge(m: Event, d: EventData): Event = toModel(d).copy(uuid = m.uuid, source = m.source, created = m.created)
}
