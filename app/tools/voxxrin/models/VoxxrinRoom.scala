package tools.voxxrin.models

import models.Place
import play.api.libs.json.Json

case class VoxxrinRoom(
  id: String,
  name: String,
  uri: String,
  lastmodified: Option[Long]) {
  def toPlace(): String = this.name
}
object VoxxrinRoom {
  implicit val format = Json.format[VoxxrinRoom]
}
