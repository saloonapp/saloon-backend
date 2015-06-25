package models.event

import common.Utils
import common.infrastructure.repository.Repository
import models.values.DataSource
import services.FileImporter
import org.joda.time.DateTime
import scala.util.Try
import play.api.data.Forms._
import play.api.libs.json.Json

case class AttendeeImages(
  avatar: String)
case class AttendeeInfo(
  role: String, // staff, exposant, speaker, participant
  job: String,
  company: String,
  website: Option[String])
case class AttendeeSocial(
  blogUrl: Option[String],
  facebookUrl: Option[String],
  twitterUrl: Option[String],
  linkedinUrl: Option[String],
  githubUrl: Option[String])
case class AttendeeMeta(
  source: Option[DataSource], // where the session were fetched (if applies)
  created: DateTime,
  updated: DateTime)
case class Attendee(
  uuid: String,
  eventId: String,
  name: String,
  description: String,
  images: AttendeeImages,
  info: AttendeeInfo,
  social: AttendeeSocial,
  meta: AttendeeMeta) extends EventItem {
  def toMap(): Map[String, String] = Attendee.toMap(this)
  def merge(e: Attendee): Attendee = Attendee.merge(this, e)
}
object Attendee {
  val className = "attendees"
  implicit val formatAttendeeImages = Json.format[AttendeeImages]
  implicit val formatAttendeeInfo = Json.format[AttendeeInfo]
  implicit val formatAttendeeSocial = Json.format[AttendeeSocial]
  implicit val formatAttendeeMeta = Json.format[AttendeeMeta]
  implicit val format = Json.format[Attendee]
  private def parseDate(date: String) = Utils.parseDate(FileImporter.dateFormat)(date)

  def fromMap(eventId: String)(d: Map[String, String]): Try[Attendee] =
    Try(Attendee(
      d.get("uuid").flatMap(u => if (u.isEmpty) None else Some(u)).getOrElse(Repository.generateUuid()),
      eventId,
      d.get("name").get,
      d.get("description").getOrElse(""),
      AttendeeImages(
        d.get("images.avatar").getOrElse("")),
      AttendeeInfo(
        d.get("info.role").getOrElse(""),
        d.get("info.job").getOrElse(""),
        d.get("info.company").getOrElse(""),
        d.get("info.website").flatMap(s => if (s.isEmpty) None else Some(s))),
      AttendeeSocial(
        d.get("social.blogUrl").flatMap(s => if (s.isEmpty) None else Some(s)),
        d.get("social.facebookUrl").flatMap(s => if (s.isEmpty) None else Some(s)),
        d.get("social.twitterUrl").flatMap(s => if (s.isEmpty) None else Some(s)),
        d.get("social.linkedinUrl").flatMap(s => if (s.isEmpty) None else Some(s)),
        d.get("social.githubUrl").flatMap(s => if (s.isEmpty) None else Some(s))),
      AttendeeMeta(
        d.get("meta.source.ref").map { ref => DataSource(ref, d.get("meta.source.name").getOrElse(""), d.get("meta.source.url").getOrElse("")) },
        d.get("meta.created").flatMap(d => parseDate(d)).getOrElse(new DateTime()),
        d.get("meta.updated").flatMap(d => parseDate(d)).getOrElse(new DateTime()))))

  def toMap(e: Attendee): Map[String, String] = Map(
    "uuid" -> e.uuid,
    "eventId" -> e.eventId,
    "name" -> e.name,
    "description" -> e.description,
    "images.avatar" -> e.images.avatar,
    "info.role" -> e.info.role,
    "info.job" -> e.info.job,
    "info.company" -> e.info.company,
    "info.website" -> e.info.website.getOrElse(""),
    "social.blogUrl" -> e.social.blogUrl.getOrElse(""),
    "social.facebookUrl" -> e.social.facebookUrl.getOrElse(""),
    "social.twitterUrl" -> e.social.twitterUrl.getOrElse(""),
    "social.linkedinUrl" -> e.social.linkedinUrl.getOrElse(""),
    "social.githubUrl" -> e.social.githubUrl.getOrElse(""),
    "meta.source.ref" -> e.meta.source.map(_.ref).getOrElse(""),
    "meta.source.name" -> e.meta.source.map(_.name).getOrElse(""),
    "meta.source.url" -> e.meta.source.map(_.url).getOrElse(""),
    "meta.created" -> e.meta.created.toString(FileImporter.dateFormat),
    "meta.updated" -> e.meta.updated.toString(FileImporter.dateFormat))

  def merge(e1: Attendee, e2: Attendee): Attendee = Attendee(
    e1.uuid,
    e1.eventId,
    merge(e1.name, e2.name),
    merge(e1.description, e2.description),
    merge(e1.images, e2.images),
    merge(e1.info, e2.info),
    merge(e1.social, e2.social),
    merge(e1.meta, e2.meta))
  private def merge(e1: AttendeeImages, e2: AttendeeImages): AttendeeImages = AttendeeImages(
    merge(e1.avatar, e2.avatar))
  private def merge(e1: AttendeeInfo, e2: AttendeeInfo): AttendeeInfo = AttendeeInfo(
    merge(e1.role, e2.role),
    merge(e1.job, e2.job),
    merge(e1.company, e2.company),
    merge(e1.website, e2.website))
  private def merge(e1: AttendeeSocial, e2: AttendeeSocial): AttendeeSocial = AttendeeSocial(
    merge(e1.blogUrl, e2.blogUrl),
    merge(e1.facebookUrl, e2.facebookUrl),
    merge(e1.twitterUrl, e2.twitterUrl),
    merge(e1.linkedinUrl, e2.linkedinUrl),
    merge(e1.githubUrl, e2.githubUrl))
  private def merge(e1: AttendeeMeta, e2: AttendeeMeta): AttendeeMeta = AttendeeMeta(
    merge(e1.source, e2.source),
    e1.created,
    e2.updated)
  private def merge(e1: String, e2: String): String = if (e2.isEmpty) e1 else e2
  private def merge[A](e1: Option[A], e2: Option[A]): Option[A] = if (e2.isEmpty) e1 else e2
  private def merge[A](e1: List[A], e2: List[A]): List[A] = if (e2.isEmpty) e1 else e2
}

// mapping object for Attendee Form
case class AttendeeMetaData(
  source: Option[DataSource])
case class AttendeeData(
  eventId: String,
  name: String,
  description: String,
  images: AttendeeImages,
  info: AttendeeInfo,
  social: AttendeeSocial,
  meta: AttendeeMetaData)
object AttendeeData {
  val fields = mapping(
    "eventId" -> nonEmptyText,
    "name" -> nonEmptyText,
    "description" -> text,
    "images" -> mapping(
      "avatar" -> text)(AttendeeImages.apply)(AttendeeImages.unapply),
    "info" -> mapping(
      "role" -> text,
      "job" -> text,
      "company" -> text,
      "website" -> optional(text))(AttendeeInfo.apply)(AttendeeInfo.unapply),
    "social" -> mapping(
      "blogUrl" -> optional(text),
      "facebookUrl" -> optional(text),
      "twitterUrl" -> optional(text),
      "linkedinUrl" -> optional(text),
      "githubUrl" -> optional(text))(AttendeeSocial.apply)(AttendeeSocial.unapply),
    "meta" -> mapping(
      "source" -> optional(DataSource.fields))(AttendeeMetaData.apply)(AttendeeMetaData.unapply))(AttendeeData.apply)(AttendeeData.unapply)

  def toModel(d: AttendeeMetaData): AttendeeMeta = AttendeeMeta(d.source, new DateTime(), new DateTime())
  def toModel(d: AttendeeData): Attendee = Attendee(Repository.generateUuid(), d.eventId, d.name, d.description, d.images, d.info, d.social, toModel(d.meta))
  def fromModel(d: AttendeeMeta): AttendeeMetaData = AttendeeMetaData(d.source)
  def fromModel(d: Attendee): AttendeeData = AttendeeData(d.eventId, d.name, d.description, d.images, d.info, d.social, fromModel(d.meta))
  def merge(m: AttendeeMeta, d: AttendeeMetaData): AttendeeMeta = toModel(d).copy(source = m.source, created = m.created)
  def merge(m: Attendee, d: AttendeeData): Attendee = toModel(d).copy(uuid = m.uuid, meta = merge(m.meta, d.meta))
}