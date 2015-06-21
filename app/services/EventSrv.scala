package services

import models.event.Event
import models.event.Session
import models.event.Exponent
import models.event.EventItem
import models.user.User
import models.user.UserActionFull
import models.user.SubscribeUserAction
import common.infrastructure.repository.Repository
import infrastructure.repository.EventRepository
import infrastructure.repository.SessionRepository
import infrastructure.repository.ExponentRepository
import infrastructure.repository.EventItemRepository
import infrastructure.repository.UserActionRepository
import infrastructure.repository.UserRepository
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current
import play.api.libs.ws._
import play.api.mvc.RequestHeader
import reactivemongo.core.commands.LastError
import org.joda.time.DateTime

object EventSrv {
  def addMetadata(event: Event): Future[(Event, Int, Int)] = {
    for {
      sessionCount <- SessionRepository.countForEvent(event.uuid)
      exponentCount <- ExponentRepository.countForEvent(event.uuid)
    } yield (event, sessionCount, exponentCount)
  }

  def addMetadata(events: Seq[Event]): Future[Seq[(Event, Int, Int)]] = {
    val uuids = events.map(_.uuid)
    for {
      sessionCounts <- SessionRepository.countForEvents(uuids)
      exponentCounts <- ExponentRepository.countForEvents(uuids)
    } yield {
      events.map { event =>
        (event, sessionCounts.get(event.uuid).getOrElse(0), exponentCounts.get(event.uuid).getOrElse(0))
      }
    }
  }

  def getActions(eventId: String): Future[List[UserActionFull]] = {
    UserActionRepository.findByEvent(eventId).flatMap { actions =>
      for {
        events: Map[String, Event] <- EventRepository.findByUuids(actions.map(_.eventId).flatten.distinct).map(_.map(u => (u.uuid, u)).toMap)
        users: Map[String, User] <- UserRepository.findByUuids(actions.map(_.userId).distinct).map(_.map(u => (u.uuid, u)).toMap)
        items: Map[(String, String), EventItem] <- EventItemRepository.findByUuids(actions.map(a => (a.itemType, a.itemId)).distinct)
      } yield {
        actions.map { a =>
          for {
            event <- a.eventId.flatMap(id => events.get(id))
            user <- users.get(a.userId)
            item <- items.get((a.itemType, a.itemId))
          } yield {
            UserActionFull(event, user, a.action, item)
          }
        }.flatten
      }
    }
  }

  def fetchFullEvent(url: String)(implicit req: RequestHeader): Future[Option[(Event, List[Session], List[Exponent])]] = {
    val realUrl = if (url.startsWith("http")) url else "http://" + req.host + url
    WS.url(realUrl).get().map { response =>
      response.json.asOpt[Event].map { event =>
        val sessions = (response.json \ "sessions").as[List[Session]]
        val exponents = (response.json \ "exponents").as[List[Exponent]]
        (event, sessions, exponents)
      }
    }
  }

  def sessionDiff(oldElts: List[Session], newElts: List[Session]): (List[Session], List[Session], List[(Session, Session)]) = {
    diff(oldElts, newElts, (s: Session) => s.name, (s: Session) => s.meta.source.map(_.ref), (os: Session, ns: Session) => os.merge(ns), (os: Session, ns: Session) => os.copy(meta = os.meta.copy(updated = new DateTime(0))) == ns.copy(meta = ns.meta.copy(updated = new DateTime(0))))
  }

  def exponentDiff(oldElts: List[Exponent], newElts: List[Exponent]): (List[Exponent], List[Exponent], List[(Exponent, Exponent)]) = {
    diff(oldElts, newElts, (e: Exponent) => e.name, (e: Exponent) => e.meta.source.map(_.ref), (oe: Exponent, ne: Exponent) => oe.merge(ne), (oe: Exponent, ne: Exponent) => oe.copy(meta = oe.meta.copy(updated = new DateTime(0))) == ne.copy(meta = ne.meta.copy(updated = new DateTime(0))))
  }

  private def diff[A](oldElts: List[A], newElts: List[A], getName: A => String, getRef: A => Option[String], merge: (A, A) => A, equals: (A, A) => Boolean): (List[A], List[A], List[(A, A)]) = {
    val createdElts = newElts.filter(ne => oldElts.find(oe => eqOpt(getRef(oe), getRef(ne)) || getName(oe) == getName(ne)).isEmpty)
    val deletedElts = oldElts.filter(oe => newElts.find(ne => eqOpt(getRef(oe), getRef(ne)) || getName(oe) == getName(ne)).isEmpty)
    val updatedElts = oldElts
      .map(oe => newElts.find(ne => eqOpt(getRef(oe), getRef(ne)) || getName(oe) == getName(ne)).map(ne => (oe, ne))).flatten
      .map { case (oe, ne) => (oe, merge(oe, ne)) }
      .filter { case (oe, ne) => !equals(oe, ne) }
    (createdElts, deletedElts, updatedElts)
  }

  private def eqOpt[A](opt1: Option[A], opt2: Option[A]): Boolean = {
    opt1.map { value1 =>
      opt2.map { _ == value1 }.getOrElse(false)
    }.getOrElse(false)
  }

  private val eventUrlMatcher = """https?://(.+\.herokuapp.com)/events/([0-9a-z]{8}-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{12})""".r
  def formatUrl(url: String)(implicit req: RequestHeader): String = {
    url match {
      case eventUrlMatcher(remoteHost, eventId) => {
        val localUrl = controllers.api.routes.Events.detailsFull(eventId).absoluteURL(true)
        localUrl.replace(req.host, remoteHost)
      }
      case _ => url
    }
  }

  def fetchEvent(remoteHost: String, eventId: String, generateIds: Boolean)(implicit req: RequestHeader): Future[Option[(Event, List[Session], List[Exponent])]] = {
    val localUrl = controllers.api.routes.Events.detailsFull(eventId).absoluteURL(true)
    val remoteUrl = localUrl.replace(req.host, remoteHost)
    WS.url(remoteUrl).get().map { response =>
      response.json.asOpt[Event].map { event =>
        val sessions = (response.json \ "sessions").as[List[Session]]
        val exponents = (response.json \ "exponents").as[List[Exponent]]

        if (generateIds) {
          val newEventId = Repository.generateUuid()
          (event.copy(uuid = newEventId),
            sessions.map(_.copy(uuid = Repository.generateUuid(), eventId = newEventId)),
            exponents.map(_.copy(uuid = Repository.generateUuid(), eventId = newEventId)))
        } else {
          (event, sessions, exponents)
        }
      }
    }
  }

  def insertAll(event: Event, sessions: List[Session], exponents: List[Exponent]): Future[Option[Event]] = {
    EventRepository.insert(event).map { insertedOpt =>
      if (insertedOpt.isDefined) {
        SessionRepository.bulkInsert(sessions)
        ExponentRepository.bulkInsert(exponents)
      }
      insertedOpt
    }
  }
}