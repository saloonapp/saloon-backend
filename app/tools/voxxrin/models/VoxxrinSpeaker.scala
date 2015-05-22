package tools.voxxrin.models

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
}
object VoxxrinSpeaker {
  implicit val format = Json.format[VoxxrinSpeaker]
}
