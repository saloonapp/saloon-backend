package infrastructure.repository

import infrastructure.repository.common.Repository
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
}
