package common.repositories.event

import common.repositories.Repository
import common.repositories.CollectionReferences
import common.models.event.EventItem
import common.models.event.Event
import common.models.event.Session
import common.models.event.Exponent
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object EventItemRepository {
  def getByUuid(itemType: String, itemId: String): Future[Option[EventItem]] = {
    if (itemType == Event.className) EventRepository.getByUuid(itemId)
    else if (itemType == Session.className) SessionRepository.getByUuid(itemId)
    else if (itemType == Exponent.className) ExponentRepository.getByUuid(itemId)
    else Future(None)
  }

  def findByUuids(uuids: List[(String, String)]): Future[Map[(String, String), EventItem]] = {
    for {
      events <- EventRepository.findByUuids(uuids.filter(_._1 == Event.className).map(_._2).distinct)
      sessions <- SessionRepository.findByUuids(uuids.filter(_._1 == Session.className).map(_._2).distinct)
      exponents <- ExponentRepository.findByUuids(uuids.filter(_._1 == Exponent.className).map(_._2).distinct)
    } yield {
      events.map(e => ((Event.className, e.uuid), e)).toMap ++
        sessions.map(e => ((Session.className, e.uuid), e)).toMap ++
        exponents.map(e => ((Exponent.className, e.uuid), e)).toMap
    }
  }
}
