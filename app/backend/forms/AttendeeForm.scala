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

case class AttendeeCreateData(
  eventId: String,
  name: String,
  descriptionHTML: String,
  avatar: String,
  info: AttendeeInfo,
  social: AttendeeSocial)
object AttendeeCreateData {
  val fields = mapping(
    "eventId" -> nonEmptyText,
    "name" -> nonEmptyText, // TODO remove from form
    "descriptionHTML" -> text,
    "avatar" -> text,
    "info" -> mapping(
      "role" -> text,
      "genre" -> text, // TODO add to form
      "firstName" -> text, // TODO add to form
      "lasName" -> text, // TODO add to form
      "birthYear" -> optional(number), // TODO add to form
      "email" -> email, // TODO add to form
      "phone" -> text, // TODO add to form
      "address" -> Address.fields, // TODO add to form
      "job" -> text,
      "company" -> text,
      "website" -> optional(text))(AttendeeInfo.apply)(AttendeeInfo.unapply),
    "social" -> mapping(
      "blogUrl" -> optional(text),
      "facebookUrl" -> optional(text),
      "twitterUrl" -> optional(text),
      "linkedinUrl" -> optional(text),
      "viadeoUrl" -> optional(text),
      "githubUrl" -> optional(text))(AttendeeSocial.apply)(AttendeeSocial.unapply))(AttendeeCreateData.apply)(AttendeeCreateData.unapply)

  def toMeta(d: AttendeeCreateData): AttendeeMeta = AttendeeMeta(None, new DateTime(), new DateTime())
  def toImages(d: AttendeeCreateData): AttendeeImages = AttendeeImages(d.avatar)
  def toModel(d: AttendeeCreateData): Attendee = Attendee(Repository.generateUuid(), d.eventId, d.name, Jsoup.parse(d.descriptionHTML).text(), d.descriptionHTML, toImages(d), d.info, None, d.social, List(), toMeta(d))
  def fromModel(d: Attendee): AttendeeCreateData = AttendeeCreateData(d.eventId, d.name, d.description, d.images.avatar, d.info, d.social)
  def merge(m: Attendee, d: AttendeeCreateData): Attendee = m.copy(name = d.name, description = Jsoup.parse(d.descriptionHTML).text(), descriptionHTML = d.descriptionHTML, images = toImages(d), info = d.info, social = d.social, meta = m.meta.copy(updated = new DateTime()))
}