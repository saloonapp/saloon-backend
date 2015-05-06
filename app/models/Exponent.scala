package models

import infrastructure.repository.common.Repository
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json.Json

case class Exponent(
  uuid: String,
  eventId: String,
  name: String,
  description: String,
  company: String,
  place: Place, // room, booth...
  tags: List[String],
  created: DateTime,
  updated: DateTime)
object Exponent {
  implicit val format = Json.format[Exponent]
}

// mapping object for Exponent Form
case class ExponentData(
  eventId: String,
  name: String,
  description: String,
  company: String,
  place: Place,
  tags: String)
object ExponentData {
  implicit val format = Json.format[ExponentData]
  val fields = mapping(
    "eventId" -> nonEmptyText,
    "name" -> nonEmptyText,
    "description" -> text,
    "company" -> nonEmptyText,
    "place" -> Place.fields,
    "tags" -> text)(ExponentData.apply)(ExponentData.unapply)

  def toModel(d: ExponentData): Exponent = Exponent(Repository.generateUuid(), d.eventId, d.name, d.description, d.company, d.place, toTags(d.tags), new DateTime(), new DateTime())
  def fromModel(m: Exponent): ExponentData = ExponentData(m.eventId, m.name, m.description, m.company, m.place, m.tags.mkString(", "))
  def merge(m: Exponent, d: ExponentData): Exponent = m.copy(eventId = d.eventId, name = d.name, description = d.description, company = d.company, place = d.place, tags = toTags(d.tags), updated = new DateTime())

  private def toTags(str: String): List[String] = str.split(",").toList.map(_.trim())
}
