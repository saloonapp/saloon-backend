package tools.api.devoxx.models

import play.api.libs.json.Json

case class DevoxxSpeakerTalk(
  id: String,
  title: String,
  talkType: String,
  track: String,
  links: List[Link])
case class DevoxxSpeaker(
  uuid: String,
  firstName: String,
  lastName: String,
  avatarURL: Option[String],
  bio: String,
  bioAsHtml: String,
  blog: Option[String],
  twitter: Option[String],
  company: Option[String],
  companyLogoUrl: Option[String],
  zipCode: String,
  lang: Option[String],
  acceptedTalks: List[DevoxxSpeakerTalk],
  sourceUrl: Option[String])
object DevoxxSpeaker {
  implicit val formatDevoxxSpeakerTalk = Json.format[DevoxxSpeakerTalk]
  implicit val format = Json.format[DevoxxSpeaker]
}
