package common.models.event

import common.models.values.UUID
import common.models.values.typed.FullName
import common.models.values.typed.ItemType

trait EventItem {
  val uuid: UUID
  val name: FullName
  def getType(): String = {
    this match {
      case _: Event => ItemType.events.unwrap
      case _: Attendee => ItemType.attendees.unwrap
      case _: Exponent => ItemType.exponents.unwrap
      case _: Session => ItemType.sessions.unwrap
      case _ => "Unknown"
    }
  }
}
