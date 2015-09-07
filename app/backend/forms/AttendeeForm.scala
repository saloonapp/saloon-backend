package backend.forms

import common.models.utils.tStringConstraints._
import common.models.values.typed._
import common.models.values.Address
import common.models.event.EventId
import common.models.event.Attendee
import common.models.event.AttendeeId
import common.models.event.AttendeeImages
import common.models.event.AttendeeInfo
import common.models.event.AttendeeSocial
import common.models.event.AttendeeMeta
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json._


case class AttendeeInfoCreateData(
  role: AttendeeRole,
  genre: Genre,
  firstName: FirstName,
  lastName: LastName,
  birthYear: Option[Int],
  email: Option[Email],
  phone: PhoneNumber,
  address: Address,
  job: JobTitle,
  company: CompanyName,
  website: Option[WebsiteUrl])
case class AttendeeCreateData(
  eventId: EventId,
  descriptionHTML: TextHTML,
  avatar: ImageUrl,
  info: AttendeeInfoCreateData,
  social: AttendeeSocial)
object AttendeeCreateData {
  val fields = mapping(
    "eventId" -> of[EventId].verifying(nonEmpty),
    "descriptionHTML" -> of[TextHTML],
    "avatar" -> of[ImageUrl],
    "info" -> mapping(
      "role" -> of[AttendeeRole],
      "genre" -> of[Genre].verifying(nonEmpty),
      "firstName" -> of[FirstName].verifying(nonEmpty),
      "lasName" -> of[LastName].verifying(nonEmpty),
      "birthYear" -> optional(number),
      "email" -> optional(of[Email]),
      "phone" -> of[PhoneNumber],
      "address" -> Address.fields,
      "job" -> of[JobTitle],
      "company" -> of[CompanyName],
      "website" -> optional(of[WebsiteUrl]))(AttendeeInfoCreateData.apply)(AttendeeInfoCreateData.unapply),
    "social" -> mapping(
      "blogUrl" -> optional(of[WebsiteUrl]),
      "facebookUrl" -> optional(of[WebsiteUrl]),
      "twitterUrl" -> optional(of[WebsiteUrl]),
      "linkedinUrl" -> optional(of[WebsiteUrl]),
      "viadeoUrl" -> optional(of[WebsiteUrl]),
      "githubUrl" -> optional(of[WebsiteUrl]))(AttendeeSocial.apply)(AttendeeSocial.unapply))(AttendeeCreateData.apply)(AttendeeCreateData.unapply)

  def toMeta(d: AttendeeCreateData): AttendeeMeta = AttendeeMeta(None, new DateTime(), new DateTime())
  def toImages(d: AttendeeCreateData): AttendeeImages = AttendeeImages(d.avatar)
  def toInfo(d: AttendeeInfoCreateData): AttendeeInfo = AttendeeInfo(d.role, d.genre, d.firstName, d.lastName, d.birthYear, d.email.getOrElse(Email("")), d.phone, d.address, d.job, d.company, d.website)
  def toModel(d: AttendeeCreateData): Attendee = Attendee(AttendeeId.generate(), d.eventId, FullName.build(d.info.firstName, d.info.lastName), d.descriptionHTML.toPlainText, d.descriptionHTML, toImages(d), toInfo(d.info), None, d.social, List(), toMeta(d))
  def fromInfo(d: AttendeeInfo): AttendeeInfoCreateData = AttendeeInfoCreateData(d.role, d.genre, d.firstName, d.lastName, d.birthYear, Some(d.email), d.phone, d.address, d.job, d.company, d.website)
  def fromModel(d: Attendee): AttendeeCreateData = AttendeeCreateData(d.eventId, d.descriptionHTML, d.images.avatar, fromInfo(d.info), d.social)
  def merge(m: Attendee, d: AttendeeCreateData): Attendee = m.copy(name = FullName.build(d.info.firstName, d.info.lastName), description = d.descriptionHTML.toPlainText, descriptionHTML = d.descriptionHTML, images = toImages(d), info = toInfo(d.info), social = d.social, meta = m.meta.copy(updated = new DateTime()))
}