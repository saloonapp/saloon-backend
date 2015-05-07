package services

import models.Event
import models.EventUI
import infrastructure.repository.SessionRepository
import infrastructure.repository.ExponentRepository
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

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
}