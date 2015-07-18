package backend.controllers

import common.models.user.User
import common.models.user.UserInfo
import common.models.event.AttendeeRegistration
import common.models.event.EventConfigAttendeeSurvey
import common.models.event.EventConfigAttendeeSurveyQuestion
import common.repositories.user.OrganizationRepository
import common.repositories.event.EventRepository
import common.services.EventSrv
import backend.forms.EventCreateData
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import com.mohiva.play.silhouette.core.LoginInfo

object Events extends SilhouetteEnvironment {
  val createForm: Form[EventCreateData] = Form(EventCreateData.fields)
  val registerForm: Form[AttendeeRegistration] = Form(AttendeeRegistration.fields)

  def list = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    EventRepository.findAll(sort = "-info.start").flatMap { events =>
      val eventsNullFirst = events.filter(_.info.start.isEmpty) ++ events.filter(_.info.start.isDefined)
      EventSrv.addMetadata(eventsNullFirst).map { fullEvents =>
        Ok(backend.views.html.Events.list(fullEvents.toList))
      }
    }
  }

  def details(uuid: String) = Action.async { implicit req =>
    //implicit val user = req.identity
    implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    EventRepository.getByUuid(uuid).flatMap {
      _.map { elt =>
        for {
          (event, attendeeCount, sessionCount, exponentCount, actionCount) <- EventSrv.addMetadata(elt)
        } yield {
          Ok(backend.views.html.Events.details(event, attendeeCount, sessionCount, exponentCount, actionCount))
        }
      }.getOrElse(Future(NotFound(backend.views.html.error("404", "Event not found..."))))
    }
  }

  def create = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    for {
      organizations <- OrganizationRepository.findAll()
      categories <- EventRepository.getCategories()
    } yield {
      if (user.canAdministrateSaloon() || user.organizationId.isDefined) {
        Ok(backend.views.html.Events.create(createForm, organizations, categories))
      } else {
        Redirect(backend.controllers.routes.Events.list()).flashing("error" -> s"Vous ne pouvez pas créer un événement !")
      }
    }
  }

  def doCreate = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    createForm.bindFromRequest.fold(
      formWithErrors => for {
        organizations <- OrganizationRepository.findAll()
        categories <- EventRepository.getCategories()
      } yield BadRequest(backend.views.html.Events.create(formWithErrors, organizations, categories)),
      formData => EventRepository.insert(EventCreateData.toModel(formData)).flatMap {
        _.map { elt =>
          Future(Redirect(backend.controllers.routes.Events.details(elt.uuid)).flashing("success" -> s"Événement '${elt.name}' créé !"))
        }.getOrElse {
          for {
            organizations <- OrganizationRepository.findAll()
            categories <- EventRepository.getCategories()
          } yield InternalServerError(backend.views.html.Events.create(createForm.fill(formData), organizations, categories)).flashing("error" -> s"Impossible de créer l'événement '${formData.name}'")
        }
      })
  }

  def update(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    for {
      eltOpt <- EventRepository.getByUuid(uuid)
      organizations <- OrganizationRepository.findAll()
      categories <- EventRepository.getCategories()
    } yield {
      eltOpt.map { elt =>
        Ok(backend.views.html.Events.update(createForm.fill(EventCreateData.fromModel(elt)), elt, organizations, categories))
      }.getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }

  def doUpdate(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    EventRepository.getByUuid(uuid).flatMap {
      _.map { elt =>
        createForm.bindFromRequest.fold(
          formWithErrors => for {
            organizations <- OrganizationRepository.findAll()
            categories <- EventRepository.getCategories()
          } yield BadRequest(backend.views.html.Events.update(formWithErrors, elt, organizations, categories)),
          formData => EventRepository.update(uuid, EventCreateData.merge(elt, formData)).flatMap {
            _.map { updatedElt =>
              Future(Redirect(backend.controllers.routes.Events.details(updatedElt.uuid)).flashing("success" -> s"L'événement '${updatedElt.name}' a bien été modifié"))
            }.getOrElse {
              for {
                organizations <- OrganizationRepository.findAll()
                categories <- EventRepository.getCategories()
              } yield InternalServerError(backend.views.html.Events.update(createForm.fill(formData), elt, organizations, categories)).flashing("error" -> s"Impossible de modifier l'événement '${elt.name}'")
            }
          })
      }.getOrElse(Future(NotFound(backend.views.html.error("404", "Event not found..."))))
    }
  }

  def delete(uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    EventRepository.getByUuid(uuid).map {
      _.map { elt =>
        EventRepository.delete(uuid)
        Redirect(backend.controllers.routes.Events.list()).flashing("success" -> s"Suppression de l'événement '${elt.name}'")
      }.getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }
}
