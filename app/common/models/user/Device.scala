package common.models.user

import common.repositories.Repository
import play.api.data.Forms._
import play.api.libs.json.Json
import org.joda.time.DateTime

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

// mapping object for Device Form
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
