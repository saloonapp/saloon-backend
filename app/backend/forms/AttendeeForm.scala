package backend.forms

import common.models.values.Address
import common.models.event.Attendee
import common.models.event.AttendeeImages
import common.models.event.AttendeeInfo
import common.models.event.AttendeeSocial
import common.models.event.AttendeeMeta
import common.repositories.Repository
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json._
import org.jsoup.Jsoup

case class AttendeeInfoCreateData(
  role: String,
  genre: String,
  firstName: String,
  lastName: String,
  birthYear: Option[Int],
  email: Option[String],
  phone: String,
  address: Address,
  job: String,
  company: String,
  website: Option[String])
case class AttendeeCreateData(
  eventId: String,
  descriptionHTML: String,
  avatar: String,
  info: AttendeeInfoCreateData,
  social: AttendeeSocial)
object AttendeeCreateData {
  val fields = mapping(
    "eventId" -> nonEmptyText,
    "descriptionHTML" -> text,
    "avatar" -> text,
    "info" -> mapping(
      "role" -> text,
      "genre" -> nonEmptyText,
      "firstName" -> nonEmptyText,
      "lasName" -> nonEmptyText,
      "birthYear" -> optional(number),
      "email" -> optional(text),
      "phone" -> text,
      "address" -> Address.fields,
      "job" -> text,
      "company" -> text,
      "website" -> optional(text))(AttendeeInfoCreateData.apply)(AttendeeInfoCreateData.unapply),
    "social" -> mapping(
      "blogUrl" -> optional(text),
      "facebookUrl" -> optional(text),
      "twitterUrl" -> optional(text),
      "linkedinUrl" -> optional(text),
      "viadeoUrl" -> optional(text),
      "githubUrl" -> optional(text))(AttendeeSocial.apply)(AttendeeSocial.unapply))(AttendeeCreateData.apply)(AttendeeCreateData.unapply)

  def toMeta(d: AttendeeCreateData): AttendeeMeta = AttendeeMeta(None, new DateTime(), new DateTime())
  def toImages(d: AttendeeCreateData): AttendeeImages = AttendeeImages(d.avatar)
  def toInfo(d: AttendeeInfoCreateData): AttendeeInfo = AttendeeInfo(d.role, d.genre, d.firstName, d.lastName, d.birthYear, d.email.getOrElse(""), d.phone, d.address, d.job, d.company, d.website)
  def toModel(d: AttendeeCreateData): Attendee = Attendee(Repository.generateUuid(), d.eventId, d.info.firstName + " " + d.info.lastName, Jsoup.parse(d.descriptionHTML).text(), d.descriptionHTML, toImages(d), toInfo(d.info), None, d.social, List(), toMeta(d))
  def fromInfo(d: AttendeeInfo): AttendeeInfoCreateData = AttendeeInfoCreateData(d.role, d.genre, d.firstName, d.lastName, d.birthYear, Some(d.email), d.phone, d.address, d.job, d.company, d.website)
  def fromModel(d: Attendee): AttendeeCreateData = AttendeeCreateData(d.eventId, d.description, d.images.avatar, fromInfo(d.info), d.social)
  def merge(m: Attendee, d: AttendeeCreateData): Attendee = m.copy(name = d.info.firstName + " " + d.info.lastName, description = Jsoup.parse(d.descriptionHTML).text(), descriptionHTML = d.descriptionHTML, images = toImages(d), info = toInfo(d.info), social = d.social, meta = m.meta.copy(updated = new DateTime()))
}