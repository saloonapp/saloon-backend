package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper

case class EventStatus(value: String) extends AnyVal with tString {
  def unwrap: String = this.value
}
object EventStatus extends tStringHelper[EventStatus] {
  def build(str: String): Either[String, EventStatus] = Right(EventStatus(str)) // TODO : add validation
  val draft = EventStatus("draft")
  val publishing = EventStatus("publishing")
  val published = EventStatus("published")
  val all = List(draft, publishing, published)
}
