package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper

case class ItemType(value: String) extends AnyVal with tString {
  def unwrap: String = this.value
}
object ItemType extends tStringHelper[ItemType] {
  def build(str: String): Either[String, ItemType] = Right(ItemType(str)) // TODO : add validation
  val events = ItemType("events")
  val attendees = ItemType("attendees")
  val exponents = ItemType("exponents")
  val sessions = ItemType("sessions")
  val all = List(events, attendees, exponents, sessions)
}
