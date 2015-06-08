package services

import models._
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

object EventSrv {
  def addMetadata(event: Event): Future[EventUI] = {
    for {
      sessionCount <- SessionRepository.countForEvent(event.uuid)
      exponentCount <- ExponentRepository.countForEvent(event.uuid)
    } yield EventUI.fromModel(event, sessionCount, exponentCount)
  }

  def addMetadata(events: Seq[Event]): Future[Seq[EventUI]] = {
    val uuids = events.map(_.uuid)
    for {
      sessionCounts <- SessionRepository.countForEvents(uuids)
      exponentCounts <- ExponentRepository.countForEvents(uuids)
    } yield {
      events.map { event =>
        EventUI.fromModel(event, sessionCounts.get(event.uuid).getOrElse(0), exponentCounts.get(event.uuid).getOrElse(0))
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

  def sessionDiff(oldElts: List[Session], newElts: List[Session]): (List[Session], List[Session], List[Session]) = {
    diff(oldElts, newElts, (s: Session) => s.source.map(_.ref).getOrElse(""), (os: Session, ns: Session) => os.merge(ns))
  }

  def exponentDiff(oldElts: List[Exponent], newElts: List[Exponent]): (List[Exponent], List[Exponent], List[Exponent]) = {
    diff(oldElts, newElts, (e: Exponent) => e.source.map(_.ref).getOrElse(""), (oe: Exponent, ne: Exponent) => oe.merge(ne))
  }

  private def diff[A](oldElts: List[A], newElts: List[A], getRef: A => String, merge: (A, A) => A): (List[A], List[A], List[A]) = {
    val createdElts = newElts.filter(ne => oldElts.find(oe => getRef(oe) == getRef(ne)).isEmpty)
    val deletedElts = oldElts.filter(oe => newElts.find(ne => getRef(oe) == getRef(ne)).isEmpty)
    val updatedElts = oldElts
      .map(oe => newElts.find(ne => getRef(oe) == getRef(ne)).map(ne => (oe, ne))).flatten
      .map { case (oe, ne) => merge(oe, ne) }
    (createdElts, updatedElts, deletedElts)
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