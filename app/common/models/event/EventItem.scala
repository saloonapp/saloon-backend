package common.models.event

import common.models.values.UUID

trait EventItem {
  val uuid: UUID
  val name: String
  def getType(): String = {
    this match {
      case _: Event => Event.className
      case _: Session => Session.className
      case _: Exponent => Exponent.className
      case _ => "Unknown"
    }
  }
}
