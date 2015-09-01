package backend.controllers

import common.models.user.User
import common.models.user.UserInfo
import common.models.utils.Page
import common.services.FileExporter
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
          .map { event => Ok(backend.views.html.Events.Attendees.list(eltPage, event)) }
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
        eventOpt.map { event => Ok(backend.views.html.Events.Attendees.details(elt, sessions, exponents, event)) }
      }.getOrElse { NotFound(backend.views.html.error("404", "Event not found...")) }
    }
  }

  def create(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    for {
      eventOpt <- EventRepository.getByUuid(eventId)
      roles <- AttendeeRepository.findEventRoles(eventId)
    } yield {
      eventOpt
        .map { event => Ok(backend.views.html.Events.Attendees.create(createForm, roles, event)) }
        .getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }

  def doCreate(eventId: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        createForm.bindFromRequest.fold(
          formWithErrors => for {
            roles <- AttendeeRepository.findEventRoles(eventId)
          } yield BadRequest(backend.views.html.Events.Attendees.create(formWithErrors, roles, event)),
          formData => AttendeeRepository.insert(AttendeeCreateData.toModel(formData)).flatMap {
            _.map { elt =>
              Future(Redirect(backend.controllers.routes.Attendees.details(eventId, elt.uuid)).flashing("success" -> s"Participant '${elt.name}' créé !"))
            }.getOrElse {
              for {
                roles <- AttendeeRepository.findEventRoles(eventId)
              } yield InternalServerError(backend.views.html.Events.Attendees.create(createForm.fill(formData), roles, event)).flashing("error" -> s"Impossible de créer le participant '${formData.info.firstName} ${formData.info.lastName}'")
            }
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
      roles <- AttendeeRepository.findEventRoles(eventId)
    } yield {
      eltOpt.flatMap { elt =>
        eventOpt.map { event => Ok(backend.views.html.Events.Attendees.update(createForm.fill(AttendeeCreateData.fromModel(elt)), elt, roles, event)) }
      }.getOrElse(NotFound(backend.views.html.error("404", "Event not found...")))
    }
  }

  def doUpdate(eventId: String, uuid: String) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    val res: Future[Future[Result]] = for {
      eventOpt <- EventRepository.getByUuid(eventId)
      eltOpt <- AttendeeRepository.getByUuid(uuid)
    } yield {
      val res2: Option[Future[Result]] = for {
        event <- eventOpt
        elt <- eltOpt
      } yield {
        createForm.bindFromRequest.fold(
          formWithErrors => for {
            roles <- AttendeeRepository.findEventRoles(eventId)
          } yield BadRequest(backend.views.html.Events.Attendees.update(formWithErrors, elt, roles, event)),
          formData => AttendeeRepository.update(uuid, AttendeeCreateData.merge(elt, formData)).flatMap {
            _.map { updatedElt =>
              Future(Redirect(backend.controllers.routes.Attendees.details(eventId, updatedElt.uuid)).flashing("success" -> s"Le participant '${updatedElt.name}' a bien été modifié"))
            }.getOrElse {
              for {
                roles <- AttendeeRepository.findEventRoles(eventId)
              } yield InternalServerError(backend.views.html.Events.Attendees.update(createForm.fill(formData), elt, roles, event)).flashing("error" -> s"Impossible de modifier le participant '${elt.name}'")
            }
          })
      }
      res2.getOrElse(Future(NotFound(backend.views.html.error("404", "Event not found..."))))
    }
    res.flatMap(identity)
  }

  def delete(eventId: String, uuid: String, redirectOpt: Option[String]) = SecuredAction.async { implicit req =>
    implicit val user = req.identity
    //implicit val user = User(loginInfo = LoginInfo("", ""), email = "loicknuchel@gmail.com", info = UserInfo("Loïc", "Knuchel"), rights = Map("administrateSaloon" -> true))
    AttendeeRepository.getByUuid(uuid).flatMap {
      _.map { attendee =>
        for {
          res <- AttendeeRepository.delete(uuid)
          res2 <- ExponentRepository.removeFromAllTeams(uuid)
        } yield {
          redirectOpt.map { redirect => Redirect(redirect) }
            .getOrElse { Redirect(backend.controllers.routes.Attendees.list(eventId)) }
            .flashing("success" -> s"Suppression du profil ${attendee.name}")
        }
      }.getOrElse(Future(NotFound(backend.views.html.error("404", "Event not found..."))))
    }
  }

  def fileExport(eventId: String) = SecuredAction.async { implicit req =>
    EventRepository.getByUuid(eventId).flatMap { eventOpt =>
      eventOpt.map { event =>
        AttendeeRepository.findByEvent(eventId).map { elts =>
          val filename = event.name + "_attendees.csv"
          val content = FileExporter.makeCsv(elts.map(_.toBackendExport))
          Ok(content)
            .withHeaders(CONTENT_DISPOSITION -> ("attachment; filename=\"" + filename + "\""))
            .as("text/csv")
        }
      }.getOrElse(Future(NotFound(admin.views.html.error404())))
    }
  }

}
