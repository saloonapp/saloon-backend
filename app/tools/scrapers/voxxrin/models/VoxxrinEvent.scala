package tools.scrapers.voxxrin.models

import common.infrastructure.repository.Repository
import models.Event
import models.EventImages
import models.EventInfo
import models.EventInfoSocial
import models.EventInfoSocialTwitter
import models.EventEmail
import models.EventConfig
import models.EventMeta
import models.Session
import models.Address
import models.DataSource
import models.Link
import tools.scrapers.voxxrin.VoxxrinApi
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
  def toEvent(eventId: String = Repository.generateUuid()): (Event, List[Session]) = {
    val event = Event(
      eventId,
      this.title,
      this.description.getOrElse(""),
      EventImages("", ""),
      EventInfo(
        "",
        VoxxrinEvent.parseDate(this.from),
        VoxxrinEvent.parseDate(this.to),
        Address("", location.getOrElse(""), "", ""), // TODO : parse address
        Link("", ""),
        EventInfoSocial(EventInfoSocialTwitter(None, None))),
      EventEmail(None),
      EventConfig(None, false),
      EventMeta(
        List(),
        Some("/api/v1/tools/scrapers/events/voxxrin/" + this.id + "/formated"),
        Some(DataSource(this.id, Some("Voxxrin API"), VoxxrinApi.eventUrl(this.id))),
        new DateTime(),
        new DateTime()))

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
