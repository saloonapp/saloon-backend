package models

import common.Utils
import common.infrastructure.repository.Repository
import services.FileImporter
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json.Json

case class OldAddress(name: String)
object OldAddress {
  implicit val format = Json.format[OldAddress]
}
case class OldEvent(
  uuid: String,
  image: String,
  name: String,
  description: String,
  start: Option[DateTime],
  end: Option[DateTime],
  address: Option[OldAddress],
  twitterHashtag: Option[String],
  published: Boolean,
  created: DateTime,
  updated: DateTime) {
  def transform(): Event = Event(
    this.uuid,
    this.name,
    this.description,
    this.image,
    this.image,
    "",
    this.start,
    this.end,
    this.address.map(a => Address("", a.name, "", "")).getOrElse(Address("", "", "", "")),
    "",
    "",
    this.twitterHashtag,
    None,
    List(),
    this.published,
    None,
    this.created,
    this.updated)
}
object OldEvent {
  implicit val format = Json.format[OldEvent]
}

case class Event(
  uuid: String,
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
  tags: List[String],
  published: Boolean,
  source: Option[DataSource], // where the event were fetched (if applies)
  created: DateTime,
  updated: DateTime) extends EventItem {
  def toMap(): Map[String, String] = Event.toMap(this)
}
object Event {
  implicit val format = Json.format[Event]
  def fromMap(d: Map[String, String]): Option[Event] =
    if (d.get("name").isDefined) {
      Some(Event(
        d.get("uuid").getOrElse(Repository.generateUuid()),
        d.get("name").get,
        d.get("description").getOrElse(""),
        d.get("logoUrl").getOrElse(""),
        d.get("landingUrl").getOrElse(""),
        d.get("siteUrl").getOrElse(""),
        d.get("start").map(d => DateTime.parse(d, FileImporter.dateFormat)),
        d.get("end").map(d => DateTime.parse(d, FileImporter.dateFormat)),
        Address(
          d.get("address.name").getOrElse(""),
          d.get("address.street").getOrElse(""),
          d.get("address.zipCode").getOrElse(""),
          d.get("address.city").getOrElse("")),
        d.get("price").getOrElse(""),
        d.get("priceUrl").getOrElse(""),
        d.get("twitterHashtag"),
        d.get("twitterAccount"),
        Utils.toList(d.get("tags").getOrElse("")),
        d.get("published").map(_.toBoolean).getOrElse(false),
        d.get("source.url").map(url => DataSource(d.get("source.ref").getOrElse(""), url)),
        d.get("created").map(d => DateTime.parse(d, FileImporter.dateFormat)).getOrElse(new DateTime()),
        d.get("updated").map(d => DateTime.parse(d, FileImporter.dateFormat)).getOrElse(new DateTime())))
    } else {
      None
    }
  def toMap(e: Event): Map[String, String] = Map(
    "uuid" -> e.uuid,
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
    "tags" -> Utils.fromList(e.tags),
    "published" -> e.published.toString,
    "source.ref" -> e.source.map(_.ref).getOrElse(""),
    "source.url" -> e.source.map(_.url).getOrElse(""),
    "created" -> e.created.toString(FileImporter.dateFormat),
    "updated" -> e.updated.toString(FileImporter.dateFormat))
}

// model sent to client : no field source / add field className, sessionCount & exponentCount
case class EventUI(
  uuid: String,
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
  tags: List[String],
  published: Boolean,
  created: DateTime,
  updated: DateTime,
  sessionCount: Int,
  exponentCount: Int,
  className: String = EventUI.className)
object EventUI {
  val className = "events"
  implicit val format = Json.format[EventUI]
  // def toModel(d: EventUI): Event = Event(d.uuid, d.name, d.description, d.logoUrl, d.landingUrl, d.siteUrl, d.start, d.end, d.address, d.price, d.priceUrl, d.twitterHashtag, d.twitterAccount, d.tags, d.published, None, d.created, d.updated)
  def fromModel(d: Event, sessionCount: Int, exponentCount: Int): EventUI = EventUI(d.uuid, d.name, d.description, d.logoUrl, d.landingUrl, d.siteUrl, d.start, d.end, d.address, d.price, d.priceUrl, d.twitterHashtag, d.twitterAccount, d.tags, d.published, d.created, d.updated, sessionCount, exponentCount)
}

// mapping object for Event Form
case class EventData(
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
  tags: String,
  published: Boolean)
object EventData {
  implicit val format = Json.format[EventData]
  val fields = mapping(
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
    "tags" -> text,
    "published" -> boolean)(EventData.apply)(EventData.unapply)

  def toModel(d: EventData): Event = Event(Repository.generateUuid(), d.name, d.description, d.logoUrl, d.landingUrl, d.siteUrl, d.start, d.end, d.address, d.price, d.priceUrl, d.twitterHashtag.map(Utils.toTwitterHashtag), d.twitterAccount.map(Utils.toTwitterAccount), Utils.toList(d.tags), d.published, None, new DateTime(), new DateTime())
  def fromModel(d: Event): EventData = EventData(d.name, d.description, d.logoUrl, d.landingUrl, d.siteUrl, d.start, d.end, d.address, d.price, d.priceUrl, d.twitterHashtag.map(Utils.toTwitterHashtag), d.twitterAccount.map(Utils.toTwitterAccount), Utils.fromList(d.tags), d.published)
  def merge(m: Event, d: EventData): Event = toModel(d).copy(uuid = m.uuid, source = m.source, created = m.created)
}
