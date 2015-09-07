package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper

case class EventLocation(value: String) extends AnyVal with tString {
  def unwrap: String = this.value
}
object EventLocation extends tStringHelper[EventLocation] {
  def build(str: String): Either[String, EventLocation] = Right(EventLocation(str))
}
