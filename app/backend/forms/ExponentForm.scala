package backend.forms

import common.models.event.Exponent
import common.models.event.ExponentImages
import common.models.event.ExponentInfo
import common.models.event.ExponentConfig
import common.models.event.ExponentMeta
import common.repositories.Repository
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json._

case class ExponentCreateData(
  eventId: String,
  name: String,
  descriptionHTML: String,
  logo: String,
  landing: String,
  website: String,
  place: String,
  team: List[String],
  level: Option[Int])
object ExponentCreateData {
  val fields = mapping(
    "eventId" -> nonEmptyText,
    "name" -> nonEmptyText,
    "descriptionHTML" -> text,
    "logo" -> text,
    "landing" -> text,
    "website" -> text,
    "place" -> text,
    "team" -> list(text),
    "level" -> optional(number))(ExponentCreateData.apply)(ExponentCreateData.unapply)

  def toMeta(d: ExponentCreateData): ExponentMeta = ExponentMeta(None, new DateTime(), new DateTime())
  def toConfig(d: ExponentCreateData): ExponentConfig = ExponentConfig(false)
  def toInfo(d: ExponentCreateData): ExponentInfo = ExponentInfo(d.website, d.place, d.team, d.level, d.level.isDefined)
  def toImages(d: ExponentCreateData): ExponentImages = ExponentImages(d.logo, d.landing)
  def toModel(d: ExponentCreateData): Exponent = Exponent(Repository.generateUuid(), d.eventId, d.name, d.descriptionHTML, toImages(d), toInfo(d), toConfig(d), toMeta(d))
  def fromModel(d: Exponent): ExponentCreateData = ExponentCreateData(d.eventId, d.name, d.description, d.images.logo, d.images.landing, d.info.website, d.info.place, d.info.team, d.info.level)
  def merge(m: Exponent, d: ExponentCreateData): Exponent = m.copy(name = d.name, description = d.descriptionHTML, images = toImages(d), info = toInfo(d), meta = m.meta.copy(updated = new DateTime()))
}