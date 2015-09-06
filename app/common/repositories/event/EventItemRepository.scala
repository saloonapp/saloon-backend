package common.repositories.event

import common.models.event.Event
import common.models.event.EventId
import common.models.event.EventItem
import common.models.event.Session
import common.models.event.SessionId
import common.models.event.Exponent
import common.models.event.ExponentId
import common.models.values.typed._
import common.repositories.CollectionReferences
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object EventItemRepository {
  def getByUuid(itemType: ItemType, itemId: GenericId): Future[Option[EventItem]] = {
    if (itemType == ItemType.events) EventRepository.getByUuid(itemId.toEventId)
    else if (itemType == ItemType.sessions) SessionRepository.getByUuid(itemId.toSessionId)
    else if (itemType == ItemType.exponents) ExponentRepository.getByUuid(itemId.toExponentId)
    else Future(None)
  }

  def findByUuids(uuids: List[(ItemType, GenericId)]): Future[Map[(ItemType, GenericId), EventItem]] = {
    for {
      events <- EventRepository.findByUuids(uuids.filter(_._1 == ItemType.events).map(p => p._2.toEventId).distinct)
      sessions <- SessionRepository.findByUuids(uuids.filter(_._1 == ItemType.sessions).map(p => p._2.toSessionId).distinct)
      exponents <- ExponentRepository.findByUuids(uuids.filter(_._1 == ItemType.exponents).map(p => p._2.toExponentId).distinct)
    } yield {
      events.map(e => ((ItemType.events, e.uuid.toGenericId), e)).toMap ++
        sessions.map(e => ((ItemType.sessions, e.uuid.toGenericId), e)).toMap ++
        exponents.map(e => ((ItemType.exponents, e.uuid.toGenericId), e)).toMap
    }
  }
}
