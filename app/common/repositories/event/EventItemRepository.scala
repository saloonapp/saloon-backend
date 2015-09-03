package common.repositories.event

import common.models.event.Event
import common.models.event.EventId
import common.models.event.EventItem
import common.models.event.Session
import common.models.event.SessionId
import common.models.event.Exponent
import common.models.event.ExponentId
import common.models.values.GenericId
import common.repositories.CollectionReferences
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object EventItemRepository {
  def getByUuid(itemType: String, itemId: GenericId): Future[Option[EventItem]] = {
    if (itemType == Event.className) EventRepository.getByUuid(itemId.toEventId)
    else if (itemType == Session.className) SessionRepository.getByUuid(itemId.toSessionId)
    else if (itemType == Exponent.className) ExponentRepository.getByUuid(itemId.toExponentId)
    else Future(None)
  }

  def findByUuids(uuids: List[(String, GenericId)]): Future[Map[(String, GenericId), EventItem]] = {
    for {
      events <- EventRepository.findByUuids(uuids.filter(_._1 == Event.className).map(p => p._2.toEventId).distinct)
      sessions <- SessionRepository.findByUuids(uuids.filter(_._1 == Session.className).map(p => p._2.toSessionId).distinct)
      exponents <- ExponentRepository.findByUuids(uuids.filter(_._1 == Exponent.className).map(p => p._2.toExponentId).distinct)
    } yield {
      events.map(e => ((Event.className, e.uuid.toGenericId), e)).toMap ++
        sessions.map(e => ((Session.className, e.uuid.toGenericId), e)).toMap ++
        exponents.map(e => ((Exponent.className, e.uuid.toGenericId), e)).toMap
    }
  }
}
