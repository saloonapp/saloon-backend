package infrastructure.repository

import common.infrastructure.repository.Repository
import models.EventItem
import models.EventUI
import models.SessionUI
import models.ExponentUI
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object EventItemRepository {
  def getByUuid(itemType: String, itemId: String): Future[Option[EventItem]] = {
    if (itemType == EventUI.className) EventRepository.getByUuid(itemId)
    else if (itemType == SessionUI.className) SessionRepository.getByUuid(itemId)
    else if (itemType == ExponentUI.className) ExponentRepository.getByUuid(itemId)
    else Future(None)
  }

  def findByUuids(uuids: List[(String, String)]): Future[Map[(String, String), EventItem]] = {
    for {
      events <- EventRepository.findByUuids(uuids.filter(_._1 == EventUI.className).map(_._2).distinct)
      sessions <- SessionRepository.findByUuids(uuids.filter(_._1 == SessionUI.className).map(_._2).distinct)
      exponents <- ExponentRepository.findByUuids(uuids.filter(_._1 == ExponentUI.className).map(_._2).distinct)
    } yield {
      events.map(e => ((EventUI.className, e.uuid), e)).toMap ++
        sessions.map(e => ((SessionUI.className, e.uuid), e)).toMap ++
        exponents.map(e => ((ExponentUI.className, e.uuid), e)).toMap
    }
  }
}
