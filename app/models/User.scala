package models

import infrastructure.repository.common.Repository
import org.joda.time.DateTime
import play.api.libs.json.Json

case class User(
  uuid: String,
  device: Device,
  created: DateTime,
  updated: DateTime)
object User {
  implicit val format = Json.format[User]
  def fromDevice(device: Device): User = User(Repository.generateUuid(), device, new DateTime(), new DateTime())
}
