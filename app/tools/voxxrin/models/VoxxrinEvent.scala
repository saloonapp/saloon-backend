package tools.voxxrin.models

import models.Event
import models.Session
import models.Address
import infrastructure.repository.common.Repository
import org.joda.time.DateTime
import play.api.libs.json.Json

case class VoxxrinEventDay(
  id: String,
  name: String,
  uri: String,
  nbPresentations: Int)
object VoxxrinEventDay {
  implicit val format = Json.format[VoxxrinEventDay]
}

case class VoxxrinEvent(
  id: String,
  title: String,
  subtitle: Option[String],
  description: Option[String],
  location: Option[String],
  dates: String,
  from: String,
  to: String,
  timezone: Option[String],
  nbPresentations: Int,
  days: List[VoxxrinEventDay],
  schedule: Option[List[VoxxrinSession]],
  enabled: Boolean,
  lastmodified: Long) {
  def toEvent(): (Event, List[Session]) = {
    val eventId = Repository.generateUuid()

    val event = Event(
      eventId,
      "",
      this.title,
      this.description.getOrElse(""),
      VoxxrinEvent.parseDate(this.from),
      VoxxrinEvent.parseDate(this.to),
      location.map(l => Address(l)),
      None,
      false,
      new DateTime(),
      new DateTime())

    val sessions = this.schedule.map {
      _.map { s => s.toSession(eventId) }
    }.getOrElse(List[Session]())

    (event, sessions)
  }
}
object VoxxrinEvent {
  implicit val format = Json.format[VoxxrinEvent]
  def parseDate(date: String): Option[DateTime] = if (date.isEmpty) None else Some(DateTime.parse(date))
}
