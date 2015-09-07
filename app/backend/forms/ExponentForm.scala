package backend.forms

import common.models.utils.tStringConstraints._
import common.models.values.typed._
import common.models.event.EventId
import common.models.event.AttendeeId
import common.models.event.Exponent
import common.models.event.ExponentId
import common.models.event.ExponentImages
import common.models.event.ExponentInfo
import common.models.event.ExponentConfig
import common.models.event.ExponentMeta
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json._

case class ExponentCreateData(
  eventId: EventId,
  name: FullName,
  descriptionHTML: TextHTML,
  logo: ImageUrl,
  landing: ImageUrl,
  website: WebsiteUrl,
  place: EventLocation,
  sponsorLevel: Option[Int])
object ExponentCreateData {
  val fields = mapping(
    "eventId" -> of[EventId].verifying(nonEmpty),
    "name" -> of[FullName].verifying(nonEmpty),
    "descriptionHTML" -> of[TextHTML],
    "logo" -> of[ImageUrl],
    "landing" -> of[ImageUrl],
    "website" -> of[WebsiteUrl],
    "place" -> of[EventLocation],
    "sponsorLevel" -> optional(number))(ExponentCreateData.apply)(ExponentCreateData.unapply)

  def toMeta(d: ExponentCreateData): ExponentMeta = ExponentMeta(None, new DateTime(), new DateTime())
  def toConfig(d: ExponentCreateData): ExponentConfig = ExponentConfig(false)
  def toInfo(d: ExponentCreateData, team: List[AttendeeId]): ExponentInfo = ExponentInfo(d.website, d.place, team, d.sponsorLevel)
  def toImages(d: ExponentCreateData): ExponentImages = ExponentImages(d.logo, d.landing)
  def toModel(d: ExponentCreateData): Exponent = Exponent(ExponentId.generate(), d.eventId, None, d.name, d.descriptionHTML.toPlainText, d.descriptionHTML, toImages(d), toInfo(d, List()), toConfig(d), toMeta(d))
  def fromModel(d: Exponent): ExponentCreateData = ExponentCreateData(d.eventId, d.name, d.descriptionHTML, d.images.logo, d.images.landing, d.info.website, d.info.place, d.info.sponsorLevel)
  def merge(m: Exponent, d: ExponentCreateData): Exponent = m.copy(name = d.name, description = d.descriptionHTML.toPlainText, descriptionHTML = d.descriptionHTML, images = toImages(d), info = toInfo(d, m.info.team), meta = m.meta.copy(updated = new DateTime()))
}