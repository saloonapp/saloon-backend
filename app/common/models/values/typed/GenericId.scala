package common.models.values.typed

import common.models.utils.tString
import common.models.utils.tStringHelper
import common.models.values.UUID
import common.models.event.EventId
import common.models.event.AttendeeId
import common.models.event.ExponentId
import common.models.event.SessionId

case class GenericId(id: String) extends AnyVal with tString with UUID {
  def unwrap: String = this.id

  def toEventId: EventId = EventId(this.id)
  def toAttendeeId: AttendeeId = AttendeeId(this.id)
  def toExponentId: ExponentId = ExponentId(this.id)
  def toSessionId: SessionId = SessionId(this.id)
}
object GenericId extends tStringHelper[GenericId] {
  def generate(): GenericId = GenericId(UUID.generate())
  def build(str: String): Either[String, GenericId] = UUID.toUUID(str).right.map(id => GenericId(id)).left.map(_ + " for GenericId")

  implicit def fromUUID(uuid: UUID): GenericId = GenericId(uuid.unwrap)
  implicit def fromEventId(id: EventId): GenericId = GenericId(id.unwrap)
  implicit def fromAttendeeId(id: AttendeeId): GenericId = GenericId(id.unwrap)
  implicit def fromExponentId(id: ExponentId): GenericId = GenericId(id.unwrap)
  implicit def fromSessionId(id: SessionId): GenericId = GenericId(id.unwrap)
}
