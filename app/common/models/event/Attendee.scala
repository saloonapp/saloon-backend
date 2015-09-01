package common.models.event

import common.views.Helpers
import common.Utils
import common.models.values.Address
import common.repositories.Repository
import common.models.values.DataSource
import common.services.FileImporter
import org.joda.time.DateTime
import scala.util.Try
import play.api.data.Forms._
import play.api.libs.json.Json
import org.jsoup.Jsoup

case class AttendeeImages(
  avatar: String)
case class AttendeeInfo(
  role: String, // cf AttendeeRole
  genre: String,
  firstName: String,
  lastName: String,
  birthYear: Option[Int],
  email: String,
  phone: String,
  address: Address,
  job: String,
  company: String,
  website: Option[String])
case class AttendeeCV(
  integral: String,
  nameless: String,
  content: String)
case class AttendeeSocial(
  blogUrl: Option[String],
  facebookUrl: Option[String],
  twitterUrl: Option[String],
  linkedinUrl: Option[String],
  viadeoUrl: Option[String],
  githubUrl: Option[String])
case class AttendeeQuestion(
  question: String,
  answers: List[String])
case class AttendeeMeta(
  source: Option[DataSource], // where the session were fetched (if applies)
  created: DateTime,
  updated: DateTime)
case class Attendee(
  uuid: String,
  eventId: String,
  name: String,
  description: String,
  descriptionHTML: String,
  images: AttendeeImages,
  info: AttendeeInfo,
  cv: Option[AttendeeCV],
  social: AttendeeSocial,
  survey: List[AttendeeQuestion],
  meta: AttendeeMeta) extends EventItem {
  def links(): List[(String, String, String)] = {
    List(
      this.info.website.map(url => (url, "Site", "md md-link")),
      this.social.blogUrl.map(url => (url, "Blog", "md md-messenger")),
      this.social.facebookUrl.map(url => (url, "Facebook", "socicon socicon-facebook")),
      this.social.twitterUrl.map(url => (url, "Twitter", "socicon socicon-twitter")),
      this.social.linkedinUrl.map(url => (url, "Linkedin", "socicon socicon-linkedin")),
      this.social.viadeoUrl.map(url => (url, "Viadeo", "socicon socicon-viadeo")),
      this.social.githubUrl.map(url => (url, "Github", "socicon socicon-github"))).flatten
  }
  def position(): Option[String] = Helpers.strOpt(List(this.info.job, this.info.company).filter(_!="").mkString(" chez "))
  def merge(e: Attendee): Attendee = Attendee.merge(this, e)
  def toBackendExport(): Map[String, String] = Attendee.toBackendExport(this)
  //def toMap(): Map[String, String] = Attendee.toMap(this)
}
object Attendee {
  val className = "attendees"
  implicit val formatAttendeeImages = Json.format[AttendeeImages]
  implicit val formatAttendeeInfo = Json.format[AttendeeInfo]
  implicit val formatAttendeeCV = Json.format[AttendeeCV]
  implicit val formatAttendeeSocial = Json.format[AttendeeSocial]
  implicit val formatAttendeeQuestion = Json.format[AttendeeQuestion]
  implicit val formatAttendeeMeta = Json.format[AttendeeMeta]
  implicit val format = Json.format[Attendee]

  def toBackendExport(e: Attendee): Map[String, String] = e.survey.map(q => (q.question, Utils.fromList(q.answers))).toMap ++ Map(
    "uuid" -> e.uuid,
    "name" -> e.name,
    "description" -> e.description,
    "avatar" -> e.images.avatar,
    "role" -> e.info.role,
    "genre" -> e.info.genre,
    "firstName" -> e.info.firstName,
    "lastName" -> e.info.lastName,
    "birthYear" -> e.info.birthYear.map(_.toString).getOrElse(""),
    "email" -> e.info.email,
    "phone" -> e.info.phone,
    "street" -> e.info.address.street,
    "zipCode" -> e.info.address.zipCode,
    "city" -> e.info.address.city,
    "job" -> e.info.job,
    "company" -> e.info.company,
    "website" -> e.info.website.getOrElse(""),
    "blogUrl" -> e.social.blogUrl.getOrElse(""),
    "facebookUrl" -> e.social.facebookUrl.getOrElse(""),
    "twitterUrl" -> e.social.twitterUrl.getOrElse(""),
    "linkedinUrl" -> e.social.linkedinUrl.getOrElse(""),
    "viadeoUrl" -> e.social.viadeoUrl.getOrElse(""),
    "githubUrl" -> e.social.githubUrl.getOrElse(""),
    "created" -> e.meta.created.toString(FileImporter.dateFormat),
    "updated" -> e.meta.updated.toString(FileImporter.dateFormat))

  def merge(e1: Attendee, e2: Attendee): Attendee = Attendee(
    e1.uuid,
    e1.eventId,
    merge(e1.name, e2.name),
    merge(e1.description, e2.description),
    merge(e1.descriptionHTML, e2.descriptionHTML),
    merge(e1.images, e2.images),
    merge(e1.info, e2.info),
    merge(e1.cv, e2.cv),
    merge(e1.social, e2.social),
    merge(e1.survey, e2.survey),
    merge(e1.meta, e2.meta))
  private def merge(e1: AttendeeImages, e2: AttendeeImages): AttendeeImages = AttendeeImages(
    merge(e1.avatar, e2.avatar))
  private def merge(e1: AttendeeInfo, e2: AttendeeInfo): AttendeeInfo = AttendeeInfo(
    merge(e1.role, e2.role),
    merge(e1.genre, e2.genre),
    merge(e1.firstName, e2.firstName),
    merge(e1.lastName, e2.lastName),
    merge(e1.birthYear, e2.birthYear),
    merge(e1.email, e2.email),
    merge(e1.phone, e2.phone),
    merge(e1.address, e2.address),
    merge(e1.job, e2.job),
    merge(e1.company, e2.company),
    merge(e1.website, e2.website))
  private def merge(e1: AttendeeSocial, e2: AttendeeSocial): AttendeeSocial = AttendeeSocial(
    merge(e1.blogUrl, e2.blogUrl),
    merge(e1.facebookUrl, e2.facebookUrl),
    merge(e1.twitterUrl, e2.twitterUrl),
    merge(e1.linkedinUrl, e2.linkedinUrl),
    merge(e1.viadeoUrl, e2.viadeoUrl),
    merge(e1.githubUrl, e2.githubUrl))
  private def merge(e1: AttendeeMeta, e2: AttendeeMeta): AttendeeMeta = AttendeeMeta(
    merge(e1.source, e2.source),
    e1.created,
    e2.updated)
  private def merge(e1: Address, e2: Address): Address = if (e2.name.isEmpty && e2.street.isEmpty && e2.zipCode.isEmpty && e2.city.isEmpty) e1 else e2
  private def merge(e1: String, e2: String): String = if (e2.isEmpty) e1 else e2
  private def merge[A](e1: Option[A], e2: Option[A]): Option[A] = if (e2.isEmpty) e1 else e2
  private def merge[A](e1: List[A], e2: List[A]): List[A] = if (e2.isEmpty) e1 else e2

  /*def fromMap(eventId: String)(d: Map[String, String]): Try[Attendee] =
    Try(Attendee(
      d.get("uuid").flatMap(u => if (u.isEmpty) None else Some(u)).getOrElse(Repository.generateUuid()),
      eventId,
      d.get("name").get,
      Jsoup.parse(d.get("descriptionHTML").getOrElse("")).text(),
      d.get("descriptionHTML").getOrElse(""),
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
  private def parseDate(date: String) = Utils.parseDate(FileImporter.dateFormat)(date)*/
}

object AttendeeRole {
  val staff = "staff"
  val exposant = "exposant"
  val speaker = "speaker"
  val visiteur = "visiteur"
  val all = List(staff, exposant, speaker, visiteur)
}

// mapping object for Attendee Form
case class AttendeeMetaData(
  source: Option[DataSource])
case class AttendeeData(
  eventId: String,
  name: String,
  description: String,
  descriptionHTML: String,
  images: AttendeeImages,
  info: AttendeeInfo,
  social: AttendeeSocial,
  meta: AttendeeMetaData)
object AttendeeData {
  val fields = mapping(
    "eventId" -> nonEmptyText,
    "name" -> nonEmptyText,
    "description" -> text,
    "descriptionHTML" -> text,
    "images" -> mapping(
      "avatar" -> text)(AttendeeImages.apply)(AttendeeImages.unapply),
    "info" -> mapping(
      "role" -> text,
      "genre" -> text,
      "firstName" -> text,
      "lasName" -> text,
      "birthYear" -> optional(number),
      "email" -> email,
      "phone" -> text,
      "address" -> Address.fields,
      "job" -> text,
      "company" -> text,
      "website" -> optional(text))(AttendeeInfo.apply)(AttendeeInfo.unapply),
    "social" -> mapping(
      "blogUrl" -> optional(text),
      "facebookUrl" -> optional(text),
      "twitterUrl" -> optional(text),
      "linkedinUrl" -> optional(text),
      "viadeoUrl" -> optional(text),
      "githubUrl" -> optional(text))(AttendeeSocial.apply)(AttendeeSocial.unapply),
    "meta" -> mapping(
      "source" -> optional(DataSource.fields))(AttendeeMetaData.apply)(AttendeeMetaData.unapply))(AttendeeData.apply)(AttendeeData.unapply)

  def toModel(d: AttendeeMetaData): AttendeeMeta = AttendeeMeta(d.source, new DateTime(), new DateTime())
  def toModel(d: AttendeeData): Attendee = Attendee(Repository.generateUuid(), d.eventId, d.name, d.description, d.descriptionHTML, d.images, d.info, None, d.social, List(), toModel(d.meta))
  def fromModel(d: AttendeeMeta): AttendeeMetaData = AttendeeMetaData(d.source)
  def fromModel(d: Attendee): AttendeeData = AttendeeData(d.eventId, d.name, d.description, d.descriptionHTML, d.images, d.info, d.social, fromModel(d.meta))
  def merge(m: AttendeeMeta, d: AttendeeMetaData): AttendeeMeta = toModel(d).copy(source = m.source, created = m.created)
  def merge(m: Attendee, d: AttendeeData): Attendee = toModel(d).copy(uuid = m.uuid, survey = m.survey, meta = merge(m.meta, d.meta))
}
