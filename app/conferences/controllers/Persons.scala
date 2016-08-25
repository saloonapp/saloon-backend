package conferences.controllers

import common.repositories.conference.{ConferenceRepository, PresentationRepository, PersonRepository}
import conferences.models.{PersonData, PersonId}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.data.Form
import play.api.mvc.{Action, Controller}

object Persons extends Controller {
  val personForm = Form(PersonData.fields)

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

  def doCreate = Action.async { implicit req =>
    Future(BadRequest("not implemented"))
  }

  def edit(id: PersonId) = Action.async { implicit req =>
    val personOptFut = PersonRepository.get(id)
    for {
      personOpt <- personOptFut
    } yield {
      personOpt.map { person =>
        Ok(conferences.views.html.person.form(personForm.fill(PersonData.fromModel(person))))
      }.getOrElse {
        NotFound("Not Found !")
      }
    }
  }

  def doEdit(id: PersonId) = Action.async { implicit req =>
    personForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(conferences.views.html.person.form(formWithErrors))),
      formData => {
        val person = PersonData.toModel(formData)
        PersonRepository.get(id).flatMap { oldPersonOpt =>
          PersonRepository.update(id, person).map { success =>
            Redirect(conferences.controllers.routes.Persons.detail(id))
          }
        }
      }
    )
  }
}
