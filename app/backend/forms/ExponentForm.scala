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
import org.jsoup.Jsoup

case class ExponentCreateData(
  eventId: String,
  name: String,
  descriptionHTML: String,
  logo: String,
  landing: String,
  website: String,
  place: String,
  sponsorLevel: Option[Int])
object ExponentCreateData {
  val fields = mapping(
    "eventId" -> nonEmptyText,
    "name" -> nonEmptyText,
    "descriptionHTML" -> text,
    "logo" -> text,
    "landing" -> text,
    "website" -> text,
    "place" -> text,
    "sponsorLevel" -> optional(number))(ExponentCreateData.apply)(ExponentCreateData.unapply)

  def toMeta(d: ExponentCreateData): ExponentMeta = ExponentMeta(None, new DateTime(), new DateTime())
  def toConfig(d: ExponentCreateData): ExponentConfig = ExponentConfig(false)
  def toInfo(d: ExponentCreateData, team: List[String]): ExponentInfo = ExponentInfo(d.website, d.place, team, d.sponsorLevel)
  def toImages(d: ExponentCreateData): ExponentImages = ExponentImages(d.logo, d.landing)
  def toModel(d: ExponentCreateData): Exponent = Exponent(Repository.generateUuid(), d.eventId, None, d.name, Jsoup.parse(d.descriptionHTML).text(), d.descriptionHTML, toImages(d), toInfo(d, List()), toConfig(d), toMeta(d))
  def fromModel(d: Exponent): ExponentCreateData = ExponentCreateData(d.eventId, d.name, d.descriptionHTML, d.images.logo, d.images.landing, d.info.website, d.info.place, d.info.sponsorLevel)
  def merge(m: Exponent, d: ExponentCreateData): Exponent = m.copy(name = d.name, description = Jsoup.parse(d.descriptionHTML).text(), descriptionHTML = d.descriptionHTML, images = toImages(d), info = toInfo(d, m.info.team), meta = m.meta.copy(updated = new DateTime()))
}