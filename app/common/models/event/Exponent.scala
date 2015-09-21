package common.models.event

import common.Utils
import common.models.utils.tString
import common.models.utils.tStringHelper
import common.models.values.UUID
import common.models.values.Source
import common.models.values.typed._
import common.models.user.OrganizationId
import common.repositories.Repository
import common.services.FileImporter
import org.joda.time.DateTime
import scala.util.Try
import play.api.data.Forms._
import play.api.libs.json.Json
import org.jsoup.Jsoup
import common.models.values.typed.TextMultiline

case class ExponentId(id: String) extends AnyVal with tString with UUID {
  def unwrap: String = this.id
}
object ExponentId extends tStringHelper[ExponentId] {
  def generate(): ExponentId = ExponentId(UUID.generate())
  def build(str: String): Either[String, ExponentId] = UUID.toUUID(str).right.map(id => ExponentId(id)).left.map(_ + " for ExponentId")
}

case class ExponentImages(
  logo: ImageUrl, // squared logo (~ 100x100)
  landing: ImageUrl) // landscape img (~ 400x150)
case class ExponentInfo(
  website: WebsiteUrl, // TODO : transform to Option[String]
  place: EventLocation, // where to find this exponent
  team: List[AttendeeId], // attendees being part of this exponent
  sponsorLevel: Option[Int])
case class ExponentConfig(
  scanQRCode: Boolean)
case class ExponentMeta(
  source: Option[Source], // where the exponent were fetched (if applies)
  created: DateTime,
  updated: DateTime)
case class Exponent(
  uuid: ExponentId,
  eventId: EventId,
  ownerId: Option[OrganizationId],
  name: FullName,
  description: TextMultiline,
  descriptionHTML: TextHTML,
  images: ExponentImages,
  info: ExponentInfo,
  config: ExponentConfig,
  meta: ExponentMeta) extends EventItem {
  def hasMember(attendee: Attendee): Boolean = this.hasMember(attendee.uuid)
  def hasMember(attendeeId: AttendeeId): Boolean = this.info.team.contains(attendeeId)
  def merge(e: Exponent): Exponent = Exponent.merge(this, e)
  def toBackendExport(): Map[String, String] = Exponent.toBackendExport(this)
  //def toMap(): Map[String, String] = Exponent.toMap(this)
}
object Exponent {
  implicit val formatExponentImages = Json.format[ExponentImages]
  implicit val formatExponentInfo = Json.format[ExponentInfo]
  implicit val formatExponentConfig = Json.format[ExponentConfig]
  implicit val formatExponentMeta = Json.format[ExponentMeta]
  implicit val format = Json.format[Exponent]

  def toBackendExport(e: Exponent): Map[String, String] = Map(
    "uuid" -> e.uuid.unwrap,
    "name" -> e.name.unwrap,
    "description" -> e.description.unwrap,
    "logo" -> e.images.logo.unwrap,
    "landing" -> e.images.landing.unwrap,
    "website" -> e.info.website.unwrap,
    "location" -> e.info.place.unwrap,
    "teamUuids" -> Utils.fromList(e.info.team.map(_.unwrap)),
    "sponsorLevel" -> e.info.sponsorLevel.map(_ match {
      case 1 => "Gold"
      case 2 => "Silver"
      case 3 => "Bronze"
      case _ => "Non"
    }).getOrElse("Non"),
    "scanQRCode" -> e.config.scanQRCode.toString,
    "created" -> e.meta.created.toString(FileImporter.dateFormat),
    "updated" -> e.meta.updated.toString(FileImporter.dateFormat))

  def merge(e1: Exponent, e2: Exponent): Exponent = Exponent(
    e1.uuid,
    e1.eventId,
    merge(e1.ownerId, e2.ownerId),
    merge(e1.name, e2.name),
    merge(e1.description, e2.description),
    merge(e1.descriptionHTML, e2.descriptionHTML),
    merge(e1.images, e2.images),
    merge(e1.info, e2.info),
    merge(e1.config, e2.config),
    merge(e1.meta, e2.meta))
  private def merge(e1: ExponentImages, e2: ExponentImages): ExponentImages = ExponentImages(
    merge(e1.logo, e2.logo),
    merge(e1.landing, e2.landing))
  private def merge(e1: ExponentInfo, e2: ExponentInfo): ExponentInfo = ExponentInfo(
    merge(e1.website, e2.website),
    merge(e1.place, e2.place),
    merge(e1.team, e2.team),
    merge(e1.sponsorLevel, e2.sponsorLevel))
  private def merge(e1: ExponentConfig, e2: ExponentConfig): ExponentConfig = ExponentConfig(
    e2.scanQRCode)
  private def merge(e1: ExponentMeta, e2: ExponentMeta): ExponentMeta = ExponentMeta(
    merge(e1.source, e2.source),
    e1.created,
    e2.updated)
  private def merge(e1: String, e2: String): String = if (e2.isEmpty) e1 else e2
  private def merge[T <: tString](e1: T, e2: T): T = if (e2.isEmpty) e1 else e2
  private def merge[A](e1: Option[A], e2: Option[A]): Option[A] = if (e2.isEmpty) e1 else e2
  private def merge[A](e1: List[A], e2: List[A]): List[A] = if (e2.isEmpty) e1 else e2

  /*def fromMap(eventId: String)(d: Map[String, String]): Try[Exponent] =
    Try(Exponent(
      d.get("uuid").flatMap(u => if (u.isEmpty) None else Some(u)).getOrElse(Repository.generateUuid()),
      eventId,
      d.get("name").get,
      d.get("description").getOrElse(""),
      ExponentImages(
        d.get("images.logo").getOrElse(""),
        d.get("images.landing").getOrElse("")),
      ExponentInfo(
        d.get("info.website").getOrElse(""),
        d.get("info.place").getOrElse(""),
        Utils.toList(d.get("info.team").getOrElse("")),
        d.get("info.level").flatMap(l => if (l.isEmpty) None else Some(l.toInt)),
        d.get("info.sponsor").flatMap(s => if (s.isEmpty) None else Some(s.toBoolean)).getOrElse(false)),
      ExponentConfig(
        d.get("config.scanQRCode").flatMap(s => if (s.isEmpty) None else Some(s.toBoolean)).getOrElse(false)),
      ExponentMeta(
        d.get("meta.source.ref").map { ref => Source(ref, d.get("meta.source.name").getOrElse(""), d.get("meta.source.url").getOrElse("")) },
        d.get("meta.created").flatMap(d => parseDate(d)).getOrElse(new DateTime()),
        d.get("meta.updated").flatMap(d => parseDate(d)).getOrElse(new DateTime()))))

  def toMap(e: Exponent): Map[String, String] = Map(
    "uuid" -> e.uuid,
    "eventId" -> e.eventId,
    "name" -> e.name,
    "description" -> e.description,
    "images.logo" -> e.images.logo,
    "images.landing" -> e.images.landing,
    "info.website" -> e.info.website,
    "info.place" -> e.info.place,
    "info.team" -> Utils.fromList(e.info.team),
    "info.level" -> e.info.level.map(_.toString).getOrElse(""),
    "info.sponsor" -> e.info.sponsor.toString,
    "config.scanQRCode" -> e.config.scanQRCode.toString,
    "meta.source.ref" -> e.meta.source.map(_.ref).getOrElse(""),
    "meta.source.name" -> e.meta.source.map(_.name).getOrElse(""),
    "meta.source.url" -> e.meta.source.map(_.url).getOrElse(""),
    "meta.created" -> e.meta.created.toString(FileImporter.dateFormat),
    "meta.updated" -> e.meta.updated.toString(FileImporter.dateFormat))
  private def parseDate(date: String) = Utils.parseDate(FileImporter.dateFormat)(date)*/
}

// mapping object for Exponent Form
case class ExponentMetaData(
  source: Option[Source])
case class ExponentData(
  eventId: EventId,
  name: FullName,
  description: TextMultiline,
  descriptionHTML: TextHTML,
  images: ExponentImages,
  info: ExponentInfo,
  config: ExponentConfig,
  meta: ExponentMetaData)
object ExponentData {
  val fields = mapping(
    "eventId" -> of[EventId],
    "name" -> of[FullName],
    "description" -> of[TextMultiline],
    "descriptionHTML" -> of[TextHTML],
    "images" -> mapping(
      "logo" -> of[ImageUrl],
      "landing" -> of[ImageUrl])(ExponentImages.apply)(ExponentImages.unapply),
    "info" -> mapping(
      "website" -> of[WebsiteUrl],
      "place" -> of[EventLocation],
      "team" -> list(of[AttendeeId]),
      "sponsorLevel" -> optional(number))(ExponentInfo.apply)(ExponentInfo.unapply),
    "config" -> mapping(
      "scanQRCode" -> boolean)(ExponentConfig.apply)(ExponentConfig.unapply),
    "meta" -> mapping(
      "source" -> optional(Source.fields))(ExponentMetaData.apply)(ExponentMetaData.unapply))(ExponentData.apply)(ExponentData.unapply)

  def toModel(d: ExponentMetaData): ExponentMeta = ExponentMeta(d.source, new DateTime(), new DateTime())
  def toModel(d: ExponentData): Exponent = Exponent(ExponentId.generate(), d.eventId, None, d.name, d.description, d.descriptionHTML, d.images, d.info, d.config, toModel(d.meta))
  def fromModel(d: ExponentMeta): ExponentMetaData = ExponentMetaData(d.source)
  def fromModel(d: Exponent): ExponentData = ExponentData(d.eventId, d.name, d.description, d.descriptionHTML, d.images, d.info, d.config, fromModel(d.meta))
  def merge(m: ExponentMeta, d: ExponentMetaData): ExponentMeta = toModel(d).copy(source = m.source, created = m.created)
  def merge(m: Exponent, d: ExponentData): Exponent = toModel(d).copy(uuid = m.uuid, ownerId = m.ownerId, meta = merge(m.meta, d.meta))
}
