package conferences.models

import common.models.utils.{Forms, DateRange, tStringHelper, tString}
import common.models.values.UUID
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
  description: Option[String],
  start: DateTime,
  end: DateTime,
  siteUrl: String,
  videosUrl: Option[String],
  tags: List[String],
  venue: Option[ConferenceVenue],
  cfp: Option[ConferenceCfp],
  tickets: Option[ConferenceTickets],
  metrics: Option[ConferenceMetrics],
  social: Option[ConferenceSocial],
  created: DateTime)
case class ConferenceVenue(
  name: Option[String],
  street: String,
  zipCode: String,
  city: String,
  country: String)
case class ConferenceCfp(
  siteUrl: String,
  start: DateTime,
  end: DateTime) {
  lazy val opened: Boolean =
    start.isBeforeNow() && end.isAfterNow()
}
case class ConferenceTickets(
  siteUrl: Option[String],
  start: Option[DateTime],
  end: Option[DateTime],
  from: Option[Int],
  to: Option[Int],
  currency: Option[String]) {
  lazy val opened: Boolean =
    start.map(_.isBeforeNow()).getOrElse(true) && end.map(_.isAfterNow()).getOrElse(true)
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
  twitter: Option[ConferenceSocialTwitter])
case class ConferenceSocialTwitter(
  account: Option[String],
  hashtag: Option[String])
object Conference {
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
  description: Option[String],
  dates: DateRange,
  siteUrl: String,
  videosUrl: Option[String],
  tags: List[String],
  venue: Option[ConferenceVenue],
  cfp: Option[ConferenceDataCfp],
  tickets: Option[ConferenceDataTickets],
  metrics: Option[ConferenceMetrics],
  social: Option[ConferenceSocial])
case class ConferenceDataCfp(
  siteUrl: String,
  dates: DateRange)
case class ConferenceDataTickets(
  siteUrl: Option[String],
  dates: Option[DateRange],
  from: Option[Int],
  to: Option[Int],
  currency: Option[String])
object ConferenceData {
  val fields = mapping(
    "id" -> optional(nonEmptyText),
    "name" -> nonEmptyText,
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
    "cfp" -> optional(mapping(
      "siteUrl" -> nonEmptyText,
      "dates" -> DateRange.mapping.verifying(DateRange.Constraints.required)
    )(ConferenceDataCfp.apply)(ConferenceDataCfp.unapply)),
    "tickets" -> optional(mapping(
      "siteUrl" -> optional(nonEmptyText),
      "dates" -> optional(DateRange.mapping),
      "from" -> optional(number),
      "to" -> optional(number),
      "currency" -> optional(nonEmptyText)
    )(ConferenceDataTickets.apply)(ConferenceDataTickets.unapply)),
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
    )(ConferenceSocial.apply)(ConferenceSocial.unapply))
  )(ConferenceData.apply)(ConferenceData.unapply)
  def toModel(d: ConferenceData): Conference = Conference(
    d.id.map(s => ConferenceId(s)).getOrElse(ConferenceId.generate()),
    d.name,
    d.description,
    d.dates.start,
    d.dates.end,
    d.siteUrl,
    d.videosUrl,
    d.tags.map(_.trim.toLowerCase).filter(_.length > 0),
    d.venue,
    d.cfp.map(c => ConferenceCfp(c.siteUrl, c.dates.start, c.dates.end)),
    d.tickets.map(t => ConferenceTickets(t.siteUrl, t.dates.map(_.start), t.dates.map(_.end), t.from, t.to, t.currency)),
    d.metrics.flatMap(m => m.attendeeCount.orElse(m.sessionCount).orElse(m.sinceYear).map(_ => m)),
    d.social,
    new DateTime())
  def fromModel(m: Conference): ConferenceData = ConferenceData(
    Some(m.id.unwrap),
    m.name,
    m.description,
    DateRange(m.start, m.end),
    m.siteUrl,
    m.videosUrl,
    m.tags,
    m.venue,
    m.cfp.map(c => ConferenceDataCfp(c.siteUrl, DateRange(c.start, c.end))),
    m.tickets.map(t => ConferenceDataTickets(t.siteUrl, t.start.zip(t.end).headOption.map(d => DateRange(d._1, d._2)), t.from, t.to, t.currency)),
    m.metrics,
    m.social)
}
