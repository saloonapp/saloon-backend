package conferences.controllers

import common.repositories.conference.{ConferenceRepository, PresentationRepository, PersonRepository}
import conferences.models.PersonId
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc.{Action, Controller}

object Persons extends Controller {
  def detail(pId: PersonId) = Action.async { implicit req =>
    for {
      personOpt <- PersonRepository.get(pId)
      presentationList <- PresentationRepository.findForPerson(pId)
      conferenceList <- ConferenceRepository.findByIds(presentationList.map(_.conferenceId))
    } yield {
      personOpt.map { person =>
        Ok(conferences.views.html.person.detail(person, presentationList, conferenceList.map(c => (c.id, c)).toMap))
      }.getOrElse {
        NotFound("Not Found !")
      }
    }
  }
}
