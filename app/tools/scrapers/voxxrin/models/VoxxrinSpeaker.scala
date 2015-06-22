package tools.scrapers.voxxrin.models

import common.Utils
import models.event.Person
import models.event.PersonSocial
import tools.scrapers.voxxrin.VoxxrinApi
import play.api.libs.json._

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
  def toSpeaker(): Person = Person(
    this.name,
    this.bio.orElse(this.__description).map(html => Utils.htmlToText(html)).getOrElse(""),
    this.__company.getOrElse(""),
    this.pictureURI.orElse(this.__pictureUrl).map(VoxxrinApi.baseUrl + _).getOrElse(""),
    None,
    "",
    PersonSocial(
      None,
      None,
      this.__twitter.map(Utils.toTwitterAccount),
      None,
      None))
}
object VoxxrinSpeaker {
  implicit val format = Json.format[VoxxrinSpeaker]
}
