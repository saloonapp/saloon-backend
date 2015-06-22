package models.event

import play.api.data.Forms._
import play.api.libs.json.Json

case class PersonSocial(
  siteUrl: Option[String],
  facebookUrl: Option[String],
  twitterUrl: Option[String],
  linkedinUrl: Option[String],
  githubUrl: Option[String])
object PersonSocial {
  implicit val format = Json.format[PersonSocial]
  val fields = mapping(
    "siteUrl" -> optional(text),
    "facebookUrl" -> optional(text),
    "twitterUrl" -> optional(text),
    "linkedinUrl" -> optional(text),
    "githubUrl" -> optional(text))(PersonSocial.apply)(PersonSocial.unapply)
}

case class Person(
  name: String,
  description: String,
  company: String,
  avatar: String,
  email: Option[String],
  profilUrl: String,
  social: PersonSocial)
object Person {
  implicit val format = Json.format[Person]
  val fields = mapping(
    "name" -> text,
    "description" -> text,
    "company" -> text,
    "avatar" -> text,
    "email" -> optional(text),
    "profilUrl" -> text,
    "social" -> PersonSocial.fields)(Person.apply)(Person.unapply)
}
