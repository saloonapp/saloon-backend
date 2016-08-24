package conferences.models

import common.models.utils.{tStringHelper, tString}
import common.models.values.UUID
import common.services.TwitterSrv
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json.Json

case class PersonId(id: String) extends AnyVal with tString with UUID {
  def unwrap: String = this.id
}
object PersonId extends tStringHelper[PersonId] {
  def generate(): PersonId = PersonId(UUID.generate())
  def build(str: String): Either[String, PersonId] = UUID.toUUID(str).right.map(id => PersonId(id)).left.map(_ + " for PersonId")
}

case class Person(
  id: PersonId,
  name: String,
  avatar: Option[String],
  twitter: Option[String],
  siteUrl: Option[String],
  email: Option[String],
  created: DateTime,
  createdBy: Option[User]) {
  def trim(): Person = this.copy(
    name = name.trim,
    avatar = avatar.map(_.trim),
    twitter = twitter.map(t => TwitterSrv.toAccount(t)),
    siteUrl = siteUrl.map(_.trim),
    email = email.map(_.trim))
}
object Person {
  implicit val format = Json.format[Person]
}

case class PersonData(
  id: Option[PersonId],
  name: String,
  avatar: Option[String],
  twitter: Option[String],
  siteUrl: Option[String],
  email: Option[String],
  createdBy: User)
object PersonData {
  implicit val format = Json.format[PersonData]
  val fields = mapping(
    "id" -> optional(of[PersonId]),
    "name" -> nonEmptyText,
    "avatar" -> optional(nonEmptyText),
    "twitter" -> optional(nonEmptyText),
    "siteUrl" -> optional(nonEmptyText),
    "email" -> optional(nonEmptyText),
    "createdBy" -> User.fields
  )(PersonData.apply)(PersonData.unapply)
  def toModel(d: PersonData): Person = Person(
    id = d.id.getOrElse(PersonId.generate()),
    name = d.name,
    avatar = d.avatar,
    twitter = d.twitter,
    siteUrl = d.siteUrl,
    email = d.email,
    created = new DateTime(),
    createdBy = Some(d.createdBy))
}
