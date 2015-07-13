package tools.scrapers.voxxrin.models

import common.Utils
import common.models.values.DataSource
import common.models.event.Attendee
import common.models.event.AttendeeImages
import common.models.event.AttendeeInfo
import common.models.event.AttendeeSocial
import common.models.event.AttendeeMeta
import common.repositories.Repository
import tools.scrapers.voxxrin.VoxxrinApi
import play.api.libs.json._
import org.joda.time.DateTime

case class VoxxrinSpeaker(
  id: String,
  name: String,
  pictureURI: Option[String],
  bio: Option[String],
  __lastName: Option[String],
  __firstName: Option[String],
  __pictureUrl: Option[String],
  __description: Option[String],
  __company: Option[String],
  __twitter: Option[String],
  uri: Option[String],
  __href: Option[String],
  lastmodified: Option[Long]) {
  def merge(s: VoxxrinSpeaker)(implicit w: Format[VoxxrinSpeaker]): VoxxrinSpeaker = (Json.toJson(this).as[JsObject] ++ Json.toJson(s).as[JsObject]).as[VoxxrinSpeaker]
  /*def toSpeaker(eventId: String): Attendee =
    Attendee(
      Repository.generateUuid(),
      eventId,
      this.name,
      this.bio.orElse(this.__description).map(html => Utils.htmlToText(html)).getOrElse(""),
      AttendeeImages(
        this.pictureURI.orElse(this.__pictureUrl).map(VoxxrinApi.baseUrl + _).getOrElse("")),
      AttendeeInfo(
        "speaker",
        "",
        this.__company.getOrElse(""),
        this.uri),
      AttendeeSocial(
        None,
        None,
        this.__twitter.map(Utils.toTwitterAccount),
        None,
        None),
      AttendeeMeta(
        Some(DataSource(this.name, "Voxxrin API", VoxxrinApi.baseUrl + this.uri)),
        new DateTime(),
        new DateTime()))*/
}
object VoxxrinSpeaker {
  implicit val format = Json.format[VoxxrinSpeaker]
}
