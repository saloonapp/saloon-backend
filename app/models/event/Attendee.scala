package models.event

import play.api.data.Forms._
import play.api.libs.json.Json

case class AttendeeSocial(
  siteUrl: Option[String],
  facebookUrl: Option[String],
  twitterUrl: Option[String],
  linkedinUrl: Option[String],
  githubUrl: Option[String])
object AttendeeSocial {
  implicit val format = Json.format[AttendeeSocial]
  val fields = mapping(
    "siteUrl" -> optional(text),
    "facebookUrl" -> optional(text),
    "twitterUrl" -> optional(text),
    "linkedinUrl" -> optional(text),
    "githubUrl" -> optional(text))(AttendeeSocial.apply)(AttendeeSocial.unapply)
}

case class Attendee(
  name: String,
  description: String,
  company: String,
  avatar: String,
  email: Option[String],
  profilUrl: String,
  social: AttendeeSocial)
object Attendee {
  implicit val format = Json.format[Attendee]
  val fields = mapping(
    "name" -> text,
    "description" -> text,
    "company" -> text,
    "avatar" -> text,
    "email" -> optional(text),
    "profilUrl" -> text,
    "social" -> AttendeeSocial.fields)(Attendee.apply)(Attendee.unapply)
}
