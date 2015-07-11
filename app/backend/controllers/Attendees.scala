package backend.controllers

import common.models.user.User
import common.models.user.UserInfo
import common.models.utils.Page
import common.repositories.event.EventRepository
import common.repositories.event.AttendeeRepository
import common.repositories.event.SessionRepository
import common.repositories.event.ExponentRepository
import backend.forms.AttendeeCreateData
import authentication.environments.SilhouetteEnvironment
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api._
import play.api.mvc._
import play.api.data.Form
import com.mohiva.play.silhouette.core.LoginInfo

object Attendees extends SilhouetteEnvironment {
  val createForm: Form[AttendeeCreateData] = Form(AttendeeCreateData.fields)

  def list(eventId: String, query: Option[String], page: Option[Int], pageSize: Option[Int], sort: Option[String]) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    val curPage = page.getOrElse(1)
    for {
      eventOpt <- EventRepository.getByUuid(eventId)
      eltPage <- AttendeeRepository.findPageByEvent(eventId, query.getOrElse(""), curPage, pageSize.getOrElse(Page.defaultSize), sort.getOrElse("name"))
    } yield {
      if (curPage > 1 && eltPage.totalPages < curPage) {
        Redirect(backend.controllers.routes.Attendees.list(eventId, query, Some(eltPage.totalPages), pageSize, sort))
      } else {
        eventOpt
          .map { event => Ok(backend.views.html.Attendees.list(eltPage, event)) }
          .getOrElse { NotFound(backend.views.html.error("404", "Event not found...")) }
      }
    }
  }

  def details(eventId: String, uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    for {
      eventOpt <- EventRepository.getByUuid(eventId)
      eltOpt <- AttendeeRepository.getByUuid(uuid)
      sessions <- SessionRepository.findByEventAttendee(eventId, uuid)
      exponents <- ExponentRepository.findByEventAttendee(eventId, uuid)
    } yield {
      eltOpt.flatMap { elt =>
        eventOpt.map { event => Ok(backend.views.html.Attendees.details(elt, sessions, exponents, event)) }
      }.getOrElse { NotFound(backend.views.html.error("404", "Event not found...")) }
    }
  }

  def create(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    EventRepository.getByUuid(eventId).map { eventOpt =>
      eventOpt
        .map { event => Ok(backend.views.html.Attendees.create(createForm, event)) }
        .getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }

  def doCreate(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        createForm.bindFromRequest.fold(
          formWithErrors => Future(BadRequest(backend.views.html.Attendees.create(formWithErrors, event))),
          formData => AttendeeRepository.insert(AttendeeCreateData.toModel(formData)).map {
            _.map { elt =>
              Redirect(backend.controllers.routes.Attendees.details(eventId, elt.uuid)).flashing("success" -> s"Participant '${elt.name}' créé !")
            }.getOrElse(InternalServerError(backend.views.html.Attendees.create(createForm.fill(formData), event)).flashing("error" -> s"Impossible de créer le participant '${formData.name}'"))
          })
      }.getOrElse(Future(NotFound(backend.views.html.error("404", "Event not found..."))))
    }
  }

  def update(eventId: String, uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    for {
      eltOpt <- AttendeeRepository.getByUuid(uuid)
      eventOpt <- EventRepository.getByUuid(eventId)
    } yield {
      eltOpt.flatMap { elt =>
        eventOpt.map { event => Ok(backend.views.html.Attendees.update(createForm.fill(AttendeeCreateData.fromModel(elt)), elt, event)) }
      }.getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }

  def doUpdate(eventId: String, uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    val dataFuture = for {
      eltOpt <- AttendeeRepository.getByUuid(uuid)
      eventOpt <- EventRepository.getByUuid(eventId)
    } yield (eltOpt, eventOpt)

    dataFuture.flatMap { data =>
      data._1.flatMap { elt =>
        data._2.map { event =>
          createForm.bindFromRequest.fold(
            formWithErrors => Future(BadRequest(backend.views.html.Attendees.update(formWithErrors, elt, event))),
            formData => AttendeeRepository.update(uuid, AttendeeCreateData.merge(elt, formData)).map {
              _.map { updatedElt =>
                Redirect(backend.controllers.routes.Attendees.details(eventId, updatedElt.uuid)).flashing("success" -> s"Le participant '${updatedElt.name}' a bien été modifié")
              }.getOrElse(InternalServerError(backend.views.html.Attendees.update(createForm.fill(formData), elt, event)).flashing("error" -> s"Impossible de modifier le participant '${elt.name}'"))
            })
        }
      }.getOrElse(Future(NotFound(backend.views.html.error("404", "Event not found..."))))
    }
  }

  def delete(eventId: String, uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    AttendeeRepository.getByUuid(uuid).map {
      _.map { elt =>
        AttendeeRepository.delete(uuid)
        Redirect(backend.controllers.routes.Attendees.list(eventId)).flashing("success" -> s"Suppression du participant '${elt.name}'")
      }.getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }

}
