package backend.services

import tools.models._
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

  def fetchGenericEvent(url: WebsiteUrl): Future[Option[GenericEventFull]] = {
    WS.url(url.unwrap).get().map { response =>
      response.json.asOpt[GenericEventFull]
    }
  }

  def withGenericEvent(url: WebsiteUrl)(block: GenericEventFull => Future[Result]): Future[Result] = {
    fetchGenericEvent(url).flatMap { fullEventOpt =>
      fullEventOpt.map { fullEvent =>
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

  def makeDiff(eventFull: GenericEventFull, event: Event, attendees: List[Attendee], exponents: List[Exponent], sessions: List[Session]): (Event, List[Attendee], List[Attendee], List[(Attendee, Attendee)], List[Exponent], List[Exponent], List[(Exponent, Exponent)], List[Session], List[Session], List[(Session, Session)]) = {
    val (newEvent, newAttendees, newExponents, newSessions) = build(eventFull, event, attendees, exponents, sessions)
    val updatedEvent = event.merge(newEvent)
    val (createdAttendees, deletedAttendees, updatedAttendees) = attendeeDiff(attendees, newAttendees)
    val (createdExponents, deletedExponents, updatedExponents) = exponentDiff(exponents, newExponents)
    val (createdSessions, deletedSessions, updatedSessions) = EventImport.sessionDiff(sessions, newSessions)
    (updatedEvent, createdAttendees, deletedAttendees, updatedAttendees, createdExponents, deletedExponents, updatedExponents, createdSessions, deletedSessions, updatedSessions)
  }

  /*
   * Private methods
   */

  private def build(eventFull: GenericEventFull, organizationId: OrganizationId, importUrl: WebsiteUrl): (Event, List[Attendee], List[Exponent], List[Session]) = {
    val now = new DateTime()
    val event = build(eventFull.event, EventId.generate(), organizationId, Some(importUrl), now)
    val attendees = eventFull.attendees.map { attendee => build(attendee, AttendeeId.generate(), event.uuid, now) }
    val exponents = eventFull.exponents.map { exponent => build(exponent, ExponentId.generate(), event.uuid, now, eventFull.exponentTeam, attendees) }
    val sessions = eventFull.sessions.map { session => build(session, SessionId.generate(), event.uuid, now, eventFull.sessionSpeakers, attendees) }
    (event, attendees, exponents, sessions)
  }

  private def build(eventFull: GenericEventFull, event: Event, attendees: List[Attendee], exponents: List[Exponent], sessions: List[Session]): (Event, List[Attendee], List[Exponent], List[Session]) = {
    val now = new DateTime()
    val newEvent = build(eventFull.event, event.uuid, event.ownerId, event.meta.refreshUrl, now)
    val newAttendees = eventFull.attendees.map { attendee =>
      val attendeeId: AttendeeId = findAttendeeByRef(attendees, attendee.source.ref).map(_.uuid).getOrElse(AttendeeId.generate())
      build(attendee, attendeeId, event.uuid, now)
    }
    val newExponents = eventFull.exponents.map { exponent =>
      val exponentId: ExponentId = findExponentByRef(exponents, exponent.source.ref).map(_.uuid).getOrElse(ExponentId.generate())
      build(exponent, exponentId, event.uuid, now, eventFull.exponentTeam, newAttendees)
    }
    val newSessions = eventFull.sessions.map { session =>
      val sessionId: SessionId = findSessionByRef(sessions, session.source.ref).map(_.uuid).getOrElse(SessionId.generate())
      build(session, sessionId, event.uuid, now, eventFull.sessionSpeakers, newAttendees)
    }
    (newEvent, newAttendees, newExponents, newSessions)
  }

  private def build(event: GenericEvent, eventId: EventId, organizationId: OrganizationId, importUrl: Option[WebsiteUrl], now: DateTime): Event = Event(
    eventId,
    organizationId,
    FullName(event.name),
    TextMultiline(""),
    TextHTML(""),
    EventImages(
      ImageUrl(""),
      ImageUrl("")),
    EventInfo(
      WebsiteUrl(""),
      event.start,
      event.end,
      Address("", "", "", ""),
      Link("", ""),
      EventInfoSocial(EventInfoSocialTwitter(None, None))),
    EventEmail(None),
    EventConfig(None, Map(), None),
    EventMeta(List(), EventStatus.draft, importUrl, Some(event.source), now, now))

  private def build(attendee: GenericAttendee, attendeeId: AttendeeId, eventId: EventId, now: DateTime): Attendee = Attendee(
    attendeeId,
    eventId,
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

  private def build(exponent: GenericExponent, exponentId: ExponentId, eventId: EventId, now: DateTime, exponentTeam: Map[String, List[String]], attendees: List[Attendee]): Exponent = Exponent(
    exponentId,
    eventId,
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
      findAttendeeIds(exponentTeam.get(exponent.source.ref), attendees),
      None),
    ExponentConfig(false),
    ExponentMeta(Some(exponent.source), now, now))

  private def build(session: GenericSession, sessionId: SessionId, eventId: EventId, now: DateTime, sessionSpeakers: Map[String, List[String]], attendees: List[Attendee]): Session = Session(
    sessionId,
    eventId,
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
      findAttendeeIds(sessionSpeakers.get(session.source.ref), attendees),
      None,
      None),
    SessionMeta(Some(session.source), now, now))

  private def findAttendeeIds(refs: Option[List[String]], attendees: List[Attendee]): List[AttendeeId] = {
    refs.map {
      _.map { attendeeRef =>
        attendees.find(_.meta.source.map(_.ref == attendeeRef).getOrElse(false))
      }.flatten.map(_.uuid)
    }.getOrElse(List())
  }

  private def findAttendeeByRef(attendees: List[Attendee], ref: String): Option[Attendee] = attendees.find(_.meta.source.map(_.ref == ref).getOrElse(false))
  private def findExponentByRef(exponents: List[Exponent], ref: String): Option[Exponent] = exponents.find(_.meta.source.map(_.ref == ref).getOrElse(false))
  private def findSessionByRef(sessions: List[Session], ref: String): Option[Session] = sessions.find(_.meta.source.map(_.ref == ref).getOrElse(false))

  private def attendeeDiff(oldElts: List[Attendee], newElts: List[Attendee]): (List[Attendee], List[Attendee], List[(Attendee, Attendee)]) = {
    val getRef = (s: Attendee) => s.meta.source.map(_.ref).getOrElse("")
    val merge = (os: Attendee, ns: Attendee) => os.merge(ns)
    val equals = (os: Attendee, ns: Attendee) => os.copy(meta = os.meta.copy(updated = new DateTime(0))) == ns.copy(meta = ns.meta.copy(updated = new DateTime(0)))
    diff(oldElts, newElts, getRef, merge, equals)
  }

  private def exponentDiff(oldElts: List[Exponent], newElts: List[Exponent]): (List[Exponent], List[Exponent], List[(Exponent, Exponent)]) = {
    val getRef = (e: Exponent) => e.meta.source.map(_.ref).getOrElse("")
    val merge = (oe: Exponent, ne: Exponent) => oe.merge(ne)
    val equals = (oe: Exponent, ne: Exponent) => oe.copy(meta = oe.meta.copy(updated = new DateTime(0))) == ne.copy(meta = ne.meta.copy(updated = new DateTime(0)))
    diff(oldElts, newElts, getRef, merge, equals)
  }

  private def sessionDiff(oldElts: List[Session], newElts: List[Session]): (List[Session], List[Session], List[(Session, Session)]) = {
    val getRef = (s: Session) => s.meta.source.map(_.ref).getOrElse("")
    val merge = (os: Session, ns: Session) => os.merge(ns)
    val equals = (os: Session, ns: Session) => os.copy(meta = os.meta.copy(updated = new DateTime(0))) == ns.copy(meta = ns.meta.copy(updated = new DateTime(0)))
    diff(oldElts, newElts, getRef, merge, equals)
  }

  private def diff[A](oldElts: List[A], newElts: List[A], getRef: A => String, merge: (A, A) => A, equals: (A, A) => Boolean): (List[A], List[A], List[(A, A)]) = {
    val createdElts = newElts.filter(ne => oldElts.find(oe => getRef(oe) == getRef(ne)).isEmpty)
    val deletedElts = oldElts.filter(oe => newElts.find(ne => getRef(oe) == getRef(ne)).isEmpty)
    val updatedElts = oldElts
      .map(oe => newElts.find(ne => getRef(oe) == getRef(ne)).map(ne => (oe, ne))).flatten
      .map { case (oe, ne) => (oe, merge(oe, ne)) }
      .filter { case (oe, ne) => !equals(oe, ne) }
    (createdElts, deletedElts, updatedElts)
  }

}
