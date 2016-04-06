package backend.forms

import common.models.event.{ExponentId, SessionId, AttendeeId}
import play.api.data.Forms._

case class EventUpdateData(
  updateEvent: Boolean,
  createdAttendees: List[(String, Boolean)],
  deletedAttendees: List[(AttendeeId, Boolean)],
  updatedAttendees: List[(AttendeeId, Boolean)],
  createdSessions: List[(String, Boolean)],
  deletedSessions: List[(SessionId, Boolean)],
  updatedSessions: List[(SessionId, Boolean)],
  createdExponents: List[(String, Boolean)],
  deletedExponents: List[(ExponentId, Boolean)],
  updatedExponents: List[(ExponentId, Boolean)])
object EventUpdateData {
  val fields = mapping(
    "updateEvent" -> boolean,
    "createdAttendees" -> list(tuple(
      "id" -> text,
      "update" -> boolean)),
    "deletedAttendees" -> list(tuple(
      "id" -> of[AttendeeId],
      "update" -> boolean)),
    "updatedAttendees" -> list(tuple(
      "id" -> of[AttendeeId],
      "update" -> boolean)),
    "createdSessions" -> list(tuple(
      "id" -> text,
      "update" -> boolean)),
    "deletedSessions" -> list(tuple(
      "id" -> of[SessionId],
      "update" -> boolean)),
    "updatedSessions" -> list(tuple(
      "id" -> of[SessionId],
      "update" -> boolean)),
    "createdExponents" -> list(tuple(
      "id" -> text,
      "update" -> boolean)),
    "deletedExponents" -> list(tuple(
      "id" -> of[ExponentId],
      "update" -> boolean)),
    "updatedExponents" -> list(tuple(
      "id" -> of[ExponentId],
      "update" -> boolean))
  )(EventUpdateData.apply)(EventUpdateData.unapply)
}
