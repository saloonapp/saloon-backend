package conferences.models

import common.Defaults
import common.models.utils.{Forms, DateRange, tStringHelper, tString}
import common.models.values.{Geo, GMapPlace, UUID}
import common.services.TwitterCard
import org.joda.time.DateTime
import play.api.data.Forms._
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
  venue: Option[ConferenceVenue],
  location: Option[GMapPlace],
  cfp: Option[ConferenceCfp],
  tickets: Option[ConferenceTickets],
  metrics: Option[ConferenceMetrics],
  social: Option[ConferenceSocial],
  created: DateTime,
  createdBy: Option[ConferenceUser]) {
  lazy val twitterAccount: Option[String] = social.flatMap(_.twitter.flatMap(_.account))
  lazy val twitterHashtag: Option[String] = social.flatMap(_.twitter.flatMap(_.hashtag))
  def toTwitterCard() = TwitterCard(
    "summary",
    "@conferencelist_",
    name+", le " + start.toString(Defaults.dateFormatter) + venue.map(" à "+_.city).getOrElse(""),
    List(
      cfp.flatMap(c => if(c.end.isAfterNow) Some("CFP ouvert jusqu'au "+c.end.toString(Defaults.dateFormatter)) else None),
      tickets.flatMap(t => if(end.isAfterNow && t.from.isDefined && t.currency.isDefined) Some("Billets à partir de "+t.from.get+" "+t.currency.get) else None),
      Some(tags.map("#"+_).mkString(" ")),
      description
    ).flatten.mkString(" - "),
    logo.getOrElse("https://avatars2.githubusercontent.com/u/11368266?v=3&s=200"))
}
case class ConferenceVenue(
  name: Option[String],
  street: String,
  zipCode: String,
  city: String,
  country: String) {
  def toLocation(): GMapPlace = GMapPlace(
    id = "",
    name = name.getOrElse(""),
    streetNo = None,
    street = Some(street),
    postalCode = Some(zipCode),
    locality = Some(city),
    country = country,
    formatted = "",
    input = name.getOrElse(""),
    geo = Geo(0, 0),
    url = "",
    website = None,
    phone = None)
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
case class ConferenceMetrics(
  attendeeCount: Option[Int],
  sessionCount: Option[Int],
  sinceYear: Option[Int])
case class ConferenceSocial(
  twitter: Option[ConferenceSocialTwitter]) {
  def trim(): ConferenceSocial = this.copy(
    twitter = this.twitter.map(_.trim)
  )
}
case class ConferenceSocialTwitter(
  account: Option[String],
  hashtag: Option[String]) {
  def trim(): ConferenceSocialTwitter = this.copy(
    account = this.account.map(_.trim.replace("@", "").replace("https?://twitter.com/", "")),
    hashtag = this.hashtag.map(_.trim.replace("#", "").replace("https?://twitter.com/hashtag/", "").replace("https?://twitter.com/search?q=%23", "").replace("?src=hash", "").replace("&src=typd", ""))
  )
}
case class ConferenceUser(
  name: String,
  email: Option[String],
  siteUrl: Option[String],
  twitter: Option[String],
  public: Boolean) {
  def trim(): ConferenceUser = this.copy(
    name = this.name.trim,
    email = this.email.map(_.trim),
    twitter = this.twitter.map(_.trim.replace("@", "").replace("https?://twitter.com/", ""))
  )
}
object Conference {
  implicit val formatConferenceUser = Json.format[ConferenceUser]
  implicit val formatConferenceSocialTwitter = Json.format[ConferenceSocialTwitter]
  implicit val formatConferenceSocial = Json.format[ConferenceSocial]
  implicit val formatConferenceMetrics = Json.format[ConferenceMetrics]
  implicit val formatConferenceTickets = Json.format[ConferenceTickets]
  implicit val formatConferenceCfp = Json.format[ConferenceCfp]
  implicit val formatConferenceVenue = Json.format[ConferenceVenue]
  implicit val format = Json.format[Conference]
}

case class ConferenceData(
  id: Option[String],
  name: String,
  logo: Option[String],
  description: Option[String],
  dates: DateRange,
  siteUrl: String,
  videosUrl: Option[String],
  tags: List[String],
  venue: Option[ConferenceVenue],
  location: Option[GMapPlace],
  cfp: Option[ConferenceCfp],
  tickets: Option[ConferenceTickets],
  metrics: Option[ConferenceMetrics],
  social: Option[ConferenceSocial],
  createdBy: ConferenceUser)
object ConferenceData {
  val fields = mapping(
    "id" -> optional(nonEmptyText),
    "name" -> nonEmptyText,
    "logo" -> optional(nonEmptyText),
    "description" -> optional(nonEmptyText),
    "dates" -> DateRange.mapping.verifying(DateRange.Constraints.required),
    "siteUrl" -> nonEmptyText,
    "videosUrl" -> optional(nonEmptyText),
    "tags" -> list(nonEmptyText).verifying(Forms.Constraints.required),
    "venue" -> optional(mapping(
      "name" -> optional(nonEmptyText),
      "street" -> nonEmptyText,
      "zipCode" -> nonEmptyText,
      "city" -> nonEmptyText,
      "country" -> nonEmptyText
    )(ConferenceVenue.apply)(ConferenceVenue.unapply)),
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
    "metrics" -> optional(mapping(
      "attendeeCount" -> optional(number),
      "sessionCount" -> optional(number),
      "sinceYear" -> optional(number)
    )(ConferenceMetrics.apply)(ConferenceMetrics.unapply)),
    "social" -> optional(mapping(
      "twitter" -> optional(mapping(
        "account" -> optional(nonEmptyText),
        "hashtag" -> optional(nonEmptyText)
      )(ConferenceSocialTwitter.apply)(ConferenceSocialTwitter.unapply))
    )(ConferenceSocial.apply)(ConferenceSocial.unapply)),
    "createdBy" -> mapping(
      "name" -> nonEmptyText,
      "email" -> optional(nonEmptyText),
      "siteUrl" -> optional(nonEmptyText),
      "twitter" -> optional(nonEmptyText),
      "public" -> boolean
    )(ConferenceUser.apply)(ConferenceUser.unapply)
  )(ConferenceData.apply)(ConferenceData.unapply)
  def toModel(d: ConferenceData): Conference = Conference(
    d.id.map(s => ConferenceId(s)).getOrElse(ConferenceId.generate()),
    d.name,
    d.logo,
    d.description,
    d.dates.start,
    d.dates.end,
    d.siteUrl,
    d.videosUrl,
    d.tags.map(_.trim.toLowerCase).filter(_.length > 0),
    d.venue,
    d.location,
    d.cfp,
    d.tickets.filter(t => t.siteUrl.isDefined || t.from.isDefined),
    d.metrics.flatMap(m => m.attendeeCount.orElse(m.sessionCount).orElse(m.sinceYear).map(_ => m)),
    d.social.map(_.trim),
    new DateTime(),
    Some(d.createdBy.trim))
  def fromModel(m: Conference): ConferenceData = ConferenceData(
    Some(m.id.unwrap),
    m.name,
    m.logo,
    m.description,
    DateRange(m.start, m.end),
    m.siteUrl,
    m.videosUrl,
    m.tags,
    m.venue,
    m.location.orElse(m.venue.map(_.toLocation())),
    m.cfp,
    m.tickets,
    m.metrics,
    m.social,
    ConferenceUser("", None, None, None, false))
}
