package conferences.controllers

import common.repositories.conference.{ConferenceRepository, PresentationRepository, PersonRepository}
import conferences.models.{Person, PersonData, PersonId}
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.data.Form
import play.api.mvc.{Action, Controller}

object Persons extends Controller {
  val personForm = Form(PersonData.fields)

  def search(q: Option[String]) = Action.async { implicit req =>
    PersonRepository.find(PersonRepository.buildSearchFilter(q), Json.obj("name" -> 1)).map { personList =>
      Ok(conferences.views.html.person.list(personList))
    }
  }

  def detail(pId: PersonId) = Action.async { implicit req =>
    for {
      personOpt <- PersonRepository.get(pId)
      presentationList <- PresentationRepository.findForPerson(pId)
      conferenceMap <- ConferenceRepository.findByIds(presentationList.map(_.conferenceId))
    } yield {
      personOpt.map { person =>
        Ok(conferences.views.html.person.detail(person, presentationList, conferenceMap))
      }.getOrElse {
        NotFound("Not Found !")
      }
    }
  }

  def create = Action { implicit req =>
    Ok(conferences.views.html.person.form(personForm))
  }

  def doCreate = Action.async { implicit req =>
    personForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(conferences.views.html.person.form(formWithErrors))),
      formData => {
        val person = PersonData.toModel(formData)
        PersonRepository.insert(person).map { success =>
          Redirect(conferences.controllers.routes.Persons.detail(person.id))
        }
      }
    )
  }

  def edit(id: PersonId) = Action.async { implicit req =>
    PersonRepository.get(id).map { personOpt =>
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
