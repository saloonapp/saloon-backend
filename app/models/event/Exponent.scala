package models.event

import common.Utils
import common.infrastructure.repository.Repository
import models.values.DataSource
import services.FileImporter
import org.joda.time.DateTime
import scala.util.Try
import play.api.data.Forms._
import play.api.libs.json.Json

case class ExponentImages(
  logo: String, // squared logo (~ 100x100)
  landing: String) // landscape img (~ 400x150)
case class ExponentInfo(
  website: String,
  place: String, // where to find this exponent
  team: List[Person], // people being part of this exponent
  team2: Option[List[String]],
  level: Option[Int], // level of exponent (sponsoring) : lower is better
  sponsor: Boolean) // to show it on info tab
case class ExponentConfig(
  scanQRCode: Boolean)
case class ExponentMeta(
  source: Option[DataSource], // where the exponent were fetched (if applies)
  created: DateTime,
  updated: DateTime)
case class Exponent(
  uuid: String,
  eventId: String,
  name: String,
  description: String,
  images: ExponentImages,
  info: ExponentInfo,
  config: ExponentConfig,
  meta: ExponentMeta) extends EventItem {
  def toMap(): Map[String, String] = Exponent.toMap(this)
  def merge(e: Exponent): Exponent = Exponent.merge(this, e)
}
object Exponent {
  val className = "exponents"
  implicit val formatExponentImages = Json.format[ExponentImages]
  implicit val formatExponentInfo = Json.format[ExponentInfo]
  implicit val formatExponentConfig = Json.format[ExponentConfig]
  implicit val formatExponentMeta = Json.format[ExponentMeta]
  implicit val format = Json.format[Exponent]
  private def parseDate(date: String) = Utils.parseDate(FileImporter.dateFormat)(date)

  def fromMap(eventId: String)(d: Map[String, String]): Try[Exponent] =
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
        d.get("info.team").flatMap(json => if (json.isEmpty) None else Json.parse(json.replace("\r", "\\r").replace("\n", "\\n")).asOpt[List[Person]]).getOrElse(List()),
        Some(List()),
        d.get("info.level").flatMap(l => if (l.isEmpty) None else Some(l.toInt)),
        d.get("info.sponsor").flatMap(s => if (s.isEmpty) None else Some(s.toBoolean)).getOrElse(false)),
      ExponentConfig(
        d.get("config.scanQRCode").flatMap(s => if (s.isEmpty) None else Some(s.toBoolean)).getOrElse(false)),
      ExponentMeta(
        d.get("meta.source.ref").map { ref => DataSource(ref, d.get("meta.source.name").getOrElse(""), d.get("meta.source.url").getOrElse("")) },
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
    "info.team" -> Json.stringify(Json.toJson(e.info.team)),
    "info.level" -> e.info.level.map(_.toString).getOrElse(""),
    "info.sponsor" -> e.info.sponsor.toString,
    "config.scanQRCode" -> e.config.scanQRCode.toString,
    "meta.source.ref" -> e.meta.source.map(_.ref).getOrElse(""),
    "meta.source.name" -> e.meta.source.map(_.name).getOrElse(""),
    "meta.source.url" -> e.meta.source.map(_.url).getOrElse(""),
    "meta.created" -> e.meta.created.toString(FileImporter.dateFormat),
    "meta.updated" -> e.meta.updated.toString(FileImporter.dateFormat))

  def merge(e1: Exponent, e2: Exponent): Exponent = Exponent(
    e1.uuid,
    e1.eventId,
    merge(e1.name, e2.name),
    merge(e1.description, e2.description),
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
    merge(e1.team2, e2.team2),
    merge(e1.level, e2.level),
    e2.sponsor)
  private def merge(e1: ExponentConfig, e2: ExponentConfig): ExponentConfig = ExponentConfig(
    e2.scanQRCode)
  private def merge(e1: ExponentMeta, e2: ExponentMeta): ExponentMeta = ExponentMeta(
    merge(e1.source, e2.source),
    e1.created,
    e2.updated)
  private def merge(e1: String, e2: String): String = if (e2.isEmpty) e1 else e2
  private def merge[A](e1: Option[A], e2: Option[A]): Option[A] = if (e2.isEmpty) e1 else e2
  private def merge[A](e1: List[A], e2: List[A]): List[A] = if (e2.isEmpty) e1 else e2
}

// mapping object for Exponent Form
case class ExponentMetaData(
  source: Option[DataSource])
case class ExponentData(
  eventId: String,
  name: String,
  description: String,
  images: ExponentImages,
  info: ExponentInfo,
  config: ExponentConfig,
  meta: ExponentMetaData)
object ExponentData {
  val fields = mapping(
    "eventId" -> nonEmptyText,
    "name" -> nonEmptyText,
    "description" -> text,
    "images" -> mapping(
      "logo" -> text,
      "landing" -> text)(ExponentImages.apply)(ExponentImages.unapply),
    "info" -> mapping(
      "website" -> text,
      "place" -> text,
      "team" -> list(Person.fields),
      "team2" -> optional(list(text)),
      "level" -> optional(number),
      "sponsor" -> boolean)(ExponentInfo.apply)(ExponentInfo.unapply),
    "config" -> mapping(
      "scanQRCode" -> boolean)(ExponentConfig.apply)(ExponentConfig.unapply),
    "meta" -> mapping(
      "source" -> optional(DataSource.fields))(ExponentMetaData.apply)(ExponentMetaData.unapply))(ExponentData.apply)(ExponentData.unapply)

  def toModel(d: ExponentInfo): ExponentInfo = ExponentInfo(d.website, d.place, d.team.filter(!_.name.isEmpty), Some(List()), d.level, d.sponsor)
  def toModel(d: ExponentMetaData): ExponentMeta = ExponentMeta(d.source, new DateTime(), new DateTime())
  def toModel(d: ExponentData): Exponent = Exponent(Repository.generateUuid(), d.eventId, d.name, d.description, d.images, toModel(d.info), d.config, toModel(d.meta))
  def fromModel(d: ExponentMeta): ExponentMetaData = ExponentMetaData(d.source)
  def fromModel(d: Exponent): ExponentData = ExponentData(d.eventId, d.name, d.description, d.images, d.info, d.config, fromModel(d.meta))
  def merge(m: ExponentMeta, d: ExponentMetaData): ExponentMeta = toModel(d).copy(source = m.source, created = m.created)
  def merge(m: Exponent, d: ExponentData): Exponent = toModel(d).copy(uuid = m.uuid, meta = merge(m.meta, d.meta))
}
