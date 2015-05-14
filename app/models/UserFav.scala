package models

import org.joda.time.DateTime
import play.api.libs.json.Json

case class UserFav(
  elt: String,
  eltId: String,
  eventId: String,
  userId: String,
  created: DateTime = new DateTime())
object UserFav {
  implicit val format = Json.format[UserFav]
}
