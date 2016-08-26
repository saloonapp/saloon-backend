package conferences.models

import common.Config
import common.models.utils.{Forms, DateRange, tStringHelper, tString}
import common.models.values.{CalendarEvent, GMapMarker, GMapPlace, UUID}
import common.services.{TwitterSrv, TwitterCard}
import common.views.format.Format
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.i18n.Lang
import play.api.libs.json.Json

case class ConferenceId(id: String) extends AnyVal with tString with UUID {
  def unwrap: String = this.id
}
object ConferenceId extends tStringHelper[ConferenceId] {
  def generate(): ConferenceId = ConferenceId(UUID.generate())
  def build(str: String): Either[String, ConferenceId] = UUID.toUUID(str).right.map(id => ConferenceId(id)).left.map(_ + " for ConferenceId")
}

case class Conference(
  id: ConferenceId,
  name: String,
  logo: Option[String],
  description: Option[String],
  start: DateTime,
  end: DateTime,
  siteUrl: String,
  videosUrl: Option[String],
  tags: List[String],
  location: Option[GMapPlace],
  cfp: Option[ConferenceCfp],
  tickets: Option[ConferenceTickets],
  social: Option[ConferenceSocial],
  created: DateTime,
  createdBy: Option[User]) {
  lazy val twitterAccount: Option[String] = social.flatMap(_.twitter.flatMap(_.account))
  lazy val twitterHashtag: Option[String] = social.flatMap(_.twitter.flatMap(_.hashtag))
  def toTwitterCard(): TwitterCard = TwitterCard(
    "summary",
    "@conferencelist_",
    name+", le " + start.toString(Config.Application.dateFormatter) + location.flatMap(_.locality).map(" à "+_).getOrElse(""),
    List(
      cfp.flatMap(c => if(c.end.isAfterNow) Some("CFP ouvert jusqu'au "+c.end.toString(Config.Application.dateFormatter)) else None),
      tickets.flatMap(t => if(end.isAfterNow && t.from.isDefined && t.currency.isDefined) Some("Billets à partir de "+t.from.get+" "+t.currency.get) else None),
      Some(tags.map("#"+_).mkString(" ")),
      description
    ).flatten.mkString(" - "),
    // proxy images with cloudinary to make twitter card always show it
    "http://res.cloudinary.com/demo/image/fetch/"+logo.getOrElse("https://avatars2.githubusercontent.com/u/11368266?v=3&s=200")
  )
  def toTwitt(): String = "" // TODO : prefilled text to twitt about this conf
  def toMarker()(implicit lang: Lang): Option[GMapMarker] = location.map { l => {
      GMapMarker(
        title = name,
        date = Format.period(Some(start), Some(end)),
        location = l.formatted,
        lat = l.geo.lat,
        lng = l.geo.lng,
        url = conferences.controllers.routes.Conferences.detail(id).url)
    }}
  def toCalendar(): CalendarEvent = CalendarEvent(
    title = name,
    start = start,
    end = end,
    url = conferences.controllers.routes.Conferences.detail(id).url)
}
case class ConferenceCfp(
  siteUrl: String,
  end: DateTime)
case class ConferenceTickets(
  siteUrl: Option[String],
  from: Option[Int],
  to: Option[Int],
  currency: Option[String]) {
  lazy val price: Option[String] =
    from.orElse(to).map { d =>
      val prices = List(from, to).flatten
      if(prices.sum == 0){ "Gratuit" } else { prices.mkString("", " - ", currency.map(" "+_).getOrElse("")) }
    }
}
case class ConferenceSocial(
  twitter: Option[ConferenceSocialTwitter]) {
  def trim(): ConferenceSocial = this.copy(
    twitter = this.twitter.map(_.trim())
  )
}
case class ConferenceSocialTwitter(
  account: Option[String],
  hashtag: Option[String]) {
  def trim(): ConferenceSocialTwitter = this.copy(
    account = this.account.map(t => TwitterSrv.toAccount(t)),
    hashtag = this.hashtag.map(t => TwitterSrv.toHashtag(t))
  )
}
object Conference {
  implicit val formatConferenceSocialTwitter = Json.format[ConferenceSocialTwitter]
  implicit val formatConferenceSocial = Json.format[ConferenceSocial]
  implicit val formatConferenceTickets = Json.format[ConferenceTickets]
  implicit val formatConferenceCfp = Json.format[ConferenceCfp]
  implicit val format = Json.format[Conference]
}

case class ConferenceData(
  id: Option[ConferenceId],
  name: String,
  logo: Option[String],
  description: Option[String],
  dates: DateRange,
  siteUrl: String,
  videosUrl: Option[String],
  tags: List[String],
  location: Option[GMapPlace],
  cfp: Option[ConferenceCfp],
  tickets: Option[ConferenceTickets],
  social: Option[ConferenceSocial],
  createdBy: User)
object ConferenceData {
  implicit val formatConferenceSocialTwitter = Json.format[ConferenceSocialTwitter]
  implicit val formatConferenceSocial = Json.format[ConferenceSocial]
  implicit val formatConferenceTickets = Json.format[ConferenceTickets]
  implicit val formatConferenceCfp = Json.format[ConferenceCfp]
  implicit val format = Json.format[ConferenceData]
  val fields = mapping(
    "id" -> optional(of[ConferenceId]),
    "name" -> nonEmptyText,
    "logo" -> optional(nonEmptyText),
    "description" -> optional(nonEmptyText),
    "dates" -> DateRange.mapping.verifying(DateRange.Constraints.required),
    "siteUrl" -> nonEmptyText,
    "videosUrl" -> optional(nonEmptyText),
    "tags" -> list(nonEmptyText).verifying(Forms.Constraints.required),
    "location" -> optional(GMapPlace.fields),
    "cfp" -> optional(mapping(
      "siteUrl" -> nonEmptyText,
      "end" -> jodaDate(pattern = "dd/MM/yyyy")
    )(ConferenceCfp.apply)(ConferenceCfp.unapply)),
    "tickets" -> optional(mapping(
      "siteUrl" -> optional(nonEmptyText),
      "from" -> optional(number),
      "to" -> optional(number),
      "currency" -> optional(nonEmptyText)
    )(ConferenceTickets.apply)(ConferenceTickets.unapply)),
    "social" -> optional(mapping(
      "twitter" -> optional(mapping(
        "account" -> optional(nonEmptyText),
        "hashtag" -> optional(nonEmptyText)
      )(ConferenceSocialTwitter.apply)(ConferenceSocialTwitter.unapply))
    )(ConferenceSocial.apply)(ConferenceSocial.unapply)),
    "createdBy" -> User.fields
  )(ConferenceData.apply)(ConferenceData.unapply)
  def toModel(d: ConferenceData): Conference = Conference(
    d.id.getOrElse(ConferenceId.generate()),
    d.name,
    d.logo,
    d.description,
    d.dates.start,
    d.dates.end,
    d.siteUrl,
    d.videosUrl,
    d.tags.map(_.trim.toLowerCase).filter(_.length > 0).sorted,
    d.location,
    d.cfp,
    d.tickets.filter(t => t.siteUrl.isDefined || t.from.isDefined),
    d.social.map(_.trim()),
    new DateTime(),
    Some(d.createdBy.trim()))
  def fromModel(m: Conference): ConferenceData = ConferenceData(
    Some(m.id),
    m.name,
    m.logo,
    m.description,
    DateRange(m.start, m.end),
    m.siteUrl,
    m.videosUrl,
    m.tags,
    m.location,
    m.cfp,
    m.tickets,
    m.social,
    User.empty)
}
