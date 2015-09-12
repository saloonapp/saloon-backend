package backend.services

import tools.models.GenericEventFull
import common.models.values._
import common.models.values.typed._
import common.models.event._
import common.models.user.OrganizationId
import common.repositories.event.EventRepository
import common.repositories.event.AttendeeRepository
import common.repositories.event.ExponentRepository
import common.repositories.event.SessionRepository
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WS
import play.api.Play.current
import play.api.mvc.Result
import play.api.mvc.Results._
import org.joda.time.DateTime

object EventImport {

  def fetchGenericEvent(url: WebsiteUrl)(block: GenericEventFull => Future[Result]): Future[Result] = {
    WS.url(url.unwrap).get().flatMap { response =>
      response.json.asOpt[GenericEventFull].map { fullEvent =>
        block(fullEvent)
      }.getOrElse {
        Future(Redirect(backend.controllers.admin.routes.Events.urlImport()).flashing("error" -> s"Url ${url.unwrap} does not return an GenericEventFull instance..."))
      }
    }
  }

  def create(eventFull: GenericEventFull, organizationId: OrganizationId, importUrl: WebsiteUrl): Future[EventId] = {
    val (event, attendees, exponents, sessions) = build(eventFull, organizationId, importUrl)
    for {
      eventRes <- EventRepository.insert(event)
      attendeesRes <- AttendeeRepository.bulkInsert(attendees)
      exponentsRes <- ExponentRepository.bulkInsert(exponents)
      sessionsRes <- SessionRepository.bulkInsert(sessions)
    } yield {
      event.uuid
    }
  }

  /*
   * Private methods
   */

  private def build(eventFull: GenericEventFull, organizationId: OrganizationId, importUrl: WebsiteUrl): (Event, List[Attendee], List[Exponent], List[Session]) = {
    val now = new DateTime()

    val event = Event(
      EventId.generate(),
      organizationId,
      FullName(eventFull.event.name),
      TextMultiline(""),
      TextHTML(""),
      EventImages(
        ImageUrl(""),
        ImageUrl("")),
      EventInfo(
        WebsiteUrl(""),
        eventFull.event.start,
        eventFull.event.end,
        Address("", "", "", ""),
        Link("", ""),
        EventInfoSocial(EventInfoSocialTwitter(None, None))),
      EventEmail(None),
      EventConfig(None, Map(), None),
      EventMeta(List(), EventStatus.draft, Some(importUrl.unwrap), Some(eventFull.event.source), now, now))

    val attendees = eventFull.attendees.map { attendee =>
      Attendee(
        AttendeeId.generate(),
        event.uuid,
        FullName.build(FirstName(attendee.firstName), LastName(attendee.lastName)),
        TextMultiline(attendee.description),
        TextHTML(attendee.descriptionHTML),
        AttendeeImages(
          ImageUrl(attendee.avatar)),
        AttendeeInfo(
          AttendeeRole(attendee.role),
          Genre(""),
          FirstName(attendee.firstName),
          LastName(attendee.lastName),
          None,
          Email(""),
          PhoneNumber(""),
          Address("", "", "", ""),
          JobTitle(""),
          CompanyName(attendee.company),
          attendee.siteUrl.map(url => WebsiteUrl(url))),
        None,
        AttendeeSocial(None, None, attendee.twitterUrl.map(url => WebsiteUrl(url)), None, None, None),
        List(),
        AttendeeMeta(Some(attendee.source), now, now))
    }

    val exponents = eventFull.exponents.map { exponent =>
      Exponent(
        ExponentId.generate(),
        event.uuid,
        None,
        FullName(exponent.name),
        TextMultiline(""),
        TextHTML(""),
        ExponentImages(
          ImageUrl(""),
          ImageUrl("")),
        ExponentInfo(
          WebsiteUrl(""),
          EventLocation(""),
          findAttendeeIds(eventFull.exponentTeam.get(exponent.source.ref), attendees),
          None),
        ExponentConfig(false),
        ExponentMeta(Some(exponent.source), now, now))
    }

    val sessions = eventFull.sessions.map { session =>
      Session(
        SessionId.generate(),
        event.uuid,
        FullName(session.name),
        TextMultiline(session.description),
        TextHTML(session.descriptionHTML),
        SessionImages(
          ImageUrl("")),
        SessionInfo(
          session.format,
          session.theme,
          EventLocation(session.place),
          session.start,
          session.end,
          findAttendeeIds(eventFull.sessionSpeakers.get(session.source.ref), attendees),
          None,
          None),
        SessionMeta(Some(session.source), now, now))
    }

    (event, attendees, exponents, sessions)
  }

  private def findAttendeeIds(refs: Option[List[String]], attendees: List[Attendee]): List[AttendeeId] = {
    refs.map {
      _.map { attendeeRef =>
        attendees.find(_.meta.source.map(_.ref == attendeeRef).getOrElse(false))
      }.flatten.map(_.uuid)
    }.getOrElse(List())
  }

}
