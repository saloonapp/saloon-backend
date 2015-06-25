package models.event

import common.infrastructure.repository.Repository
import play.api.data.Forms._
import play.api.libs.json.Json
import org.joda.time.DateTime

case class PersonSocial(
  siteUrl: Option[String],
  facebookUrl: Option[String],
  twitterUrl: Option[String],
  linkedinUrl: Option[String],
  githubUrl: Option[String])
case class Person(
  name: String,
  description: String,
  company: String,
  avatar: String,
  email: Option[String],
  profilUrl: String,
  social: PersonSocial) {
  def transform(eventId: String, role: String): Attendee = Attendee(
    Repository.generateUuid(),
    eventId,
    this.name,
    this.description,
    AttendeeImages(this.avatar),
    AttendeeInfo(role, "", this.company, if (this.profilUrl.isEmpty()) None else Some(this.profilUrl)),
    AttendeeSocial(this.social.siteUrl, this.social.facebookUrl, this.social.twitterUrl, this.social.linkedinUrl, this.social.githubUrl),
    AttendeeMeta(None, new DateTime(), new DateTime()))
}
object Person {
  implicit val formatPersonSocial = Json.format[PersonSocial]
  implicit val format = Json.format[Person]
  val fields = mapping(
    "name" -> text,
    "description" -> text,
    "company" -> text,
    "avatar" -> text,
    "email" -> optional(text),
    "profilUrl" -> text,
    "social" -> mapping(
      "siteUrl" -> optional(text),
      "facebookUrl" -> optional(text),
      "twitterUrl" -> optional(text),
      "linkedinUrl" -> optional(text),
      "githubUrl" -> optional(text))(PersonSocial.apply)(PersonSocial.unapply))(Person.apply)(Person.unapply)
}
