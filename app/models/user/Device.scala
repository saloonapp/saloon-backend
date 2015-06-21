package models.user

import common.infrastructure.repository.Repository
import play.api.data.Forms._
import play.api.libs.json.Json
import org.joda.time.DateTime

case class DeviceOld(
  uuid: String,
  platform: String,
  manufacturer: String,
  model: String,
  version: String,
  cordova: String)
case class PushOld(
  uuid: String,
  platform: String)
case class UserOld(
  uuid: String,
  device: DeviceOld,
  push: Option[PushOld],
  saloonMemo: String,
  created: DateTime,
  updated: DateTime) {
  def transform(): Device = Device(
    this.uuid,
    DeviceInfo(
      this.device.uuid,
      this.device.platform,
      this.device.manufacturer,
      this.device.model,
      this.device.version,
      this.device.cordova),
    this.push.map(_.uuid),
    this.saloonMemo,
    DeviceMeta(
      this.created,
      this.updated))
}
object UserOld {
  implicit val formatDeviceOld = Json.format[DeviceOld]
  implicit val formatPushOld = Json.format[PushOld]
  implicit val format = Json.format[UserOld]
  def fromDevice(device: DeviceOld): UserOld = UserOld(Repository.generateUuid(), device, None, "", new DateTime(), new DateTime())
}

// mapping object for User Form
case class UserDataOld(
  device: DeviceOld,
  push: Option[PushOld],
  saloonMemo: String)
object UserDataOld {
  val fields = mapping(
    "device" -> mapping(
      "uuid" -> nonEmptyText,
      "platform" -> text,
      "manufacturer" -> text,
      "model" -> text,
      "version" -> text,
      "cordova" -> text)(DeviceOld.apply)(DeviceOld.unapply),
    "push" -> optional(mapping(
      "uuid" -> text,
      "platform" -> text)(PushOld.apply)(PushOld.unapply)),
    "saloonMemo" -> text)(UserDataOld.apply)(UserDataOld.unapply)

  def toModel(d: UserDataOld): UserOld = UserOld(Repository.generateUuid(), d.device, d.push, d.saloonMemo, new DateTime(), new DateTime())
  def fromModel(m: UserOld): UserDataOld = UserDataOld(m.device, m.push, m.saloonMemo)
  def merge(m: UserOld, d: UserDataOld): UserOld = m.copy(device = d.device, push = d.push, saloonMemo = d.saloonMemo, updated = new DateTime())
}

case class DeviceInfo(
  uuid: String,
  platform: String,
  manufacturer: String,
  model: String,
  version: String,
  cordova: String)
case class DeviceMeta(
  created: DateTime,
  updated: DateTime)
case class Device(
  uuid: String,
  info: DeviceInfo,
  pushId: Option[String],
  saloonMemo: String,
  meta: DeviceMeta)
object DeviceInfo {
  implicit val format = Json.format[DeviceInfo]
}
object Device {
  implicit val formatDeviceMeta = Json.format[DeviceMeta]
  implicit val format = Json.format[Device]
  def fromInfo(info: DeviceInfo): Device = Device(Repository.generateUuid(), info, None, "", DeviceMeta(new DateTime(), new DateTime()))
}

// mapping object for User Form
case class DeviceData(
  info: DeviceInfo,
  pushId: Option[String],
  saloonMemo: String)
object DeviceData {
  val fields = mapping(
    "info" -> mapping(
      "uuid" -> text,
      "platform" -> text,
      "manufacturer" -> text,
      "model" -> text,
      "version" -> text,
      "cordova" -> text)(DeviceInfo.apply)(DeviceInfo.unapply),
    "push" -> optional(text),
    "saloonMemo" -> text)(DeviceData.apply)(DeviceData.unapply)

  def toModel(d: DeviceData): Device = Device(Repository.generateUuid(), d.info, d.pushId, d.saloonMemo, DeviceMeta(new DateTime(), new DateTime()))
  def fromModel(d: Device): DeviceData = DeviceData(d.info, d.pushId, d.saloonMemo)
  def merge(m: Device, d: DeviceData): Device = toModel(d).copy(uuid = m.uuid, meta = DeviceMeta(m.meta.created, new DateTime()))
}
