package backend.forms

import common.models.event.Attendee
import common.models.event.AttendeeImages
import common.models.event.AttendeeInfo
import common.models.event.AttendeeSocial
import common.models.event.AttendeeMeta
import common.repositories.Repository
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json._

case class AttendeeCreateData(
  eventId: String,
  name: String,
  descriptionHTML: String,
  avatar: String,
  role: String, // staff, exposant, speaker, participant
  job: String,
  company: String,
  website: Option[String],
  blogUrl: Option[String],
  facebookUrl: Option[String],
  twitterUrl: Option[String],
  linkedinUrl: Option[String],
  githubUrl: Option[String])
object AttendeeCreateData {
  val fields = mapping(
    "eventId" -> nonEmptyText,
    "name" -> nonEmptyText,
    "descriptionHTML" -> text,
    "avatar" -> text,
    "role" -> text,
    "job" -> text,
    "company" -> text,
    "website" -> optional(text),
    "blogUrl" -> optional(text),
    "facebookUrl" -> optional(text),
    "twitterUrl" -> optional(text),
    "linkedinUrl" -> optional(text),
    "githubUrl" -> optional(text))(AttendeeCreateData.apply)(AttendeeCreateData.unapply)

  def toMeta(d: AttendeeCreateData): AttendeeMeta = AttendeeMeta(None, new DateTime(), new DateTime())
  def toSocial(d: AttendeeCreateData): AttendeeSocial = AttendeeSocial(d.blogUrl, d.facebookUrl, d.twitterUrl, d.linkedinUrl, d.githubUrl)
  def toInfo(d: AttendeeCreateData): AttendeeInfo = AttendeeInfo(d.role, d.job, d.company, d.website)
  def toImages(d: AttendeeCreateData): AttendeeImages = AttendeeImages(d.avatar)
  def toModel(d: AttendeeCreateData): Attendee = Attendee(Repository.generateUuid(), d.eventId, d.name, d.descriptionHTML, toImages(d), toInfo(d), toSocial(d), toMeta(d))
  def fromModel(d: Attendee): AttendeeCreateData = AttendeeCreateData(d.eventId, d.name, d.description, d.images.avatar, d.info.role, d.info.job, d.info.company, d.info.website, d.social.blogUrl, d.social.facebookUrl, d.social.twitterUrl, d.social.linkedinUrl, d.social.githubUrl)
  def merge(m: Attendee, d: AttendeeCreateData): Attendee = m.copy(name = d.name, description = d.descriptionHTML, images = toImages(d), info = toInfo(d), social = toSocial(d), meta = m.meta.copy(updated = new DateTime()))
}