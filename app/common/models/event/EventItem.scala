package common.models.event

import common.models.values.UUID
import common.models.values.typed.FullName
import common.models.values.typed.ItemType

trait EventItem {
  val uuid: UUID
  val name: FullName
  def getType(): ItemType = this match {
    case _: Event => ItemType.events
    case _: Attendee => ItemType.attendees
    case _: Exponent => ItemType.exponents
    case _: Session => ItemType.sessions
    case _ => ItemType("Unknown")
  }

  def toEvent: Option[Event] = this match {
    case e: Event => Some(e)
    case _ => None
  }
  def toAttendee: Option[Attendee] = this match {
    case a: Attendee => Some(a)
    case _ => None
  }
  def toExponent: Option[Exponent] = this match {
    case e: Exponent => Some(e)
    case _ => None
  }
  def toSession: Option[Session] = this match {
    case s: Session => Some(s)
    case _ => None
  }
  def isEvent: Boolean = this.toEvent.isDefined
  def isAttendee: Boolean = this.toAttendee.isDefined
  def isExponent: Boolean = this.toExponent.isDefined
  def isSession: Boolean = this.toSession.isDefined
}
