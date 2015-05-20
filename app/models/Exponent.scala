package models

import infrastructure.repository.common.Repository
import services.FileImporter
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json.Json

case class Exponent(
  uuid: String,
  eventId: String,
  image: Option[String],
  name: String,
  description: String,

  company: String,
  place: Place, // room, booth...
  siteUrl: Option[String],
  siteName: Option[String],
  images: Option[List[String]],
  tags: List[String],
  created: DateTime,
  updated: DateTime) {
  def toMap(): Map[String, String] = Exponent.toMap(this)
}
object Exponent {
  implicit val format = Json.format[Exponent]
  def fromMap(eventId: String)(d: Map[String, String]): Option[Exponent] =
    if (d.get("name").isDefined) {
      val images = ExponentData.toArray(d.get("images").getOrElse(""))
      Some(Exponent(
        Repository.generateUuid(),
        eventId,
        images.headOption,
        d.get("name").get,
        d.get("description").getOrElse(""),
        d.get("company").get,
        Place(
          d.get("place.ref").getOrElse(""),
          d.get("place.name").getOrElse("")),
        d.get("siteUrl"),
        d.get("siteName"),
        Some(images),
        ExponentData.toArray(d.get("tags").getOrElse("")),
        d.get("created").map(d => DateTime.parse(d, FileImporter.dateFormat)).getOrElse(new DateTime()),
        d.get("updated").map(d => DateTime.parse(d, FileImporter.dateFormat)).getOrElse(new DateTime())))
    } else {
      None
    }
  def toMap(e: Exponent): Map[String, String] = Map(
    "uuid" -> e.uuid,
    "eventId" -> e.eventId,
    "image" -> e.image.getOrElse(""),
    "name" -> e.name,
    "description" -> e.description,
    "company" -> e.company,
    "place.ref" -> e.place.ref,
    "place.name" -> e.place.name,
    "siteUrl" -> e.siteUrl.getOrElse(""),
    "siteName" -> e.siteName.getOrElse(""),
    "images" -> e.images.map(_.mkString(", ")).getOrElse(""),
    "tags" -> e.tags.mkString(", "),
    "created" -> e.created.toString(FileImporter.dateFormat),
    "updated" -> e.updated.toString(FileImporter.dateFormat))
}

// mapping object for Exponent Form
case class ExponentData(
  eventId: String,
  image: Option[String],
  name: String,
  description: String,
  company: String,
  place: Place,
  siteUrl: Option[String],
  siteName: Option[String],
  images: String,
  tags: String)
object ExponentData {
  implicit val format = Json.format[ExponentData]
  val fields = mapping(
    "eventId" -> nonEmptyText,
    "image" -> optional(text),
    "name" -> nonEmptyText,
    "description" -> text,
    "company" -> nonEmptyText,
    "place" -> Place.fields,
    "siteUrl" -> optional(text),
    "siteName" -> optional(text),
    "images" -> text,
    "tags" -> text)(ExponentData.apply)(ExponentData.unapply)

  def toModel(d: ExponentData): Exponent = Exponent(Repository.generateUuid(), d.eventId, d.image, d.name, d.description, d.company, d.place, d.siteUrl, d.siteName, Some(toArray(d.images)), toArray(d.tags), new DateTime(), new DateTime())
  def fromModel(m: Exponent): ExponentData = ExponentData(m.eventId, m.image, m.name, m.description, m.company, m.place, m.siteUrl, m.siteName, m.images.mkString(", "), m.tags.mkString(", "))
  def merge(m: Exponent, d: ExponentData): Exponent = m.copy(eventId = d.eventId, image = d.image, name = d.name, description = d.description, company = d.company, place = d.place, siteUrl = d.siteUrl, siteName = d.siteName, images = Some(toArray(d.images)), tags = toArray(d.tags), updated = new DateTime())

  def toArray(str: String): List[String] = str.split(",").toList.map(_.trim())
}
