package common.services

import common.models.event.Event
import common.models.event.EventId
import common.models.event.EventItem
import common.models.user.UserAction
import common.repositories.user.UserActionRepository
import common.repositories.event.EventRepository
import common.repositories.event.EventItemRepository
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object DeviceSrv {

  def getActionsForUser(userId: String): Future[Map[Option[Event], List[(Option[EventItem], UserAction)]]] = {
    UserActionRepository.findByUser(userId).flatMap { actions =>
      val groupedActions: Map[EventId, List[UserAction]] = actions.groupBy(_.eventId.getOrElse(EventId("unknown")))
      val mapFuture = groupedActions.map {
        case (eventId, actions) => for {
          eventOpt <- EventRepository.getByUuid(eventId)
          items <- EventItemRepository.findByUuids(actions.map(a => (a.itemType, a.itemId)))
        } yield {
          (eventOpt, actions.map(a => (items.get((a.itemType, a.itemId)), a)).sortBy(-_._2.created.getMillis()))
        }
      }
      Future.sequence(mapFuture).map(_.toMap)
    }
  }

}