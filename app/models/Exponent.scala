package models

import common.Utils
import common.infrastructure.repository.Repository
import services.FileImporter
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json.Json

case class Exponent(
  uuid: String,
  eventId: String,
  name: String,
  description: String,
  logoUrl: String, // squared logo of event (~ 100x100)
  landingUrl: String, // landscape img for event (in info screen) (~ 400x150)
  siteUrl: String,
  place: Option[String], // where to find this exponent
  team: List[Person], // people being part of this exponent
  level: Option[Int], // level of exponent (sponsoring) : lower is better
  sponsor: Boolean, // to show it on info tab
  tags: List[String],
  images: List[String],
  source: Option[DataSource], // where the exponent were fetched (if applies)
  created: DateTime,
  updated: DateTime) extends EventItem {
  def toMap(): Map[String, String] = Exponent.toMap(this)
}
object Exponent {
  implicit val format = Json.format[Exponent]
  private def parseDate(date: String) = Utils.parseDate(FileImporter.dateFormat)(date)
  def fromMap(eventId: String)(d: Map[String, String]): Option[Exponent] =
    if (d.get("name").isDefined) {
      Some(Exponent(
        d.get("uuid").flatMap(u => if (u.isEmpty) None else Some(u)).getOrElse(Repository.generateUuid()),
        eventId,
        d.get("name").get,
        d.get("description").getOrElse(""),
        d.get("logoUrl").getOrElse(""),
        d.get("landingUrl").getOrElse(""),
        d.get("siteUrl").getOrElse(""),
        d.get("place"),
        d.get("team").flatMap(json => if (json.isEmpty) None else Json.parse(json.replace("\r", "\\r").replace("\n", "\\n")).asOpt[List[Person]]).getOrElse(List()),
        d.get("level").flatMap(l => if (l.isEmpty) None else Some(l.toInt)),
        d.get("sponsor").flatMap(s => if (s.isEmpty) None else Some(s.toBoolean)).getOrElse(false),
        Utils.toList(d.get("tags").getOrElse("")),
        Utils.toList(d.get("images").getOrElse("")),
        d.get("source.url").map(url => DataSource(d.get("source.ref").getOrElse(""), url)),
        d.get("created").flatMap(d => parseDate(d)).getOrElse(new DateTime()),
        d.get("updated").flatMap(d => parseDate(d)).getOrElse(new DateTime())))
    } else {
      None
    }
  def toMap(e: Exponent): Map[String, String] = Map(
    "uuid" -> e.uuid,
    "eventId" -> e.eventId,
    "name" -> e.name,
    "description" -> e.description,
    "logoUrl" -> e.logoUrl,
    "landingUrl" -> e.landingUrl,
    "siteUrl" -> e.siteUrl,
    "place" -> e.place.getOrElse(""),
    "team" -> Json.stringify(Json.toJson(e.team)),
    "level" -> e.level.map(_.toString).getOrElse(""),
    "sponsor" -> e.sponsor.toString,
    "tags" -> Utils.fromList(e.tags),
    "images" -> Utils.fromList(e.images),
    "source.ref" -> e.source.map(_.ref).getOrElse(""),
    "source.url" -> e.source.map(_.url).getOrElse(""),
    "created" -> e.created.toString(FileImporter.dateFormat),
    "updated" -> e.updated.toString(FileImporter.dateFormat))
}

// model sent to client : no field source / add field className
case class ExponentUI(
  uuid: String,
  eventId: String,
  name: String,
  description: String,
  logoUrl: String,
  landingUrl: String,
  siteUrl: String,
  place: Option[String],
  team: List[Person],
  level: Option[Int],
  sponsor: Boolean,
  tags: List[String],
  images: List[String],
  created: DateTime,
  updated: DateTime,
  className: String = ExponentUI.className)
object ExponentUI {
  val className = "exponents"
  implicit val format = Json.format[ExponentUI]
  // def toModel(d: ExponentUI): Exponent = Exponent(d.uuid, d.eventId, d.name, d.description, d.logoUrl, d.landingUrl, d.siteUrl, d.place, d.team, d.level, d.sponsor, d.tags, d.images, None, d.created, d.updated)
  def fromModel(d: Exponent): ExponentUI = ExponentUI(d.uuid, d.eventId, d.name, d.description, d.logoUrl, d.landingUrl, d.siteUrl, d.place, d.team, d.level, d.sponsor, d.tags, d.images, d.created, d.updated)
}

// mapping object for Exponent Form
case class ExponentData(
  eventId: String,
  name: String,
  description: String,
  logoUrl: String,
  landingUrl: String,
  siteUrl: String,
  place: Option[String],
  team: List[Person],
  level: Option[Int],
  sponsor: Boolean,
  tags: String,
  images: String)
object ExponentData {
  implicit val format = Json.format[ExponentData]
  val fields = mapping(
    "eventId" -> nonEmptyText,
    "name" -> nonEmptyText,
    "description" -> text,
    "logoUrl" -> text,
    "landingUrl" -> text,
    "siteUrl" -> text,
    "place" -> optional(text),
    "team" -> list(Person.fields),
    "level" -> optional(number),
    "sponsor" -> boolean,
    "tags" -> text,
    "images" -> text)(ExponentData.apply)(ExponentData.unapply)

  def toModel(d: ExponentData): Exponent = Exponent(Repository.generateUuid(), d.eventId, d.name, d.description, d.logoUrl, d.landingUrl, d.siteUrl, d.place, d.team.filter(!_.name.isEmpty), d.level, d.sponsor, Utils.toList(d.tags), Utils.toList(d.images), None, new DateTime(), new DateTime())
  def fromModel(d: Exponent): ExponentData = ExponentData(d.eventId, d.name, d.description, d.logoUrl, d.landingUrl, d.siteUrl, d.place, d.team, d.level, d.sponsor, Utils.fromList(d.tags), Utils.fromList(d.images))
  def merge(m: Exponent, d: ExponentData): Exponent = toModel(d).copy(uuid = m.uuid, source = m.source, created = m.created)
}
