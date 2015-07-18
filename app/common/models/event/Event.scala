package common.models.event

import common.Utils
import common.models.values.Address
import common.models.values.DataSource
import common.models.values.Link
import common.services.FileImporter
import common.repositories.Repository
import org.joda.time.DateTime
import scala.util.Try
import play.api.data.Forms._
import play.api.libs.json.Json
import org.jsoup.Jsoup

case class EventImages(
  logo: String, // squared logo of event (~ 100x100)
  landing: String) // landscape img for event (in info screen) (~ 400x150)
case class EventInfoSocialTwitter(
  hashtag: Option[String],
  account: Option[String])
case class EventInfoSocial(
  twitter: EventInfoSocialTwitter)
case class EventInfo(
  website: String, // event home page
  start: Option[DateTime], // when event starts
  end: Option[DateTime], // when event ends
  address: Address,
  price: Link, // event pricing (ex: "15€ - 50€" or "inscription obligatoire")
  social: EventInfoSocial)
case class EventEmail(
  reportMessageHtml: Option[String])
case class EventConfigBranding(
  primaryColor: String,
  secondaryColor: String,
  dailySessionMenu: List[String],
  exponentMenu: List[String])
case class EventConfigAttendeeSurveyQuestion(
  question: String,
  multiple: Boolean,
  required: Boolean,
  otherAllowed: Boolean,
  answers: List[String])
case class EventConfigAttendeeSurvey(
  fields: List[String],
  questions: List[EventConfigAttendeeSurveyQuestion])
case class EventConfig(
  branding: Option[EventConfigBranding],
  options: Map[String, Boolean],
  attendeeSurvey: Option[EventConfigAttendeeSurvey],
  published: Boolean) {
  def hasTicketing(): Boolean = hasOption("ticketing")
  def hasCVTheque(): Boolean = hasOption("cvtheque")
  def hasScanQRCode(): Boolean = hasOption("scanqrcode")
  private def hasOption(option: String): Boolean = this.options.get(option).getOrElse(false)
}
case class EventMeta(
  categories: List[String],
  refreshUrl: Option[String], // a get on this url will scrape original data of this event (used to update program)
  source: Option[DataSource], // where the event were fetched (if applies)
  created: DateTime,
  updated: DateTime)
case class Event(
  uuid: String,
  ownerId: String, // Organization uuid
  name: String,
  description: String,
  descriptionHTML: String,
  images: EventImages,
  info: EventInfo,
  email: EventEmail,
  config: EventConfig,
  meta: EventMeta) extends EventItem {
  def merge(e: Event): Event = Event.merge(this, e)
  //def toMap(): Map[String, String] = Event.toMap(this)
}
object Event {
  val className = "events"
  implicit val formatEventImages = Json.format[EventImages]
  implicit val formatEventInfoSocialTwitter = Json.format[EventInfoSocialTwitter]
  implicit val formatEventInfoSocial = Json.format[EventInfoSocial]
  implicit val formatEventInfo = Json.format[EventInfo]
  implicit val formatEventEmail = Json.format[EventEmail]
  implicit val formatEventConfigBranding = Json.format[EventConfigBranding]
  implicit val formatEventConfigAttendeeSurveyQuestion = Json.format[EventConfigAttendeeSurveyQuestion]
  implicit val formatEventConfigAttendeeSurvey = Json.format[EventConfigAttendeeSurvey]
  implicit val formatEventConfig = Json.format[EventConfig]
  implicit val formatEventMeta = Json.format[EventMeta]
  implicit val format = Json.format[Event]

  def merge(e1: Event, e2: Event): Event = Event(
    e1.uuid,
    e1.ownerId,
    merge(e1.name, e2.name),
    merge(e1.description, e2.description),
    merge(e1.descriptionHTML, e2.descriptionHTML),
    merge(e1.images, e2.images),
    merge(e1.info, e2.info),
    merge(e1.email, e2.email),
    merge(e1.config, e2.config),
    merge(e1.meta, e2.meta))
  private def merge(e1: EventImages, e2: EventImages): EventImages = EventImages(
    merge(e1.logo, e2.logo),
    merge(e1.landing, e2.landing))
  private def merge(e1: EventInfo, e2: EventInfo): EventInfo = EventInfo(
    merge(e1.website, e2.website),
    merge(e1.start, e2.start),
    merge(e1.end, e2.end),
    merge(e1.address, e2.address),
    merge(e1.price, e2.price),
    merge(e1.social, e2.social))
  private def merge(e1: EventInfoSocial, e2: EventInfoSocial): EventInfoSocial = EventInfoSocial(
    merge(e1.twitter, e2.twitter))
  private def merge(e1: EventInfoSocialTwitter, e2: EventInfoSocialTwitter): EventInfoSocialTwitter = EventInfoSocialTwitter(
    merge(e1.hashtag, e2.hashtag),
    merge(e1.account, e2.account))
  private def merge(e1: EventEmail, e2: EventEmail): EventEmail = EventEmail(
    merge(e1.reportMessageHtml, e2.reportMessageHtml))
  private def merge(e1: EventConfig, e2: EventConfig): EventConfig = EventConfig(
    merge(e1.branding, e2.branding),
    merge(e1.options, e2.options),
    merge(e1.attendeeSurvey, e2.attendeeSurvey),
    e2.published)
  private def merge(e1: EventMeta, e2: EventMeta): EventMeta = EventMeta(
    merge(e1.categories, e2.categories),
    merge(e1.refreshUrl, e2.refreshUrl),
    merge(e1.source, e2.source),
    e1.created,
    e2.updated)
  private def merge(e1: Link, e2: Link): Link = if (e2.label.isEmpty && e2.url.isEmpty) e1 else e2
  private def merge(e1: Address, e2: Address): Address = if (e2.name.isEmpty && e2.street.isEmpty && e2.zipCode.isEmpty && e2.city.isEmpty) e1 else e2
  private def merge(e1: String, e2: String): String = if (e2.isEmpty) e1 else e2
  private def merge[A](e1: Option[A], e2: Option[A]): Option[A] = if (e2.isEmpty) e1 else e2
  private def merge[A](e1: List[A], e2: List[A]): List[A] = if (e2.isEmpty) e1 else e2
  private def merge[A, B](e1: Map[A, B], e2: Map[A, B]): Map[A, B] = if (e2.isEmpty) e1 else e2

  /*def fromMap(d: Map[String, String]): Try[Event] =
    Try(Event(
      d.get("uuid").flatMap(u => if (u.isEmpty) None else Some(u)).getOrElse(Repository.generateUuid()),
      d.get("name").get,
      d.get("description").getOrElse(""),
      EventImages(
        d.get("images.logo").getOrElse(""),
        d.get("images.landing").getOrElse("")),
      EventInfo(
        d.get("info.website").getOrElse(""),
        d.get("info.start").flatMap(d => parseDate(d)),
        d.get("info.end").flatMap(d => parseDate(d)),
        Address(
          d.get("info.address.name").getOrElse(""),
          d.get("info.address.street").getOrElse(""),
          d.get("info.address.zipCode").getOrElse(""),
          d.get("info.address.city").getOrElse("")),
        Link(
          d.get("info.price.label").getOrElse(""),
          d.get("info.price.url").getOrElse("")),
        EventInfoSocial(
          EventInfoSocialTwitter(
            d.get("info.social.twitter.hashtag"),
            d.get("info.social.twitter.account")))),
      EventEmail(
        d.get("email.reportMessageHtml")),
      EventConfig(
        d.get("config.branding.primaryColor").map { primaryColor =>
          EventConfigBranding(
            primaryColor,
            d.get("config.branding.secondaryColor").getOrElse(""),
            Utils.toList(d.get("config.branding.dailySessionMenu").getOrElse("")),
            Utils.toList(d.get("config.branding.exponentMenu").getOrElse("")))
        },
        d.get("config.published").flatMap(s => if (s.isEmpty) None else Some(s.toBoolean)).getOrElse(false)),
      EventMeta(
        Utils.toList(d.get("meta.categories").getOrElse("")),
        d.get("meta.refreshUrl"),
        d.get("meta.source.ref").map { ref => DataSource(ref, d.get("meta.source.name").getOrElse(""), d.get("meta.source.url").getOrElse("")) },
        d.get("meta.created").flatMap(d => parseDate(d)).getOrElse(new DateTime()),
        d.get("meta.updated").flatMap(d => parseDate(d)).getOrElse(new DateTime()))))

  def toMap(e: Event): Map[String, String] = Map(
    "uuid" -> e.uuid,
    "name" -> e.name,
    "description" -> e.description,
    "images.logo" -> e.images.logo,
    "images.landing" -> e.images.landing,
    "info.website" -> e.info.website,
    "info.start" -> e.info.start.map(_.toString(FileImporter.dateFormat)).getOrElse(""),
    "info.end" -> e.info.end.map(_.toString(FileImporter.dateFormat)).getOrElse(""),
    "info.address.name" -> e.info.address.name,
    "info.address.street" -> e.info.address.street,
    "info.address.zipCode" -> e.info.address.zipCode,
    "info.address.city" -> e.info.address.city,
    "info.price.label" -> e.info.price.label,
    "info.price.url" -> e.info.price.url,
    "info.social.twitter.hashtag" -> e.info.social.twitter.hashtag.getOrElse(""),
    "info.social.twitter.account" -> e.info.social.twitter.account.getOrElse(""),
    "email.reportMessageHtml" -> e.email.reportMessageHtml.getOrElse(""),
    "config.branding.primaryColor" -> e.config.branding.map(_.primaryColor).getOrElse(""),
    "config.branding.secondaryColor" -> e.config.branding.map(_.secondaryColor).getOrElse(""),
    "config.branding.dailySessionMenu" -> e.config.branding.map(b => Utils.fromList(b.dailySessionMenu)).getOrElse(""),
    "config.branding.exponentMenu" -> e.config.branding.map(b => Utils.fromList(b.exponentMenu)).getOrElse(""),
    "config.published" -> e.config.published.toString,
    "meta.categories" -> Utils.fromList(e.meta.categories),
    "meta.refreshUrl" -> e.meta.refreshUrl.getOrElse(""),
    "meta.source.ref" -> e.meta.source.map(_.ref).getOrElse(""),
    "meta.source.name" -> e.meta.source.map(_.name).getOrElse(""),
    "meta.source.url" -> e.meta.source.map(_.url).getOrElse(""),
    "meta.created" -> e.meta.created.toString(FileImporter.dateFormat),
    "meta.updated" -> e.meta.updated.toString(FileImporter.dateFormat))

  private def parseDate(date: String) = Utils.parseDate(FileImporter.dateFormat)(date)*/
}

// mapping object for Event Form
case class EventConfigBrandingData(
  primaryColor: String,
  secondaryColor: String,
  dailySessionMenu: String,
  exponentMenu: String)
case class EventConfigData(
  branding: Option[EventConfigBrandingData],
  published: Boolean)
case class EventMetaData(
  categories: List[String],
  refreshUrl: Option[String],
  source: Option[DataSource])
case class EventData(
  ownerId: String,
  name: String,
  description: String,
  descriptionHTML: String,
  images: EventImages,
  info: EventInfo,
  email: EventEmail,
  config: EventConfigData,
  meta: EventMetaData)
object EventData {
  val fields = mapping(
    "ownerId" -> nonEmptyText,
    "name" -> nonEmptyText,
    "description" -> text,
    "descriptionHTML" -> text,
    "images" -> mapping(
      "logo" -> text,
      "landing" -> text)(EventImages.apply)(EventImages.unapply),
    "info" -> mapping(
      "website" -> text,
      "start" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
      "end" -> optional(jodaDate(pattern = "dd/MM/yyyy HH:mm")),
      "address" -> Address.fields,
      "price" -> Link.fields,
      "social" -> mapping(
        "twitter" -> mapping(
          "hashtag" -> optional(text),
          "account" -> optional(text))(EventInfoSocialTwitter.apply)(EventInfoSocialTwitter.unapply))(EventInfoSocial.apply)(EventInfoSocial.unapply))(EventInfo.apply)(EventInfo.unapply),
    "email" -> mapping(
      "reportMessageHtml" -> optional(text))(EventEmail.apply)(EventEmail.unapply),
    "config" -> mapping(
      "branding" -> optional(mapping(
        "primaryColor" -> text,
        "secondaryColor" -> text,
        "dailySessionMenu" -> text,
        "exponentMenu" -> text)(EventConfigBrandingData.apply)(EventConfigBrandingData.unapply)),
      "published" -> boolean)(EventConfigData.apply)(EventConfigData.unapply),
    "meta" -> mapping(
      "categories" -> list(text),
      "refreshUrl" -> optional(text),
      "source" -> optional(DataSource.fields))(EventMetaData.apply)(EventMetaData.unapply))(EventData.apply)(EventData.unapply)

  def toModel(d: EventInfoSocialTwitter): EventInfoSocialTwitter = EventInfoSocialTwitter(d.hashtag.map(Utils.toTwitterHashtag), d.account.map(Utils.toTwitterAccount))
  def toModel(d: EventInfoSocial): EventInfoSocial = d.copy(twitter = toModel(d.twitter))
  def toModel(d: EventInfo): EventInfo = d.copy(social = toModel(d.social))
  def toModel(d: EventConfigBrandingData): EventConfigBranding = EventConfigBranding(d.primaryColor, d.secondaryColor, Utils.toList(d.dailySessionMenu), Utils.toList(d.exponentMenu))
  def toModel(d: EventConfigData): EventConfig = EventConfig(d.branding.map(b => toModel(b)), Map(), None, d.published)
  def toModel(d: EventMetaData): EventMeta = EventMeta(d.categories, d.refreshUrl, d.source, new DateTime(), new DateTime())
  def toModel(d: EventData): Event = Event(Repository.generateUuid(), d.ownerId, d.name, d.description, d.descriptionHTML, d.images, toModel(d.info), d.email, toModel(d.config), toModel(d.meta))
  def fromModel(d: EventConfigBranding): EventConfigBrandingData = EventConfigBrandingData(d.primaryColor, d.secondaryColor, Utils.fromList(d.dailySessionMenu), Utils.fromList(d.exponentMenu))
  def fromModel(d: EventConfig): EventConfigData = EventConfigData(d.branding.map(b => fromModel(b)), d.published)
  def fromModel(d: EventMeta): EventMetaData = EventMetaData(d.categories, d.refreshUrl, d.source)
  def fromModel(d: Event): EventData = EventData(d.ownerId, d.name, d.description, d.descriptionHTML, d.images, d.info, d.email, fromModel(d.config), fromModel(d.meta))
  def merge(m: EventMeta, d: EventMetaData): EventMeta = toModel(d).copy(source = m.source, created = m.created)
  def merge(m: Event, d: EventData): Event = toModel(d).copy(uuid = m.uuid, ownerId = m.ownerId, meta = merge(m.meta, d.meta))
}
