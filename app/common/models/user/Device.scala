package common.models.user

import common.models.utils.tString
import common.models.utils.tStringHelper
import common.models.values.UUID
import common.models.values.typed._
import play.api.data.Forms._
import play.api.libs.json.Json
import org.joda.time.DateTime

case class DeviceId(val id: String) extends AnyVal with tString with UUID {
  def unwrap: String = this.id
}
object DeviceId extends tStringHelper[DeviceId] {
  def generate(): DeviceId = DeviceId(UUID.generate())
  def build(str: String): Option[DeviceId] = UUID.toUUID(str).map(id => DeviceId(id))
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
  uuid: DeviceId,
  info: DeviceInfo,
  pushId: Option[String],
  saloonMemo: TextMultiline,
  meta: DeviceMeta)
object DeviceInfo {
  implicit val format = Json.format[DeviceInfo]
}
object Device {
  implicit val formatDeviceMeta = Json.format[DeviceMeta]
  implicit val format = Json.format[Device]
  def fromInfo(info: DeviceInfo): Device = Device(DeviceId.generate(), info, None, TextMultiline(""), DeviceMeta(new DateTime(), new DateTime()))
}

// mapping object for Device Form
case class DeviceData(
  info: DeviceInfo,
  pushId: Option[String],
  saloonMemo: TextMultiline)
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
    "saloonMemo" -> of[TextMultiline])(DeviceData.apply)(DeviceData.unapply)

  def toModel(d: DeviceData): Device = Device(DeviceId.generate(), d.info, d.pushId, d.saloonMemo, DeviceMeta(new DateTime(), new DateTime()))
  def fromModel(d: Device): DeviceData = DeviceData(d.info, d.pushId, d.saloonMemo)
  def merge(m: Device, d: DeviceData): Device = toModel(d).copy(uuid = m.uuid, meta = DeviceMeta(m.meta.created, new DateTime()))
}
