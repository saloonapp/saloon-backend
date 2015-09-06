package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper

case class EventLocation(val value: String) extends AnyVal with tString {
  def unwrap: String = this.value
}
object EventLocation extends tStringHelper[EventLocation] {
  def build(str: String): Option[EventLocation] = Some(EventLocation(str))
}
