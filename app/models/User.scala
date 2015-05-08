package models

import infrastructure.repository.common.Repository
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json.Json

case class User(
  uuid: String,
  device: Device,
  saloonMemo: Option[String],
  created: DateTime,
  updated: DateTime)
object User {
  implicit val format = Json.format[User]
  def fromDevice(device: Device): User = User(Repository.generateUuid(), device, None, new DateTime(), new DateTime())
}

// mapping object for User Form
case class UserData(
  device: Device,
  saloonMemo: Option[String])
object UserData {
  implicit val format = Json.format[UserData]
  val fields = mapping(
    "device" -> Device.fields,
    "saloonMemo" -> optional(text))(UserData.apply)(UserData.unapply)

  def toModel(d: UserData): User = User(Repository.generateUuid(), d.device, d.saloonMemo, new DateTime(), new DateTime())
  def fromModel(m: User): UserData = UserData(m.device, m.saloonMemo)
  def merge(m: User, d: UserData): User = m.copy(device = d.device, saloonMemo = d.saloonMemo, updated = new DateTime())
}
