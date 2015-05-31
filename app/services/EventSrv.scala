package services

import models._
import common.infrastructure.repository.Repository
import infrastructure.repository.EventRepository
import infrastructure.repository.SessionRepository
import infrastructure.repository.ExponentRepository
import infrastructure.repository.UserActionRepository
import infrastructure.repository.EventItemRepository
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

  def getStatistics(eventId: String): Future[List[(EventItem, Map[String, Int])]] = {
    UserActionRepository.findByEvent(eventId).flatMap { actions =>
      val actionStats = groupByItem(actions).map {
        case (item, actions) => (item, groupByActionName(actions).map {
          case (actionName, actions) => (actionName, actions.length)
        })
      }
      fetchItems(actionStats).map(_.sortBy(e => -coundAction(e._2)))
    }
  }
  private def fetchItems[A](actions: Map[(String, String), A]): Future[List[(EventItem, A)]] = {
    val listFuture = actions.toList.map { case (item, data) => EventItemRepository.getByUuid(item._1, item._2).map(_.map(i => (i, data))) }
    Future.sequence(listFuture).map(_.flatten)
  }
  private def groupByActionName(actions: List[UserAction]): Map[String, List[UserAction]] = {
    actions.groupBy(toActionName)
  }
  private def groupByItem(actions: List[UserAction]): Map[(String, String), List[UserAction]] = {
    actions.groupBy(a => (a.itemType, a.itemId))
  }
  private def coundAction(actions: Map[String, Int]): Int = {
    actions.map(_._2).foldLeft(0)(_ + _)
  }
  private def toActionName(a: UserAction): String = {
    a.action match {
      case FavoriteUserAction(favorite) => FavoriteUserAction.className
      case DoneUserAction(done) => DoneUserAction.className
      case MoodUserAction(rating, mood) => MoodUserAction.className
      case CommentUserAction(text, comment) => CommentUserAction.className
      case SubscribeUserAction(email, filter, subscribe) => SubscribeUserAction.className
      case _ => "unknown"
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