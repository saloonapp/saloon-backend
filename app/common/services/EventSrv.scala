package common.services

import common.models.event.Event
import common.models.event.EventId
import common.models.event.EventItem
import common.models.user.User
import common.models.user.Device
import common.models.user.DeviceId
import common.models.user.UserAction
import common.models.user.UserActionFull
import common.models.user.SubscribeUserAction
import common.models.values.typed._
import common.repositories.event.EventRepository
import common.repositories.event.AttendeeRepository
import common.repositories.event.SessionRepository
import common.repositories.event.ExponentRepository
import common.repositories.event.EventItemRepository
import common.repositories.user.UserActionRepository
import common.repositories.user.DeviceRepository
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object EventSrv {
  def findVisibleEvents(user: User): Future[List[Event]] = {
    EventRepository.findForOrganizations(user.organizationIds.map(_.organizationId)).map { events =>
      events.sortBy(-_.info.start.map(_.getMillis()).getOrElse(9999999999999L))
    }
  }

  def addMetadata(event: Event): Future[(Event, Int, Int, Int, Int)] = {
    for {
      attendeeCount <- AttendeeRepository.countForEvent(event.uuid)
      sessionCount <- SessionRepository.countForEvent(event.uuid)
      exponentCount <- ExponentRepository.countForEvent(event.uuid)
      actionCount <- UserActionRepository.countForEvent(event.uuid)
    } yield (event, attendeeCount, sessionCount, exponentCount, actionCount)
  }

  def addMetadata(events: Seq[Event]): Future[Seq[(Event, Int, Int, Int, Int)]] = {
    val uuids = events.map(_.uuid)
    for {
      attendeeCounts <- AttendeeRepository.countForEvents(uuids)
      sessionCounts <- SessionRepository.countForEvents(uuids)
      exponentCounts <- ExponentRepository.countForEvents(uuids)
      actionCounts <- UserActionRepository.countForEvents(uuids)
    } yield {
      events.map { event =>
        (event, attendeeCounts.get(event.uuid).getOrElse(0), sessionCounts.get(event.uuid).getOrElse(0), exponentCounts.get(event.uuid).getOrElse(0), actionCounts.get(event.uuid).getOrElse(0))
      }
    }
  }

  def getActions(eventId: EventId): Future[List[UserActionFull]] = {
    UserActionRepository.findByEvent(eventId).flatMap { actions =>
      for {
        events: Map[EventId, Event] <- EventRepository.findByUuids(actions.map(_.eventId).flatten.distinct).map(_.map(u => (u.uuid, u)).toMap)
        devices: Map[DeviceId, Device] <- DeviceRepository.findByUuids(actions.map(_.userId).distinct).map(_.map(u => (u.uuid, u)).toMap)
        items: Map[(ItemType, GenericId), EventItem] <- EventItemRepository.findByUuids(actions.map(a => (a.itemType, a.itemId)).distinct)
      } yield {
        actions.map { a =>
          for {
            event <- a.eventId.flatMap(id => events.get(id))
            device <- devices.get(a.userId)
            item <- items.get((a.itemType, a.itemId))
          } yield {
            UserActionFull(event, device, a.action, item)
          }
        }.flatten
      }
    }
  }

}
